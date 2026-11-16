#!/usr/bin/env python3
"""
BİRİZ GitHub Release ve version.json için dinamik, değişiklik odaklı Türkçe sürüm notu üreticisi.
Her sürümde sabit metinleri tekrarlamak yerine gerçekten değişen kod, diff ve commit geçmişinden
özgün Türkçe başlıklar üretir.
"""

import argparse
import json
import re
import subprocess
from pathlib import Path


# Öncelikli ince taneli özellik/düzeltme algılayıcıları:
# (id, dosya_regex, diff_veya_commit_regex_yoksa_None, baslik, aciklama)
SPECIFIC_DIFF_RULES = [
    (
        "smartboard_rest_connect",
        re.compile(r"(SmartBoardHelper|tahta\.html)", re.I),
        re.compile(r"(restPatchDocument|restHttpClient|restGetDocument|writeDocumentRestFirst|startWaitingRestPoll)", re.I),
        "Hızlı Akıllı Tahta Bağlantısı",
        "Tahta eşleştirme ve canlı veri aktarımı REST protokolüne geçirilerek zaman aşımı hatası giderildi.",
    ),
    (
        "smartboard_disconnect",
        re.compile(r"(SmartBoardHelper|tahta\.html|BirizViewModel)", re.I),
        re.compile(r"(disconnectSession|forceDisconnectToWaiting|disconnectedConnectionId|startConnectedHeartbeat|verifySessionStillConnected)", re.I),
        "Bağlantı Kesme Senkronizasyonu",
        "Telefondan akıllı tahta bağlantısı kesildiğinde tahta ekranının anında bekleme moduna dönmesi sağlandı.",
    ),
    (
        "smartboard_qr",
        re.compile(r"(QrScannerDialog)", re.I),
        None,
        "Karekod Okuma Hızı",
        "Kamera ile karekod tarama hızı ve ters renkli QR algılama kararlılığı artırıldı.",
    ),
    (
        "smartboard_cast_mirror",
        re.compile(r"(SmartBoardHelper|tahta\.html|InstalledApps|ScreenMirror|Share)", re.I),
        re.compile(r"(boardCast|SCREEN_MIRROR|castFileToBoard|publishScreenMirrorFrame|updateScreenMirrorPauseState)", re.I),
        "Tahtada Ekran ve Dosya Paylaşımı",
        "Seçilen uygulama, dosya veya bağlantıların akıllı tahta paylaşım sayfasına yansıtılması iyileştirildi.",
    ),
    (
        "release_notes_dynamic",
        re.compile(r"(generate_release_notes\.py|AppUpdateManager\.kt|AppUpdateDialog\.kt)", re.I),
        re.compile(r"(SPECIFIC_DIFF_RULES|is_release_notes_json_fresh|fetchFromReleasesApi|releaseItems|infer_notes_from_git)", re.I),
        "Dinamik Sürüm Notları",
        "Her yeni güncellemede yalnızca o sürümde yapılan gerçek değişikliklerin başlıklarla listelenmesi sağlandı.",
    ),
    (
        "app_update_installer",
        re.compile(r"(AppUpdate|AppUpdateDownload|AppUpdateInstall|AppUpdateNotification)", re.I),
        None,
        "Tek Tıkla Güncelleme",
        "Güncelleme indirme, doğrulama ve tek dokunuşla kurulum akışı iyileştirildi.",
    ),
    (
        "auth_account_switch",
        re.compile(r"(GoogleAuth|GoogleSignIn|MainActivity\.kt$|BirizViewModel\.kt$)", re.I),
        re.compile(r"(signOut|CredentialManager|switchAccount|handleGoogleSignIn|clearCredentialState)", re.I),
        "Google Hesap Geçişi",
        "Çıkış yaptıktan sonra uygulamayı kapatmadan farklı Google hesabıyla giriş yapabilme sorunu düzeltildi.",
    ),
    (
        "auth_account_delete",
        re.compile(r"(UserProfileMenu|MainActivity\.kt$|BirizViewModel\.kt$|ClassRegistryRepository|SmartBoardHelper)", re.I),
        re.compile(r"(deleteAccount|deleteUserSmartBoardTraces|Hesabımı Sil|hesap sil|purgeUser)", re.I),
        "Hesabımı Sil İşlemi",
        "Hesap silme seçeneğinin kullanıcı, sınıf ve tahta oturum izlerini tüm sistemden temizlemesi sağlandı.",
    ),
    (
        "star_rewards",
        re.compile(r"(StarReward|StarsClassroom)", re.I),
        None,
        "Yıldız ve Ödül Takibi",
        "Öğrenci yıldız puanları ve ödül ekranlarındaki veri senkronizasyonu güncellendi.",
    ),
    (
        "class_list_students",
        re.compile(r"(ClassList|Student|ClassRegistry)", re.I),
        None,
        "Sınıf Listesi Yönetimi",
        "Öğrenci listesi, sınıf düzeni ve kayıt akışları iyileştirildi.",
    ),
    (
        "tournament",
        re.compile(r"(Tournament)", re.I),
        None,
        "Sınıf Turnuvası",
        "Turnuva eşleşmeleri, skor tabloları ve tahta görünümü güncellendi.",
    ),
    (
        "library_reading",
        re.compile(r"(Library|Kitaplik)", re.I),
        None,
        "Kitaplık ve Okuma Takibi",
        "Sınıf kitaplığı ve okuma kayıtları deneyimi iyileştirildi.",
    ),
    (
        "seating_plan",
        re.compile(r"(SeatingPlan|PlannedSeating)", re.I),
        None,
        "Oturma Planı",
        "Sınıf oturma düzeni ve masa yerleşim ekranı güncellendi.",
    ),
    (
        "schedule_lessons",
        re.compile(r"(Schedule)", re.I),
        None,
        "Ders Programı",
        "Haftalık ders programı ve ders saati hatırlatıcıları güncellendi.",
    ),
    (
        "communication_chat",
        re.compile(r"(Communication|Chat|BulkMessage|Messaging)", re.I),
        None,
        "Veli İletişimi ve Duyurular",
        "Duyuru, anket ve mesajlaşma ekranları iyileştirildi.",
    ),
    (
        "class_tools",
        re.compile(r"(Timer|LuckyStudent|GroupMaker)", re.I),
        None,
        "Sınıf İçi Etkinlik Araçları",
        "Zamanlayıcı, şanslı öğrenci ve grup oluşturma araçları güncellendi.",
    ),
    (
        "notifications_quiet",
        re.compile(r"(Notification|QuietHours)", re.I),
        None,
        "Bildirim ve Sessiz Saatler",
        "Bildirim iletimi ve sessiz saat kontrolleri düzenlendi.",
    ),
    (
        "portal_dashboard",
        re.compile(r"(ParentPortal|TeacherRoom|TeacherDashboard|TeacherHome)", re.I),
        None,
        "Öğretmen ve Veli Arayüzü",
        "Ana ekran gezinmesi ve portal kullanım kolaylığı artırıldı.",
    ),
    (
        "ui_theme_assets",
        re.compile(r"(ui/theme/|res/font|res/drawable|res/mipmap|res/values)", re.I),
        None,
        "Tasarım ve Görünüm",
        "Yazı tipi, renk uyumu ve görsel arayüz detayları yenilendi.",
    ),
]

