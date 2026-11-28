#!/usr/bin/env python3
"""
BİRİZ GitHub Release ve version.json için değişiklik odaklı, yalnızca Türkçe sürüm notu üreticisi.
- Yalnızca mevcut güncellemedeki gerçek değişiklikleri baz alır (önceki sürümlerden tekrar etmez).
- İngilizce commit mesajları, teknik dosya adları veya şablon tekrarları asla çıktıya girmez.
"""

import argparse
import json
import re
import subprocess
from pathlib import Path


# (kural_id, alan_grubu, genis_yedek_mi, dosya_regex, diff_veya_commit_regex_yoksa_None, baslik, aciklama)
# Önemli: İnce taneli kurallar (genis_yedek_mi=False) önce değerlendirilir; aynı alan grubunda
# spesifik bir kural eşleştiyse o alanın genel/yedek kuralı tekrar üretilmez.
SPECIFIC_DIFF_RULES = [
    # --- Kitaplık ve Okuma İstatistikleri ---
    (
        "library_parent_requests_and_quick_assign",
        "library",
        False,
        re.compile(r"(LibraryRequestPersistenceHelper|LibraryRequestsSection|ParentLibraryRequestDialogs|LibrarySubScreen|ParentLibrarySection)", re.I),
        re.compile(r"(LibraryBookRequest|KITAPLIK_ISTEKLER|ParentAddBookRequestDialog|ParentFinishBookRequestDialog|onAssignExistingBook|validateStartBookRequest)", re.I),
        "Veli Kitap Girişi, Kitap Bitti Talebi ve Hızlı Ödünç Verme",
        "Velilerin yeni kitap girişi ve kitap bitirme talebi gönderebilmesi, öğretmenin Veli Kitap İstekleri merkezinden onaylayıp reddedebilmesi ve kitap ekleme penceresinden kayıtlı kitapların anında ödünç verilebilmesi eklendi.",
    ),
    (
        "library_student_pdf_currently_reading",
        "library",
        False,
        re.compile(r"(LibraryPdfReportsHelper|LibrarySubScreen)", re.I),
        re.compile(r"(extractCurrentlyReadingBooksForStudent|OKUMAKTA OLDUĞU KİTAPLAR|ActiveStudentBookEntry)", re.I),
        "Öğrenci Okuma Raporunda Okumakta Olduğu Kitaplar",
        "Öğrencinin Okuduğu Kitaplar Listesi raporunda öğrenci özet satırı ile tamamlanan kitaplar tablosu arasına okumakta olduğu kitaplar, okuma süresi ve gecikme uyarısı eklendi.",
    ),
    (
        "library_analytics_student_sort",
        "library",
        False,
        re.compile(r"(LibrarySubScreen|LibraryPdfReportsHelper)", re.I),
        re.compile(r"(studentNumberComparator|studentNumber\.trim\(\)\.toIntOrNull|ReadingAnalyticsView)", re.I),
        "Okuma İstatistiklerinde Öğrenci Sıralaması",
        "Öğretmen kitaplığındaki okuma istatistiklerinde öğrencilerin öğrenci numarasına göre sayısal ve doğru sıralanması sağlandı.",
    ),
    (
        "library_3d_book_cards",
        "library",
        False,
        re.compile(r"(LibrarySubScreen)", re.I),
        re.compile(r"(BookCardGridItem|bookmarkRibbon|spineColor|bookCover|sayfa kalınlığı|3 boyutlu kitap)", re.I),
        "Kitap Şeklinde Kitaplık Kartları",
        "Öğretmen kitaplığındaki kutu görünümü, kitap sırtı ve ayraç detaylarına sahip kitap formunda kartlarla yenilendi.",
    ),
    # --- Veli Portalı ve İletişim ---
    (
        "parent_chat_notification_privacy_isolation",
        "communication",
        False,
        re.compile(r"(ChatVisibilityHelper|NotificationLifecycleHelper|BirizViewModel|BackgroundNotificationSyncHelper|BirizFirebaseMessagingService|ParentPortalSubScreen)", re.I),
        re.compile(r"(isMessageOwnedByParentViewer|isChatReactionNotificationAllowedForViewer|shouldEmitLiveReactionNotification|sanitizeNotificationsForViewer|isNotificationAllowedForParent|previousChatReactionsScopeKey)", re.I),
        "Veli Mesajlaşma ve Bildirim Gizliliği Güçlendirmesi",
        "Veli hesaplarında özel mesajlar ve mesaj tepki bildirimleri yalnızca ilgili öğrencinin kendi velisiyle sınırlandırıldı; ilk girişte geçmiş tepki bildirimlerinin oluşması engellendi ve önbellekteki başka öğrenci bildirimlerinin otomatik temizlenmesi sağlandı.",
    ),
    (
        "parent_comm_font_size",
        "communication",
        False,
        re.compile(r"(ParentPortalSubScreen|CommunicationPersistenceHelper)", re.I),
        re.compile(r"(ParentCommunicationPuntoBar|loadParentFontOffset|saveParentFontOffset|fontOffset)", re.I),
        "Veli Mesaj ve Duyurularda Yazı Boyutu Ayarı",
        "Veli mesajlar ve duyurular ekranlarına yazıları büyütüp küçültme çubuğu eklendi.",
    ),
    (
        "parent_hero_sibling",
        "parent_portal",
        False,
        re.compile(r"(ParentPortalSubScreen)", re.I),
        re.compile(r"(parent_hero_student_switcher|parent_hero_add_sibling_button|KARDEŞ SEÇİCİ)", re.I),
        "Sade Veli Üst Çubuğu ve Karttan Kardeş Ekleme",
        "Üst çubuk sadeleştirilerek ek alan açıldı; kardeş değiştirme ve kardeş ekleme işlemi ana sayfadaki öğrenci kartına taşındı.",
    ),
    (
        "teacher_schedule_today_and_live_day",
        "schedule",
        False,
        re.compile(r"(ScheduleSubScreen|ScheduleHelper|BirizViewModel)", re.I),
        re.compile(r"(isLessonLiveForSelectedDay|getDefaultScheduleDayKey|resetDayToToday|activeDayKey)", re.I),
        "Öğretmen Ders Programında Gün Bazlı Canlı Ders Gösterimi",
        "Öğretmen ders programı varsayılan olarak bulunulan günle açılacak şekilde düzenlendi; aktif ders ve bitişe kalan süre bilgisi yalnızca o günün derslerinde gösteriliyor.",
    ),
    (
        "parent_schedule_live",
        "schedule",
        False,
        re.compile(r"(ParentPortalSubScreen)", re.I),
        re.compile(r"(ŞU AN İŞLENİYOR|Bitişe .* dk|kalan dakika)", re.I),
        "Veli Ders Programında Aktif Ders Vurgusu",
        "Veli ders programında o an işlenen aktif ders doğrudan ders kutusu üzerinde ve bitişe kalan süresiyle birlikte gösterildi.",
    ),
    (
        "teacher_room_coming_soon",
        "parent_portal",
        False,
        re.compile(r"(TeacherRoomSubScreen|TeacherHomeSubScreen|TeacherDashboardScreen)", re.I),
        re.compile(r"(HAZIRLIK AŞAMASINDA|teacher_room_coming_soon_card|Öğretmenler Odası)", re.I),
        "Öğretmenler Odası Hazırlık Ekranı",
        "Öğretmenler Odası bölümü hazırlık aşamasında bilgilendirme görünümüyle güncellendi.",
    ),
    # --- Akıllı Tahta ---
    (
        "smartboard_home_legibility_and_note_spotlight",
        "smartboard",
        False,
        re.compile(r"(tahta\.html|index\.html)", re.I),
        re.compile(r"(home-note-beacon|noteBeaconPulse|reader-metrics|home-metric-unit|is-long-note)", re.I),
        "Akıllı Tahta Okunabilirlik ve Günün Notu Güncellemesi",
        "Akıllı tahta ana sayfasındaki yıldız, kitap ve sayfa sayıları uzaktan okunacak şekilde büyütülüp yüksek kontrastlı rozetlere dönüştürüldü; Günün Notu şeridi belirgin canlı tasarımla öne çıkarıldı.",
    ),
    (
        "smartboard_countdown_and_daily_note",
        "smartboard",
        False,
        re.compile(r"(tahta\.html|index\.html|SmartBoardHelper|SmartBoardDailyNoteCard|TeacherDashboardScreen)", re.I),
        re.compile(r"(SmartBoardDailyNoteCard|home-countdown-hero|home-note-strip|updateHomeCountdownTick|saveSmartBoardDailyNote|buildDailyNotePayload)", re.I),
        "Akıllı Tahta Canlı Geri Sayım ve Günün Notu Panosu",
        "Akıllı tahta ana sayfasına büyük ders/teneffüs geri sayım sayacı ile Akıllı Tahta Kumandasından sabitlenebilen Günün Notu ve Ödev Panosu eklendi.",
    ),
    (
        "smartboard_home_dashboard",
        "smartboard",
        False,
        re.compile(r"(tahta\.html|index\.html|SmartBoardHelper)", re.I),
        re.compile(r"(renderHomeDashboard|mod-ANA_SAYFA|setHomeStarScope|calculatePeriodStarTotals|home-dashboard-grid)", re.I),
        "Akıllı Tahta Sınıf Ana Sayfası",
        "Uygulama ana sayfasına geçildiğinde akıllı tahtada bugünün canlı ders programı, dönem bazlı ilk 5 yıldız sıralaması ve en çok okuyan ilk 5 öğrenci özetinin görüntülenmesi sağlandı.",
    ),
    (
        "smartboard_hybrid_cf_webrtc",
        "smartboard",
        False,
        re.compile(r"(SmartBoardHelper|SmartBoardRtcManager|SmartBoardScreenCastService|worker\.js|wrangler\.toml|tahta\.html)", re.I),
        re.compile(r"(CLOUDFLARE_API_BASE|BoardRoom|SmartBoardRtcManager|DataChannel|BRZF|cfPatchSession)", re.I),
        "Gelişmiş Akıllı Tahta Altyapısı",
        "Akıllı tahta bağlantısı daha hızlı ve kesintisiz sunucu yapısına taşındı; aynı ağda doğrudan ekran yansıtma desteği eklendi.",
    ),
    (
        "smartboard_quota_429",
        "smartboard",
        False,
        re.compile(r"(SmartBoardHelper|SmartBoardScreenCastService|TimerSubScreen|LuckyStudentSubScreen|tahta\.html)", re.I),
        re.compile(r"(isQuotaCooldownActive|quotaCooldownUntilMs|clearDeltaSyncCache|429)", re.I),
        "Tahta Bağlantı Hızı İyileştirmesi",
        "Akıllı tahta bağlantısı ve ekran yansıtma sırasındaki gereksiz veri yazmaları azaltılarak bağlantı kararlılığı artırıldı.",
    ),
    (
        "smartboard_rest_connect",
        "smartboard",
        False,
        re.compile(r"(SmartBoardHelper|tahta\.html)", re.I),
        re.compile(r"(restPatchDocument|restHttpClient|restGetDocument|writeDocumentRestFirst|startWaitingRestPoll)", re.I),
        "Hızlı Akıllı Tahta Bağlantısı",
        "Tahta eşleştirme ve canlı veri aktarımı hızlandırılarak zaman aşımı sorunları giderildi.",
    ),
    (
        "smartboard_disconnect",
        "smartboard",
        False,
        re.compile(r"(SmartBoardHelper|tahta\.html|BirizViewModel)", re.I),
        re.compile(r"(disconnectSession|forceDisconnectToWaiting|disconnectedConnectionId|startConnectedHeartbeat|verifySessionStillConnected)", re.I),
        "Bağlantı Kesme Eşitlemesi",
        "Telefondan akıllı tahta bağlantısı kesildiğinde tahta ekranının anında bekleme moduna dönmesi sağlandı.",
    ),
    (
        "smartboard_qr",
        "smartboard",
        False,
        re.compile(r"(QrScannerDialog)", re.I),
        None,
        "Karekod Okuma Hızı",
        "Kamera ile karekod tarama hızı ve algılama kararlılığı artırıldı.",
    ),
    (
        "smartboard_cast_mirror",
        "smartboard",
        False,
        re.compile(r"(SmartBoardHelper|tahta\.html|InstalledApps|ScreenMirror|Share)", re.I),
        re.compile(r"(boardCast|SCREEN_MIRROR|castFileToBoard|publishScreenMirrorFrame|updateScreenMirrorPauseState)", re.I),
        "Tahtada Ekran ve Dosya Paylaşımı",
        "Seçilen uygulama, dosya veya bağlantıların akıllı tahta ekranına yansıtılması iyileştirildi.",
    ),
    # --- Güncelleme Sistemi ---
    (
        "release_notes_turkish_only",
        "app_update",
        False,
        re.compile(r"(generate_release_notes\.py|AppUpdateManager\.kt|AppUpdateDialog\.kt|release-notes\.json)", re.I),
        re.compile(r"(is_valid_turkish_note|looksLikeNonTurkishNoise|SPECIFIC_DIFF_RULES|get_fresh_json_release_notes)", re.I),
        "Türkçe ve Güncel Sürüm Açıklamaları",
        "Güncelleme açıklamalarının tamamen Türkçe olması ve yalnızca mevcut sürüme ait yenilikleri göstermesi sağlandı.",
    ),
    (
        "app_update_installer",
        "app_update",
        True,
        re.compile(r"(AppUpdate|AppUpdateDownload|AppUpdateInstall|AppUpdateNotification)", re.I),
        None,
        "Tek Dokunuşla Güncelleme",
        "Güncelleme indirme, doğrulama ve tek dokunuşla kurulum akışı iyileştirildi.",
    ),
    # --- Hesap İşlemleri ---
    (
        "auth_existing_role_direct_routing",
        "auth",
        False,
        re.compile(r"(AuthSessionRoutingHelper|FirestoreAuthRestHelper|BirizViewModel|ClassRegistryRepository|GoogleSignInScreen|MainActivity|FirestoreModels)", re.I),
        re.compile(r"(AuthSessionRoutingHelper|FirestoreAuthRestHelper|AccountSessionBookmark|raceFirstMatch|resolveInitialLaunchDecision|resolveAuthenticatedRoute|loadedFromCloud|handleTransientNetworkFailure)", re.I),
        "Hızlı ve Kesintisiz Hesap Girişi",
        "Kayıtlı öğretmen ve veli hesaplarının çok kanallı hızlı doğrulama ile zaman aşımına takılmadan ve rol seçim ekranına düşmeden doğrudan kendi panellerine açılması sağlandı.",
    ),
    (
        "auth_account_switch",
        "auth",
        False,
        re.compile(r"(GoogleAuth|GoogleSignIn|MainActivity\.kt$|BirizViewModel\.kt$)", re.I),
        re.compile(r"(signOut|CredentialManager|switchAccount|handleGoogleSignIn|clearCredentialState)", re.I),
        "Hesap Değiştirme Kolaylığı",
        "Çıkış yaptıktan sonra uygulamayı kapatmadan farklı bir hesapla giriş yapabilme sorunu düzeltildi.",
    ),
    (
        "auth_account_delete",
        "auth",
        False,
        re.compile(r"(UserProfileMenu|MainActivity\.kt$|BirizViewModel\.kt$|ClassRegistryRepository|SmartBoardHelper)", re.I),
        re.compile(r"(deleteAccount|deleteUserSmartBoardTraces|Hesabımı Sil|hesap sil|purgeUser)", re.I),
        "Hesap Silme İşlemi",
        "Hesap silme seçeneğinin kullanıcı, sınıf ve tahta oturum izlerini tüm sistemden temizlemesi sağlandı.",
    ),
    # --- Performans ve Bulut Verimliliği ---
    (
        "cloud_quota_listener_optimization",
        "performance",
        False,
        re.compile(r"(FirestoreListenerHub|FirestoreProvider|ClassRegistryRepository|BackgroundNotificationSyncHelper|CommunicationPersistenceHelper)", re.I),
        re.compile(r"(FirestoreListenerHub|observeActiveClassroom|startSetupWizardObserving|PersistentCacheSettings|DISPATCHED_READ|DEEP_NIGHT)", re.I),
        "Hız, Pil ve Bulut Veri Verimliliği",
        "Paylaşımlı canlı dinleme, büyütülmüş yerel önbellek, sınıf odaklı sorgular ve saatlik akıllı arka plan eşitlemesiyle veri tüketimi ciddi oranda azaltıldı.",
    ),
    # --- Geniş Alan Yedek Kuralları (Sadece o alanda spesifik kural eşleşmezse çalışır) ---
    (
        "star_rewards",
        "stars",
        True,
        re.compile(r"(StarReward|StarsClassroom)", re.I),
        None,
        "Yıldız ve Ödül Takibi",
        "Öğrenci yıldız puanları ve ödül ekranlarındaki veri eşitlemesi güncellendi.",
    ),
    (
        "class_list_students",
        "students",
        True,
        re.compile(r"(ClassList|Student|ClassRegistry)", re.I),
        None,
        "Sınıf Listesi Yönetimi",
        "Öğrenci listesi, sınıf düzeni ve kayıt akışları iyileştirildi.",
    ),
    (
        "tournament",
        "tournament",
        True,
        re.compile(r"(Tournament)", re.I),
        None,
        "Sınıf Turnuvası",
        "Turnuva eşleşmeleri, skor tabloları ve tahta görünümü güncellendi.",
    ),
    (
        "library_reading",
        "library",
        True,
        re.compile(r"(Library|Kitaplik)", re.I),
        None,
        "Kitaplık ve Okuma Takibi",
        "Sınıf kitaplığı ve okuma kayıtları ekranı güncellendi.",
    ),
    (
        "seating_plan",
        "seating",
        True,
        re.compile(r"(SeatingPlan|PlannedSeating)", re.I),
        None,
        "Oturma Planı",
        "Sınıf oturma düzeni ve masa yerleşim ekranı güncellendi.",
    ),
    (
        "schedule_lessons",
        "schedule",
        True,
        re.compile(r"(Schedule)", re.I),
        None,
        "Ders Programı",
        "Haftalık ders programı ve ders saati hatırlatıcıları güncellendi.",
    ),
    (
        "communication_chat",
        "communication",
        True,
        re.compile(r"(Communication|Chat|BulkMessage|Messaging)", re.I),
        None,
        "Veli İletişimi ve Duyurular",
        "Duyuru, anket ve mesajlaşma ekranları iyileştirildi.",
    ),
    (
        "class_tools",
        "tools",
        True,
        re.compile(r"(Timer|LuckyStudent|GroupMaker)", re.I),
        None,
        "Sınıf İçi Etkinlik Araçları",
        "Zamanlayıcı, şanslı öğrenci ve grup oluşturma araçları güncellendi.",
    ),
    (
        "cross_device_notif_read_sync",
        "notifications",
        False,
        re.compile(r"(NotificationLifecycleHelper|CommunicationPersistenceHelper|BirizViewModel|BackgroundNotificationSync|BirizFirebaseMessaging)", re.I),
        re.compile(r"(NotificationReadSyncState|notification_read_state|observeFirestoreNotificationReadSyncState|recordAllNotificationsRead|pruneDismissedNotifications)", re.I),
        "Çoklu Cihaz Bildirim Okundu Eşitlemesi",
        "Bir cihazda okunan veya tümü okundu işaretlenen bildirimlerin bulut üzerinden eşitlenerek aynı hesapla giriş yapılan diğer cihazlarda tekrar baştan gelmesi engellendi.",
    ),
    (
        "teacher_parent_notif_deferral",
        "notifications",
        False,
        re.compile(r"(NotificationPreferences|BackgroundNotificationSync|BirizFirebaseMessaging|BirizViewModel|ClassRegistryRepository)", re.I),
        re.compile(r"(TeacherParentDeferralCard|ParentNotificationDeferralConfig|toggleTeacherParentDeferral|shouldDeferForParent|updateParentNotificationDeferral)", re.I),
        "Öğretmen İçin Veli Bildirimlerini Erteleme (Gece Çalışma Modu)",
        "Öğretmen bildirim ayarlarına, akşam ve gece yapılan sınıf veri girişlerinin velilere anında gitmesini önleyip belirlenen sabah saatinde topluca ileten erteleme özelliği eklendi.",
    ),
    (
        "notifications_quiet",
        "notifications",
        True,
        re.compile(r"(Notification|QuietHours)", re.I),
        None,
        "Bildirim ve Sessiz Saatler",
        "Bildirim iletimi ve sessiz saat kontrolleri düzenlendi.",
    ),
    (
        "portal_dashboard",
        "parent_portal",
        True,
        re.compile(r"(ParentPortal|TeacherRoom|TeacherDashboard|TeacherHome)", re.I),
        None,
        "Öğretmen ve Veli Arayüzü",
        "Ana ekran gezinmesi ve portal kullanım kolaylığı artırıldı.",
    ),
    (
        "ui_theme_assets",
        "theme",
        True,
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

CONVENTIONAL_ENGLISH_PREFIX_RE = re.compile(
    r"^(feat|fix|chore|refactor|style|docs|ci|build|perf|test|revert|merge|sync|update|add|remove|cleanup|wip)(\([^)]*\))?[:\s]",
    re.I,
)

ENGLISH_WORD_CUE_RE = re.compile(
    r"\b(fix|fixed|fixes|add|added|adds|update|updated|updates|remove|removed|refactor|refactored|feature|features|bug|bugs|issue|issues|crash|sync|synced|merge|merged|pull|request|branch|commit|commits|release|changelog|support|improve|improved|improves|change|changed|changes|clean|cleanup|file|files|screen|screens|button|dialog|from|with|into|for|the|and|when|after|before)\b",
    re.I,
)

CODE_OR_TECH_NOISE_RE = re.compile(
    r"(\.(kt|java|py|json|xml|yml|yaml|html|js|gradle|kts)\b|pull request #|co-authored-by|signed-off-by)",
    re.I,
)

TURKISH_CHAR_RE = re.compile(r"[çğıöşüÇĞİÖŞÜ]")


def is_valid_turkish_note(text: str) -> bool:
    """Commit veya ham metnin gerçekten kullanıcıya dönük Türkçe bir cümle olduğunu doğrular."""
    cleaned = text.strip()
    if len(cleaned) < 8:
        return False
    if GENERIC_COMMIT_RE.match(cleaned):
        return False
    if CONVENTIONAL_ENGLISH_PREFIX_RE.match(cleaned):
        return False
    if CODE_OR_TECH_NOISE_RE.search(cleaned):
        return False
    if ENGLISH_WORD_CUE_RE.search(cleaned):
        return False
    if not TURKISH_CHAR_RE.search(cleaned):
        # Türkçe özel harf içermiyorsa İngilizce olma ihtimali yüksektir; güvenli tarafta kalıp reddet
        return False
    return True


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

    return ""


def clean_bullet_line(line: str, require_turkish: bool = True) -> tuple[str, str] | None:
    text = re.sub(r"^[\s\-*•]+", "", line).strip()
    if not text:
        return None
    text = text.replace("**", "").strip()
    if require_turkish and not is_valid_turkish_note(text):
        return None
    if ":" in text:
        head, body = text.split(":", 1)
        head = head.strip()
        body = body.strip()
        if head and body and 2 <= len(head) <= 48:
            if require_turkish and (ENGLISH_WORD_CUE_RE.search(head) or ENGLISH_WORD_CUE_RE.search(body)):
                return None
            return head, body
    return "Yenilik", text


def deduplicate_items(items: list[dict[str, str]], limit: int = 6) -> list[dict[str, str]]:
    unique: list[dict[str, str]] = []
    seen_titles: set[str] = set()
    seen_summaries: set[str] = set()
    for it in items:
        t = it.get("title", "").strip()
        s = it.get("summary", "").strip()
        if not t or not s:
            continue
        norm_t = re.sub(r"\s+", " ", t.lower())
        norm_s = re.sub(r"\s+", " ", s.lower())
        if norm_t in seen_titles or norm_s in seen_summaries:
            continue
        seen_titles.add(norm_t)
        seen_summaries.add(norm_s)
        unique.append({"title": t, "summary": s})
        if len(unique) >= limit:
            break
    return unique


def parse_manual_notes(raw: str) -> tuple[str, list[dict[str, str]]]:
    items: list[dict[str, str]] = []
    for line in raw.splitlines():
        parsed = clean_bullet_line(line, require_turkish=False)
        if parsed:
            items.append({"title": parsed[0], "summary": parsed[1]})
    items = deduplicate_items(items, limit=6)
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
                if t and s and not CODE_OR_TECH_NOISE_RE.search(f"{t} {s}"):
                    items.append({"title": t, "summary": s})
        items = deduplicate_items(items, limit=6)
        if items:
            return (title or "Yeni Güncelleme ve İyileştirmeler"), items, target_version
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
        # Etiket bulunamazsa yalnızca son commit içinde release-notes.json gerçekten güncellendiyse kullan
        has_parent = bool(run_git(["rev-parse", "--verify", "HEAD~1"]))
        if has_parent:
            changed_recent = run_git(["diff", "--name-only", "HEAD~1..HEAD", "--", filepath])
            if not changed_recent:
                return None
            prev_raw = run_git(["show", f"HEAD~1:{filepath}"])
            if prev_raw:
                prev_loaded = load_json_release_notes(prev_raw)
                if prev_loaded and prev_loaded[0] == title and prev_loaded[1] == items:
                    return None
        else:
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

    # Önceki sürümlere ait commitlerin karışmaması için yalnızca güncel aralıktaki commitleri al
    log_args = (
        ["log", rev_range, "--no-merges", "--pretty=format:%s"]
        if rev_range
        else ["log", "-n", "1", "--no-merges", "--pretty=format:%s"]
    )
    commit_msgs = [m.strip() for m in run_git(log_args).splitlines() if m.strip()]

    scored_items: list[tuple[int, str, str]] = []
    seen_titles: set[str] = set()
    matched_specific_areas: set[str] = set()

    # 1. Geçerli, tamamen Türkçe ve "Başlık: Açıklama" biçimli güncel commit mesajları
    for msg in commit_msgs:
        if ":" in msg and is_valid_turkish_note(msg):
            parsed = clean_bullet_line(msg, require_turkish=True)
            if parsed and parsed[0] not in seen_titles:
                seen_titles.add(parsed[0])
                scored_items.append((10_000, parsed[0], parsed[1]))

    # 2. Diff tabanlı ince taneli analiz (spesifik kurallar önce, geniş yedekler sonra)
    diff_cache: dict[str, str] = {}
    ordered_rules = [r for r in SPECIFIC_DIFF_RULES if not r[2]] + [r for r in SPECIFIC_DIFF_RULES if r[2]]

    for _rule_id, area_group, is_broad, file_re, diff_re, title, summary in ordered_rules:
        if title in seen_titles:
            continue
        if is_broad and area_group in matched_specific_areas:
            # Aynı alanda spesifik bir değişiklik zaten yakalandı; genel ifadeyi tekrar etme
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
            weight += 120  # Özgün içerik eşleşmesine öncelik ver

        if not is_broad:
            matched_specific_areas.add(area_group)
        seen_titles.add(title)
        scored_items.append((weight, title, summary))

    scored_items.sort(key=lambda x: x[0], reverse=True)
    items = deduplicate_items([{"title": t, "summary": s} for _, t, s in scored_items], limit=5)

    # 3. Eğer hâlâ boşsa yalnızca geçerli Türkçe commit mesajlarına bak
    if not items:
        fallback_commits: list[dict[str, str]] = []
        for msg in commit_msgs:
            if is_valid_turkish_note(msg):
                parsed = clean_bullet_line(msg, require_turkish=True)
                if parsed:
                    fallback_commits.append({"title": parsed[0], "summary": parsed[1]})
        items = deduplicate_items(fallback_commits, limit=4)

    # 4. Son çare: Dosya adı veya İngilizce terim içermeyen temiz Türkçe genel özet
    if not items:
        items = [
            {
                "title": "Performans ve Kararlılık İyileştirmeleri",
                "summary": "Uygulama hızı, ekran geçişleri ve genel kullanım kararlılığı iyileştirildi.",
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

    items = deduplicate_items(items, limit=6)
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
