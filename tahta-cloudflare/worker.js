import { DurableObject } from "cloudflare:workers";

const CORS_HEADERS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, PATCH, PUT, DELETE, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Accept",
  "Cache-Control": "no-store"
};

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...CORS_HEADERS
    }
  });
}

function cleanSessionCode(raw) {
  return String(raw || "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "");
}

function cleanPresenceClassId(raw) {
  return String(raw || "")
    .trim()
    .replace(/[^A-Za-z0-9_-]/g, "");
}

function parseStudentIdsParam(raw) {
  if (Array.isArray(raw)) {
    return Array.from(new Set(raw.map((s) => String(s || "").trim()).filter(Boolean)));
  }
  return Array.from(
    new Set(
      String(raw || "")
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean)
    )
  );
}

export class BoardRoom extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.sqlReady = false;
    this.cachedState = null;
    this.stateLoaded = false;
  }

  getPresenceMeta(ws) {
    try {
      if (typeof ws.deserializeAttachment === "function") {
        const att = ws.deserializeAttachment();
        if (att && att.kind === "presence") return att;
      }
    } catch (_e) {}
    return null;
  }

  setPresenceMeta(ws, meta) {
    try {
      if (typeof ws.serializeAttachment === "function") {
        ws.serializeAttachment(meta);
      }
    } catch (_e) {}
  }

  loadJsonKv(key, fallback) {
    const raw = this.getKv(key);
    if (!raw) return fallback;
    try {
      const parsed = JSON.parse(raw);
      return parsed ?? fallback;
    } catch (_e) {
      return fallback;
    }
  }

  buildPresenceSnapshot(classId, excludeSocket = null) {
    const sockets = this.ctx.getWebSockets("presence") || [];
    const now = Date.now();
    let teacherOnline = false;
    let teacherLastActiveMillis = Number(this.getKv("presence_teacher_last") || 0) || null;

    const onlineParentMap = new Map();
    for (const ws of sockets) {
      if (excludeSocket && ws === excludeSocket) continue;
      const meta = this.getPresenceMeta(ws);
      if (!meta) continue;
      const ts = Number(meta.lastSeenAt || meta.connectedAt || now);
      if (meta.role === "teacher") {
        teacherOnline = true;
        if (!teacherLastActiveMillis || ts > teacherLastActiveMillis) {
          teacherLastActiveMillis = ts;
        }
      } else if (meta.role === "parent") {
        const emailKey = String(meta.email || "").trim().toLowerCase();
        if (!emailKey) continue;
        const existing = onlineParentMap.get(emailKey);
        const sIds = parseStudentIdsParam(meta.studentIds);
        if (!existing) {
          onlineParentMap.set(emailKey, {
            email: emailKey,
            name: String(meta.name || "Öğrenci Velisi"),
            studentIds: sIds,
            lastActiveMillis: ts
          });
        } else {
          const mergedIds = Array.from(new Set([...existing.studentIds, ...sIds]));
          existing.studentIds = mergedIds;
          if (meta.name && (!existing.name || existing.name === "Öğrenci Velisi")) {
            existing.name = String(meta.name);
          }
          if (ts > existing.lastActiveMillis) existing.lastActiveMillis = ts;
        }
      }
    }

    const storedOffline = this.loadJsonKv("presence_offline_parents", {});
    const offlineParents = [];
    const cutoff = now - 24 * 60 * 60 * 1000;
    for (const [emailKey, info] of Object.entries(storedOffline || {})) {
      if (onlineParentMap.has(emailKey)) continue;
      const ts = Number(info?.lastActiveMillis || 0);
      if (ts < cutoff) continue;
      offlineParents.push({
        email: emailKey,
        studentIds: parseStudentIdsParam(info?.studentIds),
        lastActiveMillis: ts
      });
    }

    return {
      type: "presence_snapshot",
      classId: String(classId || this.getKv("presence_class_id") || ""),
      serverTs: now,
      teacherOnline,
      teacherLastActiveMillis,
      onlineParents: Array.from(onlineParentMap.values()),
      offlineParents
    };
  }

  broadcastPresenceSnapshot(classId, excludeSocket = null) {
    const snapshot = this.buildPresenceSnapshot(classId, excludeSocket);
    const sockets = this.ctx.getWebSockets("presence") || [];
    const text = JSON.stringify(snapshot);
    for (const ws of sockets) {
      if (excludeSocket && ws === excludeSocket) continue;
      try {
        ws.send(text);
      } catch (_e) {
        try {
          ws.close(1011, "send_failed");
        } catch (_e2) {}
      }
    }
    return snapshot;
  }

  handlePresenceDisconnect(ws) {
    const meta = this.getPresenceMeta(ws);
    if (!meta) return false;
    const now = Date.now();
    const classId = meta.classId || this.getKv("presence_class_id") || "";
    const remaining = (this.ctx.getWebSockets("presence") || []).filter((s) => s !== ws);

    if (meta.role === "teacher") {
      const stillHasTeacher = remaining.some((s) => this.getPresenceMeta(s)?.role === "teacher");
      if (!stillHasTeacher) {
        this.setKv("presence_teacher_last", String(now));
      }
    } else if (meta.role === "parent") {
      const emailKey = String(meta.email || "").trim().toLowerCase();
      if (emailKey) {
        const stillHasParent = remaining.some(
          (s) => {
            const m = this.getPresenceMeta(s);
            return m && m.role === "parent" && String(m.email || "").trim().toLowerCase() === emailKey;
          }
        );
        if (!stillHasParent) {
          const offlineMap = this.loadJsonKv("presence_offline_parents", {});
          offlineMap[emailKey] = {
            studentIds: parseStudentIdsParam(meta.studentIds),
            lastActiveMillis: now
          };
          const keys = Object.keys(offlineMap);
          if (keys.length > 100) {
            keys
              .sort((a, b) => Number(offlineMap[a]?.lastActiveMillis || 0) - Number(offlineMap[b]?.lastActiveMillis || 0))
              .slice(0, keys.length - 100)
              .forEach((k) => delete offlineMap[k]);
          }
          this.setKv("presence_offline_parents", JSON.stringify(offlineMap));
        }
      }
    }

    this.broadcastPresenceSnapshot(classId, ws);
    return true;
  }

  ensureTables() {
    if (this.sqlReady) return;
    this.ctx.storage.sql.exec(`
      CREATE TABLE IF NOT EXISTS kv (
        k TEXT PRIMARY KEY,
        v TEXT NOT NULL
      );
      CREATE TABLE IF NOT EXISTS chunks (
        id TEXT PRIMARY KEY,
        data TEXT NOT NULL,
        updated_at INTEGER NOT NULL
      );
    `);
    this.sqlReady = true;
  }

  getKv(key) {
    this.ensureTables();
    const rows = this.ctx.storage.sql
      .exec("SELECT v FROM kv WHERE k = ?", key)
      .toArray();
    if (!rows || rows.length === 0) return null;
    return rows[0].v;
  }

  setKv(key, value) {
    this.ensureTables();
    this.ctx.storage.sql.exec(
      "INSERT OR REPLACE INTO kv (k, v) VALUES (?, ?)",
      key,
      String(value)
    );
  }

  deleteKv(key) {
    this.ensureTables();
    this.ctx.storage.sql.exec("DELETE FROM kv WHERE k = ?", key);
  }

  loadState() {
    if (this.stateLoaded) return this.cachedState;
    const raw = this.getKv("state");
    if (raw) {
      try {
        this.cachedState = JSON.parse(raw);
      } catch (_e) {
        this.cachedState = null;
      }
    } else {
      this.cachedState = null;
    }
    this.stateLoaded = true;
    return this.cachedState;
  }

  saveState(stateObj) {
    this.cachedState = stateObj;
    this.stateLoaded = true;
    this.setKv("state", JSON.stringify(stateObj));
  }

  clearSignaling() {
    this.deleteKv("rtc_offer");
    this.deleteKv("rtc_answer");
    this.deleteKv("rtc_ice_phone");
    this.deleteKv("rtc_ice_board");
  }

  clearChunks() {
    this.ensureTables();
    this.ctx.storage.sql.exec("DELETE FROM chunks");
  }

  broadcastToTag(tag, obj) {
    const sockets = this.ctx.getWebSockets(tag);
    if (!sockets || sockets.length === 0) return 0;
    const text = JSON.stringify(obj);
    let sent = 0;
    for (const ws of sockets) {
      try {
        ws.send(text);
        sent++;
      } catch (_e) {
        try {
          ws.close(1011, "send_failed");
        } catch (_e2) {}
      }
    }
    return sent;
  }

  applyStatePatch(patchObj, replace) {
    const current = replace ? {} : (this.loadState() || {});
    const next = Object.assign({}, current, patchObj || {});
    next._transport = "cloudflare";
    if (replace || String(next.status || "").toUpperCase() === "DISCONNECTED") {
      this.clearChunks();
      this.clearSignaling();
    }
    this.saveState(next);
    this.broadcastToTag("board", {
      type: "session_state",
      data: next
    });
    return next;
  }

  handleRtcSignalMessage(msg) {
    const kind = String(msg.type || "");
    const connId = String(msg.connectionId || "");
    if (kind === "rtc_offer") {
      this.clearSignaling();
      this.setKv("rtc_offer", JSON.stringify(msg));
      this.broadcastToTag("board", msg);
      return;
    }
    if (kind === "rtc_answer") {
      this.setKv("rtc_answer", JSON.stringify(msg));
      this.broadcastToTag("phone", msg);
      return;
    }
    if (kind === "rtc_ice") {
      const role = String(msg.role || "");
      const targetTag = role === "board" ? "phone" : "board";
      const storageKey = role === "board" ? "rtc_ice_board" : "rtc_ice_phone";
      let list = [];
      try {
        const raw = this.getKv(storageKey);
        if (raw) list = JSON.parse(raw);
      } catch (_e) {
        list = [];
      }
      if (!Array.isArray(list)) list = [];
      // Farklı connectionId'den kalan eski adayları temizle
      list = list.filter((c) => !connId || !c.connectionId || c.connectionId === connId);
      if (list.length < 40) {
        list.push(msg);
        this.setKv(storageKey, JSON.stringify(list));
      }
      this.broadcastToTag(targetTag, msg);
    }
  }

  async fetch(request) {
    const url = new URL(request.url);
    const subPath = url.pathname;

    if (subPath.endsWith("/presence/snapshot")) {
      const classId = cleanPresenceClassId(url.searchParams.get("classId"));
      if (classId) this.setKv("presence_class_id", classId);
      return jsonResponse(this.buildPresenceSnapshot(classId));
    }

    if (subPath.endsWith("/presence/ws")) {
      if (request.headers.get("Upgrade") !== "websocket") {
        return jsonResponse({ ok: false, error: "WebSocket upgrade required" }, 426);
      }
      const classId = cleanPresenceClassId(url.searchParams.get("classId"));
      const role = url.searchParams.get("role") === "teacher" ? "teacher" : "parent";
      const email = String(url.searchParams.get("email") || "").trim().toLowerCase();
      const name = String(url.searchParams.get("name") || "").trim();
      const studentIds = parseStudentIdsParam(url.searchParams.get("studentIds"));
      const now = Date.now();

      if (classId) this.setKv("presence_class_id", classId);

      const pair = new WebSocketPair();
      const client = pair[0];
      const server = pair[1];
      this.ctx.acceptWebSocket(server, ["presence"]);
      this.setPresenceMeta(server, {
        kind: "presence",
        classId,
        role,
        email,
        name,
        studentIds,
        connectedAt: now,
        lastSeenAt: now
      });

      if (role === "teacher") {
        this.setKv("presence_teacher_last", String(now));
      } else if (role === "parent" && email) {
        const offlineMap = this.loadJsonKv("presence_offline_parents", {});
        if (offlineMap[email]) {
          delete offlineMap[email];
          this.setKv("presence_offline_parents", JSON.stringify(offlineMap));
        }
      }

      this.broadcastPresenceSnapshot(classId);
      return new Response(null, { status: 101, webSocket: client });
    }

    if (request.headers.get("Upgrade") === "websocket" || subPath.endsWith("/ws")) {
      if (request.headers.get("Upgrade") !== "websocket") {
        return jsonResponse({ ok: false, error: "WebSocket upgrade required" }, 426);
      }
      const role = url.searchParams.get("role") === "phone" ? "phone" : "board";
      const pair = new WebSocketPair();
      const client = pair[0];
      const server = pair[1];
      this.ctx.acceptWebSocket(server, [role]);

      try {
        if (role === "board") {
          const state = this.loadState();
          if (state) {
            server.send(JSON.stringify({ type: "session_state", data: state }));
            const activeConnId = String(state.connectionId || "");
            const rawOffer = this.getKv("rtc_offer");
            if (rawOffer) {
              const offerObj = JSON.parse(rawOffer);
              if (!activeConnId || !offerObj.connectionId || offerObj.connectionId === activeConnId) {
                server.send(rawOffer);
                const rawIce = this.getKv("rtc_ice_phone");
                if (rawIce) {
                  const iceList = JSON.parse(rawIce);
                  for (const item of iceList) {
                    if (!activeConnId || !item.connectionId || item.connectionId === activeConnId) {
                      server.send(JSON.stringify(item));
                    }
                  }
                }
              }
            }
          } else {
            server.send(JSON.stringify({ type: "waiting", exists: false }));
          }
        } else {
          const boards = this.ctx.getWebSockets("board");
          server.send(JSON.stringify({ type: "room_ready", boardCount: boards.length }));
        }
      } catch (_e) {}

      return new Response(null, { status: 101, webSocket: client });
    }

    if (subPath.includes("/chunk/")) {
      const chunkId = decodeURIComponent(subPath.split("/chunk/")[1] || "").trim();
      if (!chunkId) return jsonResponse({ ok: false, error: "Missing chunkId" }, 400);

      if (request.method === "PUT" || request.method === "POST") {
        let body = {};
        try {
          body = await request.json();
        } catch (_e) {
          return jsonResponse({ ok: false, error: "Invalid JSON" }, 400);
        }
        this.ensureTables();
        this.ctx.storage.sql.exec(
          "INSERT OR REPLACE INTO chunks (id, data, updated_at) VALUES (?, ?, ?)",
          chunkId,
          String(body.data || ""),
          Number(body.updatedAt || Date.now())
        );
        return jsonResponse({ ok: true });
      }

      if (request.method === "GET") {
        this.ensureTables();
        const rows = this.ctx.storage.sql
          .exec("SELECT data, updated_at FROM chunks WHERE id = ?", chunkId)
          .toArray();
        if (!rows || rows.length === 0) {
          return jsonResponse({ ok: false, exists: false }, 404);
        }
        return jsonResponse({
          ok: true,
          exists: true,
          data: rows[0].data,
          updatedAt: rows[0].updated_at
        });
      }
    }

    if (subPath.endsWith("/signal")) {
      if (request.method === "POST") {
        let msg = {};
        try {
          msg = await request.json();
        } catch (_e) {
          return jsonResponse({ ok: false, error: "Invalid JSON" }, 400);
        }
        this.handleRtcSignalMessage(msg);
        return jsonResponse({ ok: true });
      }
      if (request.method === "GET") {
        const role = url.searchParams.get("role") === "board" ? "board" : "phone";
        const rawOffer = this.getKv("rtc_offer");
        const rawAnswer = this.getKv("rtc_answer");
        const rawIce = this.getKv(role === "board" ? "rtc_ice_phone" : "rtc_ice_board");
        return jsonResponse({
          ok: true,
          offer: rawOffer ? JSON.parse(rawOffer) : null,
          answer: rawAnswer ? JSON.parse(rawAnswer) : null,
          ice: rawIce ? JSON.parse(rawIce) : []
        });
      }
    }

    if (request.method === "GET") {
      const state = this.loadState();
      if (!state) {
        return jsonResponse({ ok: true, exists: false, data: null }, 404);
      }
      return jsonResponse({ ok: true, exists: true, data: state }, 200);
    }

    if (request.method === "POST" || request.method === "PATCH" || request.method === "PUT") {
      let body = {};
      try {
        body = await request.json();
      } catch (_e) {
        return jsonResponse({ ok: false, error: "Invalid JSON" }, 400);
      }
      const replace = url.searchParams.get("replace") === "1";
      const next = this.applyStatePatch(body, replace);
      const boardClients = this.ctx.getWebSockets("board").length;
      return jsonResponse({
        ok: true,
        boardClients,
        updatedAt: next.updatedAt || Date.now()
      });
    }

    return jsonResponse({ ok: false, error: "Method not allowed" }, 405);
  }

  async webSocketMessage(ws, rawMessage) {
    if (typeof rawMessage !== "string") return;
    let msg;
    try {
      msg = JSON.parse(rawMessage);
    } catch (_e) {
      return;
    }
    if (!msg || !msg.type) return;

    if (msg.type === "ping") {
      const meta = this.getPresenceMeta(ws);
      if (meta) {
        meta.lastSeenAt = Date.now();
        this.setPresenceMeta(ws, meta);
        if (meta.role === "teacher") {
          this.setKv("presence_teacher_last", String(meta.lastSeenAt));
        }
      }
      try {
        ws.send(JSON.stringify({ type: "pong", ts: Date.now() }));
      } catch (_e) {}
      return;
    }

    if (msg.type === "presence_update") {
      const meta = this.getPresenceMeta(ws);
      if (meta) {
        meta.lastSeenAt = Date.now();
        if (Array.isArray(msg.studentIds)) {
          meta.studentIds = parseStudentIdsParam(msg.studentIds);
        }
        if (typeof msg.name === "string" && msg.name.trim()) {
          meta.name = msg.name.trim();
        }
        this.setPresenceMeta(ws, meta);
        this.broadcastPresenceSnapshot(meta.classId);
      }
      return;
    }

    if (msg.type === "state_patch") {
      this.applyStatePatch(msg.data || {}, Boolean(msg.replace));
      return;
    }

    if (msg.type === "mirror_frame") {
      const frameData = msg.data || {};
      const current = this.loadState() || {};
      current.boardCast = frameData;
      current.activeModule = "PAYLASIM";
      current.activeModuleTitle = "📡 Paylaşım";
      current.currentSubPage = "PAYLASIM";
      current.currentSubPageTitle = "📡 Paylaşım";
      current.updatedAt = frameData.updatedAt || Date.now();
      current._transport = "cloudflare";
      this.saveState(current);
      this.broadcastToTag("board", {
        type: "mirror_frame",
        data: frameData
      });
      return;
    }

    if (msg.type === "rtc_offer" || msg.type === "rtc_answer" || msg.type === "rtc_ice") {
      this.handleRtcSignalMessage(msg);
    }
  }

  async webSocketClose(ws, code, reason, _wasClean) {
    this.handlePresenceDisconnect(ws);
    try {
      ws.close(code || 1000, reason || "closed");
    } catch (_e) {}
  }

  async webSocketError(ws, _err) {
    this.handlePresenceDisconnect(ws);
    try {
      ws.close(1011, "error");
    } catch (_e) {}
  }
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const pathname = url.pathname;

    if (pathname.startsWith("/tahta/api/")) {
      if (request.method === "OPTIONS") {
        return new Response(null, { status: 204, headers: CORS_HEADERS });
      }

      if (pathname === "/tahta/api/health") {
        return jsonResponse({ ok: true, engine: "cloudflare-do", ts: Date.now() });
      }

      if (!env.BOARD_ROOMS) {
        return jsonResponse({ ok: false, error: "Durable Object binding missing" }, 503);
      }

      if (pathname === "/tahta/api/presence/ws" || pathname === "/tahta/api/presence/snapshot") {
        const classId = cleanPresenceClassId(url.searchParams.get("classId"));
        if (classId.length < 2) {
          return jsonResponse({ ok: false, error: "Invalid classId" }, 400);
        }
        const id = env.BOARD_ROOMS.idFromName("PRESENCE_" + classId);
        const stub = env.BOARD_ROOMS.get(id);
        return stub.fetch(request);
      }

      let code = "";
      if (pathname === "/tahta/api/ws") {
        code = cleanSessionCode(url.searchParams.get("code"));
      } else if (pathname.startsWith("/tahta/api/session/")) {
        const rest = pathname.slice("/tahta/api/session/".length);
        code = cleanSessionCode(decodeURIComponent(rest.split("/")[0] || ""));
      } else if (pathname.startsWith("/tahta/api/signal/")) {
        const rest = pathname.slice("/tahta/api/signal/".length);
        code = cleanSessionCode(decodeURIComponent(rest.split("/")[0] || ""));
      }

      if (code.length < 4) {
        return jsonResponse({ ok: false, error: "Invalid session code" }, 400);
      }

      const id = env.BOARD_ROOMS.idFromName(code);
      const stub = env.BOARD_ROOMS.get(id);
      return stub.fetch(request);
    }

    return env.ASSETS.fetch(request);
  }
};