GENERIC_COMMIT_RE = re.compile(
    r"^(update|sync|auto|chore|wip|fix|test|merge|commit|push|aistudio|initial|minor|build|release|update files|sync from ai studio)[\s:._-]*$",
    re.I,
)


def run_git(args: list[str]) -> str:
    try:
        out = subprocess.check_output(["git", *args], stderr=subprocess.DEVNULL, text=True)
        return out.strip()
    except Exception:
        return ""


def parse_semver_tuple(tag: str) -> tuple[int, int, int] | None:
    m = re.match(r"^v?(\d+)\.(\d+)\.(\d+)$", tag.strip(), re.I)
    if not m:
        return None
    return int(m.group(1)), int(m.group(2)), int(m.group(3))


def get_previous_tag() -> str:
    head_sha = run_git(["rev-parse", "HEAD"])
    raw_tags = [t.strip() for t in run_git(["tag"]).splitlines() if t.strip()]
    if not raw_tags:
        return ""

    semver_candidates: list[tuple[tuple[int, int, int], str]] = []
    other_candidates: list[str] = []

    for tag in raw_tags:
        tag_sha = run_git(["rev-list", "-n", "1", tag])
        if head_sha and tag_sha == head_sha:
            # HEAD'in üzerindeki etiketi atla ki önceki sürümle gerçek farkı kıyaslayabilelim
            continue
        parsed = parse_semver_tuple(tag)
        if parsed is not None:
            semver_candidates.append((parsed, tag))
        else:
            other_candidates.append(tag)

    if semver_candidates:
        semver_candidates.sort(key=lambda item: item[0], reverse=True)
        return semver_candidates[0][1]

    if other_candidates:
        return other_candidates[-1]

    # Tek etiket varsa ve HEAD üzerindeyse son çare boş dönüp HEAD~1..HEAD kullanılmasını sağla
    return ""


def clean_bullet_line(line: str) -> tuple[str, str] | None:
    text = re.sub(r"^[\s\-*•]+", "", line).strip()
    if not text:
        return None
    text = text.replace("**", "").strip()
    if ":" in text:
        head, body = text.split(":", 1)
        head = head.strip()
        body = body.strip()
        if head and body and 2 <= len(head) <= 45:
            return head, body
    return "Yenilik", text


