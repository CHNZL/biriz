# Uygulama Planı: Profesyonel ve Modern Sınıf Zamanlayıcısı (Timer Modülü)

Öğretmenlerin ders anlatımı, sınav yönetimi, grup çalışmaları ve serbest okuma etkinliklerinde hem telefondan pratikçe yönetebileceği hem de akıllı tahtaya canlı yansıtabileceği modern, şık, dairesel animasyonlu ve sesli/titreşimli bir **Zamanlayıcı (Timer)** modülü geliştirilecektir.

---

## 1. Kullanıcı Tercihleri ve Tasarım Yönü
- **Ana Odak**: Geri Sayım Sayacı, Hızlı Hazır Süreler ve Özel Sınıf Modları:
  - 📝 **Sınav Sayacı** (Örn: 20 dk, 30 dk, 40 dk - sessiz, odaklayıcı mor/gece mavisi tema)
  - 👥 **Grup Etkinliği** (Örn: 5 dk, 10 dk, 15 dk - dinamik turkuaz/yeşil tema)
  - 📖 **Kitap Okuma Saati** (Örn: 15 dk, 20 dk - huzurlu kehribar/sıcak tema)
  - ⏱️ **Serbest Hızlı Sayaç** (1 dk, 2 dk, 3 dk, 5 dk, 10 dk ve özel dakika/saniye girişi)
- **Görsel Tasarım**:
  - Merkezde Canvas tabanlı, parlayan neon gradient dairesel ilerleme halkası (`DrawScope.drawArc`).
  - Ortasında büyük, kalın, tabular dijital sayaç (`05:00`).
  - Süre azaldıkça yumuşak renk geçişi (Turkuaz ➔ Son %20'de Kehribar ➔ Son 10 saniyede Canlı Kırmızı ve hafif nabız/pulse efekti).
- **Aksiyonlar & Kontroller**:
  - Başlat / Duraklat (büyük konforlu ana buton), Sıfırla, Hızlı `+1 Dk` ve `+5 Dk` ekleme butonları.
  - Tam Ekran / Sunum Modu (telefon masaya konulduğunda sadece dev saat ve çember görünümü).
  - Süre bittiğinde hoş bir zil/bip sesi (`ToneGenerator` / `RingtoneManager`), titreşim (`Vibrator`) ve görsel konfeti kutlaması.
- **Akıllı Tahta Canlı Yansıtma**:
  - Telefondaki sayaç durumu, kalan süre ve seçili mod (`Sınav`, `Okuma`, `Grup`) akıllı tahta Firestore oturumuna (`board_sessions/{sessionCode}`) canlı yazılacak ve bilgisayar ekranında da senkron akacaktır.

---

## 2. Yapılacak Değişiklikler

### Dosya 1: `app/src/main/java/com/example/ui/screens/TimerSubScreen.kt` (Yeni Ekran)
- **Mod Seçici Sekmeleri**: "Hızlı Sayaç", "📝 Sınav", "👥 Grup Etkinliği", "📖 Okuma Saati".
- **Dairesel İlerleme Çemberi (Circular Progress)**:
  - Canvas ile çizilen pürüzsüz yay animasyonu, başlangıç ve bitiş süresine göre oranlanan doluluk.
  - Kalan süre formatlaması (`mm:ss` veya `hh:mm:ss`).
- **Hızlı Süre Çipleri**:
  - `1 Dk`, `2 Dk`, `3 Dk`, `5 Dk`, `10 Dk`, `15 Dk`, `20 Dk`, `30 Dk`, `40 Dk`.
- **Özel Süre Giriş Diyaloğu**:
  - Dakika ve saniyeyi klavyeden veya artır/azalt tuşlarıyla elle girme imkanı.
- **Canlı Kontrol Paneli**:
  - Başlat / Duraklat (Play/Pause), Sıfırla (Reset), `+1 Dk`, `+5 Dk`.
  - Ses ve Titreşim açma/kapama anahtarları.
  - Tam Ekran Sunum Modu açma/kapama.
- **Süre Bitiş Uyarı Modalı**:
  - "Süre Doldu! 🔔" bildirimi, konfeti animasyonu ve tek dokunuşla yeni süre başlatma.

### Dosya 2: `app/src/main/java/com/example/util/SoundAlertHelper.kt` (Yardımcı Sınıf)
- Süre bittiğinde sistemin yerleşik bildirim sesini çalma (`RingtoneManager` veya `ToneGenerator` ile sıfır harici bağımlılık, anında ve güvenilir ses).
- `Vibrator` ile hafif dokunsal titreşim geri bildirimi.

### Dosya 3: `app/src/main/java/com/example/ui/screens/TeacherDashboardScreen.kt`
- `DashboardSubPage.ZAMANLAYICI` durumunda `ModulePreviewsSubScreen` yerine tam fonksiyonel `TimerSubScreen` çağrısı yapılacak.
- Akıllı tahtaya canlı zamanlayıcı senkronizasyonu bağlanacak.

### Dosya 4: `app/src/main/assets/tahta.html`
- Tahtadaki zamanlayıcı şablonu güncellenerek öğretmenin telefonundaki mod adı ("Sınav", "Okuma Saati", "Grup Çalışması") ve telefonla birebir saniye senkronizasyonu sağlanacak.

---

## 3. Doğrulama Planı
1. **Derleme Kontrolü**:
   - `compile_applet` ile projenin sorunsuz derlendiği doğrulanacak.
2. **Birim Testleri**:
   - `gradle :app:testDebugUnitTest --tests com.example.ExampleUnitTest` ile tüm zamanlayıcı formatlama, saniye hesaplama ve geri sayım mantık testleri çalıştırılacak.
3. **Fonksiyonel Doğrulama**:
   - Sayacın başlatılması, duraklatılması, sıfırlanması, `+1 Dk` / `+5 Dk` butonlarının çalışması.
   - Sınav, Grup Etkinliği ve Okuma Saati modlarının hazır sürelerinin yüklenmesi.
   - Açık ve Koyu tema uyumluluğu, tam ekran modu ve süre bitiş ses/titreşim bildirimleri.
