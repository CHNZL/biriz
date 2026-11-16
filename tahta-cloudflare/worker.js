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

export class BoardRoom extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.sqlReady = false;
    this.cachedState = null;
    this.stateLoaded = false;
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
      try {
        ws.send(JSON.stringify({ type: "pong", ts: Date.now() }));
      } catch (_e) {}
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
    try {
      ws.close(code || 1000, reason || "closed");
    } catch (_e) {}
  }

  async webSocketError(ws, _err) {
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