def parse_manual_notes(raw: str) -> tuple[str, list[dict[str, str]]]:
    items: list[dict[str, str]] = []
    for line in raw.splitlines():
        parsed = clean_bullet_line(line)
        if parsed:
            items.append({"title": parsed[0], "summary": parsed[1]})
    items = items[:6]
    title = items[0]["title"] if len(items) == 1 else "Yeni Güncelleme ve İyileştirmeler"
    return title, items


def load_json_release_notes(raw_text: str) -> tuple[str, list[dict[str, str]], str] | None:
    try:
        data = json.loads(raw_text)
        title = str(data.get("title", "")).strip()
        target_version = str(data.get("targetVersion", "")).strip().lstrip("vV")
        raw_items = data.get("items", [])
        items: list[dict[str, str]] = []
        for entry in raw_items:
            if isinstance(entry, dict):
                t = str(entry.get("title", "")).strip()
                s = str(entry.get("summary", "")).strip()
                if t and s:
                    items.append({"title": t, "summary": s})
        if items:
            return (title or "Yeni Güncelleme ve İyileştirmeler"), items[:6], target_version
    except Exception:
        pass
    return None


def get_fresh_json_release_notes(
    prev_tag: str,
    version_name: str,
    filepath: str = "release-notes.json",
) -> tuple[str, list[dict[str, str]]] | None:
    p = Path(filepath)
    if not p.exists():
        return None

    current_raw = p.read_text(encoding="utf-8")
    loaded = load_json_release_notes(current_raw)
    if not loaded:
        return None
    title, items, target_version = loaded

    if target_version and target_version != version_name.lstrip("vV"):
        return None

    if prev_tag:
        prev_raw = run_git(["show", f"{prev_tag}:{filepath}"])
        if prev_raw:
            prev_loaded = load_json_release_notes(prev_raw)
            if prev_loaded and prev_loaded[0] == title and prev_loaded[1] == items:
                # Önceki sürümle birebir aynı release-notes.json taşınmış; yeni sürümde tekrar etme!
                return None
    else:
        changed_recent = run_git(["diff", "--name-only", "HEAD~1..HEAD", "--", filepath])
        if not changed_recent:
            return None

    return title, items


def collect_diff_churn(rev_range: str) -> dict[str, int]:
    numstat = (
        run_git(["diff", "--numstat", rev_range])
        if rev_range
        else run_git(["diff", "--numstat", "HEAD~1..HEAD"])
    )
    churn: dict[str, int] = {}
    for line in numstat.splitlines():
        parts = line.split("\t")
        if len(parts) < 3:
            continue
        add_s, del_s, path = parts[0], parts[1], parts[2].strip()
        add_n = int(add_s) if add_s.isdigit() else 5
        del_n = int(del_s) if del_s.isdigit() else 2
        churn[path] = add_n + del_n
    return churn


def collect_per_file_diff(rev_range: str, path: str) -> str:
    args = (
        ["diff", "-U0", rev_range, "--", path]
        if rev_range
        else ["diff", "-U0", "HEAD~1..HEAD", "--", path]
    )
    return run_git(args)[:40000]


