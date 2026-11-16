#!/usr/bin/env python3
"""
BİRİZ GitHub Release ve version.json için kısa Türkçe başlık + madde madde sürüm notu üreticisi.
"""

import argparse
import json
import os
import re
import subprocess
from pathlib import Path


AREA_RULES = [
    (
        re.compile(r"(AppUpdate|generate_release_notes|release-notes\.json|MainActivity\.kt$)", re.I),
        "Güncelleme Sistemi",
        "Uygulama güncelleme indirme, kurulum ve sürüm notları deneyimi iyileştirildi.",
    ),
    (
        re.compile(r"(SmartBoard|tahta\.html|public/|tahta-cloudflare|InstalledApps)", re.I),
        "Akıllı Tahta & Paylaşım",
        "Akıllı tahta bağlantısı ve canlı ekran paylaşımı özellikleri geliştirildi.",
    ),
    (
        re.compile(r"(StarReward|StarsClassroom)", re.I),
        "Yıldız & Ödül Sistemi",
        "Öğrenci yıldız puanları ve ödül takip ekranları güncellendi.",
    ),
    (
        re.compile(r"(ClassList|Student)", re.I),
        "Sınıf Listesi",
        "Öğrenci listesi yönetimi ve sınıf düzenlemeleri iyileştirildi.",
    ),
    (
        re.compile(r"(Tournament)", re.I),
        "Turnuva Modülü",
        "Turnuva eşleşmeleri, skorlar ve görünüm iyileştirildi.",
    ),
    (
        re.compile(r"(Library|Kitaplik)", re.I),
        "Kitaplık & Okuma",
        "Sınıf kitaplığı ve okuma takibi özellikleri güncellendi.",
    ),
    (
        re.compile(r"(SeatingPlan|PlannedSeating)", re.I),
        "Oturma Planı",
        "Sınıf oturma düzeni ve masa yerleşimi iyileştirildi.",
    ),
    (
        re.compile(r"(Schedule)", re.I),
        "Ders Programı",
        "Haftalık ders programı ve bildirim zamanlaması güncellendi.",
    ),
    (
        re.compile(r"(Communication|Chat|BulkMessage|Messaging)", re.I),
        "Mesajlaşma & Duyurular",
        "Veli iletişimi, duyuru ve anket özellikleri iyileştirildi.",
    ),
    (
        re.compile(r"(Timer|LuckyStudent|GroupMaker)", re.I),
        "Sınıf Araçları",
        "Süre sayacı, şanslı öğrenci ve grup oluşturma araçları güncellendi.",
    ),
    (
        re.compile(r"(ParentPortal|TeacherRoom|TeacherDashboard|TeacherHome)", re.I),
        "Ana Ekran & Portal",
        "Öğretmen ve veli arayüzlerinde kullanım kolaylığı artırıldı.",
    ),
    (
        re.compile(r"(Notification|QuietHours)", re.I),
        "Bildirim Yönetimi",
        "Bildirim gönderimi ve sessiz saat ayarları düzenlendi.",
    ),
    (
        re.compile(r"(GoogleAuth|GoogleSignIn|Kvkk|TermsOfService|firestore\.rules)", re.I),
        "Güvenlik & Senkronizasyon",
        "Oturum güvenliği ve bulut veri eşitlemesi güçlendirildi.",
    ),
    (
        re.compile(r"(ui/theme/|res/drawable|res/mipmap|res/values)", re.I),
        "Tasarım & Arayüz",
        "Görsel tasarım, okunabilirlik ve tema uyumu iyileştirildi.",
    ),
]

GENERIC_COMMIT_RE = re.compile(
    r"^(update|sync|auto|chore|wip|fix|test|merge|commit|push|aistudio|initial|minor)[\s:._-]*$",
    re.I,
)


def run_git(args: list[str]) -> str:
    try:
        out = subprocess.check_output(["git", *args], stderr=subprocess.DEVNULL, text=True)
        return out.strip()
    except Exception:
        return ""


def get_previous_tag() -> str:
    tags = run_git(["tag", "--sort=-v:refname"]).splitlines()
    return tags[0].strip() if tags else ""


def clean_bullet_line(line: str) -> tuple[str, str] | None:
    text = re.sub(r"^[\s\-*•]+", "", line).strip()
    if not text:
        return None
    text = text.replace("**", "").strip()
    if ":" in text:
        head, body = text.split(":", 1)
        head = head.strip()
        body = body.strip()
        if head and body and len(head) <= 45:
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


def was_file_changed_since(tag: str, filepath: str) -> bool:
    if not Path(filepath).exists():
        return False
    if not tag:
        return True
    diff = run_git(["diff", "--name-only", f"{tag}..HEAD", "--", filepath])
    if diff:
        return True
    # Eğer tag HEAD'in kendisiyse (tekrar tetikleme), son commit'i kontrol et
    last_diff = run_git(["diff", "--name-only", "HEAD~1..HEAD", "--", filepath])
    return bool(last_diff)


def load_json_release_notes(filepath: str) -> tuple[str, list[dict[str, str]]] | None:
    p = Path(filepath)
    if not p.exists():
        return None
    try:
        data = json.loads(p.read_text(encoding="utf-8"))
        title = str(data.get("title", "")).strip()
        raw_items = data.get("items", [])
        items: list[dict[str, str]] = []
        for entry in raw_items:
            if isinstance(entry, dict):
                t = str(entry.get("title", "")).strip()
                s = str(entry.get("summary", "")).strip()
                if t and s:
                    items.append({"title": t, "summary": s})
        if items:
            return (title or "Yeni Güncelleme ve İyileştirmeler"), items[:6]
    except Exception:
        pass
    return None


def infer_notes_from_git(prev_tag: str) -> tuple[str, list[dict[str, str]]]:
    rev_range = f"{prev_tag}..HEAD" if prev_tag else "HEAD~5..HEAD"
    changed_files = run_git(["diff", "--name-only", rev_range]).splitlines()
    if not changed_files:
        changed_files = run_git(["diff", "--name-only", "HEAD~1..HEAD"]).splitlines()

    items: list[dict[str, str]] = []
    seen_titles: set[str] = set()

    for rule_re, title, summary in AREA_RULES:
        if any(rule_re.search(path) for path in changed_files):
            if title not in seen_titles:
                seen_titles.add(title)
                items.append({"title": title, "summary": summary})
        if len(items) >= 4:
            break

    # Eğer commit mesajlarında anlamlı kısa Türkçe notlar varsa başa ekleyebiliriz
    commit_msgs = run_git(["log", rev_range, "--no-merges", "--pretty=format:%s"]).splitlines()
    if not items and commit_msgs:
        for msg in commit_msgs:
            m = msg.strip()
            if len(m) >= 8 and not GENERIC_COMMIT_RE.match(m):
                parsed = clean_bullet_line(m)
                if parsed and parsed[0] not in seen_titles:
                    seen_titles.add(parsed[0])
                    items.append({"title": parsed[0], "summary": parsed[1]})
            if len(items) >= 4:
                break

    if not items:
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

    if not items and was_file_changed_since(prev_tag, "release-notes.json"):
        loaded = load_json_release_notes("release-notes.json")
        if loaded:
            title, items = loaded

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