def infer_notes_from_git(prev_tag: str) -> tuple[str, list[dict[str, str]]]:
    rev_range = f"{prev_tag}..HEAD" if prev_tag else ""
    churn_map = collect_diff_churn(rev_range)
    changed_files = list(churn_map.keys())
    if not changed_files:
        changed_files = run_git(["diff", "--name-only", "HEAD~1..HEAD"]).splitlines()
        churn_map = {p: 10 for p in changed_files}

    log_args = (
        ["log", rev_range, "--no-merges", "--pretty=format:%s"]
        if rev_range
        else ["log", "-n", "5", "--no-merges", "--pretty=format:%s"]
    )
    commit_msgs = [m.strip() for m in run_git(log_args).splitlines() if m.strip()]

    # Önce gerçek commit mesajlarında anlamlı başlık: açıklama varsa topla
    scored_items: list[tuple[int, str, str]] = []
    seen_titles: set[str] = set()

    for msg in commit_msgs:
        if len(msg) < 10 or GENERIC_COMMIT_RE.match(msg):
            continue
        # İngilizce/generic oto commitleri atla, Türkçe veya colon formatlı anlamlı notları al
        if ":" in msg:
            parsed = clean_bullet_line(msg)
            if parsed and parsed[0] not in seen_titles:
                seen_titles.add(parsed[0])
                scored_items.append((10_000, parsed[0], parsed[1]))

    # Diff tabanlı ince taneli analiz
    diff_cache: dict[str, str] = {}
    for _rule_id, file_re, diff_re, title, summary in SPECIFIC_DIFF_RULES:
        if title in seen_titles:
            continue
        matched_files = [p for p in changed_files if file_re.search(p)]
        if not matched_files:
            continue
        weight = sum(churn_map.get(p, 1) for p in matched_files)
        if diff_re is not None:
            combined_diff = []
            for p in matched_files[:8]:
                if p not in diff_cache:
                    diff_cache[p] = collect_per_file_diff(rev_range, p)
                combined_diff.append(diff_cache[p])
            haystack = "\n".join(combined_diff) + "\n" + "\n".join(commit_msgs)
            if not diff_re.search(haystack):
                continue
            weight += 50  # Özgün içerik eşleşmesine öncelik ver

        seen_titles.add(title)
        scored_items.append((weight, title, summary))

    scored_items.sort(key=lambda x: x[0], reverse=True)
    items = [{"title": t, "summary": s} for _, t, s in scored_items[:5]]

    # Eğer hâlâ boşsa kalan anlamlı commit mesajlarını veya dosya isimlerini kullan
    if not items:
        for msg in commit_msgs:
            if len(msg) >= 10 and not GENERIC_COMMIT_RE.match(msg):
                parsed = clean_bullet_line(msg)
                if parsed and parsed[0] not in seen_titles:
                    seen_titles.add(parsed[0])
                    items.append({"title": parsed[0], "summary": parsed[1]})
            if len(items) >= 4:
                break

    if not items:
        readable_modules = []
        for path in changed_files:
            stem = Path(path).stem
            if stem and stem not in readable_modules and not stem.startswith("."):
                readable_modules.append(stem)
        if readable_modules:
            sample = ", ".join(readable_modules[:3])
            items = [
                {
                    "title": "Sistem Güncellemesi",
                    "summary": f"{sample} bileşenlerinde iyileştirmeler ve kararlılık düzenlemeleri yapıldı.",
                }
            ]
        else:
            items = [
                {
                    "title": "Performans ve Kararlılık",
                    "summary": "Genel hız iyileştirmeleri ve hata düzeltmeleri yapıldı.",
                }
            ]

    if len(items) == 1:
        headline = items[0]["title"]
    elif len(items) == 2:
        headline = f"{items[0]['title']} ve {items[1]['title']}"
    else:
        headline = "Yeni Özellikler ve İyileştirmeler"

    return headline, items


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--version-name", required=True)
    parser.add_argument("--version-code", required=True, type=int)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--apk-url", required=True)
    parser.add_argument("--output-dir", default="release-output")
    parser.add_argument("--notes-file", default="release-notes.md")
    parser.add_argument("--manual-notes", default="")
    args = parser.parse_args()

    prev_tag = get_previous_tag()
    title = ""
    items: list[dict[str, str]] = []

    if args.manual_notes.strip():
        title, items = parse_manual_notes(args.manual_notes.strip())

    if not items:
        fresh = get_fresh_json_release_notes(prev_tag, args.version_name, "release-notes.json")
        if fresh:
            title, items = fresh

    if not items:
        title, items = infer_notes_from_git(prev_tag)

    release_notes_text = "\n".join(f"• {it['title']}: {it['summary']}" for it in items)

    out_dir = Path(args.output_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    version_payload = {
        "versionCode": args.version_code,
        "versionName": args.version_name,
        "tag": f"v{args.version_name}",
        "apkUrl": args.apk_url,
        "appName": "BİRİZ",
        "releaseTitle": title,
        "releaseItems": items,
        "releaseNotes": release_notes_text,
    }
    (out_dir / "version.json").write_text(
        json.dumps(version_payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    bullet_md = "\n".join(f"- **{it['title']}:** {it['summary']}" for it in items)
    notes_md = (
        f"### 🚀 BİRİZ v{args.version_name} — {title}\n\n"
        f"#### ✨ Bu Sürümde Neler Değişti?\n"
        f"{bullet_md}\n\n"
        f"#### 📲 İndirme Seçenekleri\n"
        f"- **Doğrudan APK İndir:** [Biriz.apk](https://github.com/{args.repo}/releases/download/v{args.version_name}/Biriz.apk)\n"
        f"- **Sürümlü APK:** [Biriz-v{args.version_name}.apk](https://github.com/{args.repo}/releases/download/v{args.version_name}/Biriz-v{args.version_name}.apk)\n\n"
        f"*Uygulamayı açtığınızda üst çubuktan tek dokunuşla indirip kurabilirsiniz.*\n"
    )
    Path(args.notes_file).write_text(notes_md, encoding="utf-8")


if __name__ == "__main__":
    main()
