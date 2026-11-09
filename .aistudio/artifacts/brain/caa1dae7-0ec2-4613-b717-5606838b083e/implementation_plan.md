# BİRİZ Veli Modülü & KVKK Veri Gizliliği Mimarisi Uygulama Planı

Bu plan, öğretmen modülünde üretilen zengin öğrenci gelişim verilerini (yıldızlar, kitap okuma, haftalık ders programı, oturma planı, turnuvalar, duyurular ve birebir iletişim) velilere **%100 KVKK ve Veri Gizliliği** prensipleriyle, modern bir **Bento-Grid Ana Sayfa Dashboard'u** ve **Derinlemesine Detay Ekranları** eşliğinde sunmayı amaçlar.

---

## 1. Temel Prensipler & KVKK Veri Güvenliği Mimarisi

> [!CRITICAL]
> **KVKK & Veri Gizliliği Kuralı:** Hiçbir veli, hangi ekranda olursa olsun (sıralamalar, oturma planı, turnuva eşleşmeleri, kitap listeleri) sınıftaki diğer öğrencilerin adını, soyadını, numarasını veya kişisel detaylarını **asla** göremez. Veli yalnızca kendi velisi olduğu öğrencinin/öğrencilerin (kardeşler dahil) bilgilerini net görür; sınıftaki tüm diğer öğrenciler istisnasız **"Öğrenci"** (veya anonim ikon) olarak maskelenir.

### Veri Maskeleme Motoru (`ParentPrivacyHelper.kt`)
* **Öğrenci İsmi Maskeleme:** `maskStudentName(studentId, studentName, linkedStudentIds)`:
  * Eğer `studentId` velinin bağlı çocuklarından biri ise: Gerçek tam isim (örn. `ELİF YAZICI`).
  * Değilse: Sabit `"Öğrenci"` etiketi.
* **Öğrenci Numarası Maskeleme:**
  * Kendi çocuğu ise: Gerçek okul numarası (örn. `14`).
  * Değilse: `"-"` veya gizli.
* **Sıralama ve İstatistik Gösterimi:**
  * Çocuğun başarı derecesi korunur: *"Sınıfta 3. Sırada • İlk %10'luk Dilimde"*.
  * Sınıf lider tablosunda velinin çocuğu belirgin renk ve rozetle parıldarken, 1., 2., 4., 5. sıralardaki isimler `"Öğrenci"` olarak gösterilir. Bu sayede pedagojik motivasyon korunurken KVKK ihlali sıfıra indirilir.

---

## 2. Kullanıcı Deneyimi (UX) & Veli Ana Sayfası (Dashboard)

Veli uygulamaya girdiğinde, karmaşık menüler yerine çocuğunun okul hayatını bir bakışta gösteren sıcak ve modern bir **Bento-Grid Güncel Bilgi Hub'ı** ile karşılanır:

### A. Üst Bölüm (Header & Kardeş Seçici)
* **Kişisel Selamlama:** *"İyi günler, Zeynep Hanım 👋 • BİRİZ Veli Portalı"*.
* **Sınıf & Öğretmen Kimliği:** *"Süleyman Sami Kepenek İlkokulu • 4/D Sınıfı • Öğr: Cihan Özel"*.
* **Kardeş / Çocuk Seçici Çubuğu:** Velinin birden fazla çocuğu varsa tek dokunuşla çocuk değiştirme (örn. `👧 Elif (4/D)` ↔ `👦 Ali (1/B)`) ve *"➕ Kardeş Ekle (8 Haneli Kod)"* butonu.
* **Hızlı Aksiyonlar:** 🔔 Bildirim zili ve 👤 Profil/Tema ayarları.

### B. "Bugün Çocuğum" Canlı Özet Kartları (Bento Dashboard)
Her bir kart güncel durumu özetler ve tıklandığında ilgili modülün tam ekran detayına yönlendirir:

1. **⭐ Güncel Yıldız & Gelişim Kartı (Hero Bento):**
   * Toplam yıldız sayısı, sınıf seviye rozeti (örn. `🥇 Sınıf Yıldızı`).
   * Öğretmenin verdiği **en son yıldızın açıklaması**, kategorisi ve saati (örn. *"Matematik problem çözme gayreti (+3 ⭐) - 2 saat önce"*).
   * *Tıklanınca:* Yıldız Geçmişi ve Kazanım Analizi Detayına gider.

2. **📅 Bugünkü Dersler & Zaman Akışı Kartı:**
   * Bugünün tarihi ve haftalık programdaki günün dersleri (örn. *1. Türkçe, 2. Matematik, 3. Fen Bilimleri...*).
   * Çantaya konulacak ders araç-gereçleri hatırlatması.
   * *Tıklanınca:* 5 Günlük Tam Haftalık Ders Programı & Zil Saatleri Detayına gider.

3. **📖 Kitaplık & Okuma Durumu Kartı:**
   * Öğrencinin şu an okumakta olduğu aktif kitap (Kitap adı, sayfa sayısı).
   * Bu ay okunan toplam kitap sayısı ve okuma serisi rozeti.
   * *Tıklanınca:* Kitaplık, Okuma Karnesi ve Sınıf Kitap Kataloğu Detayına gider.

4. **🪑 Sınıftaki Oturma Yeri Kartı:**
   * Mini sınıf konumu şeridi: *"Orta Sıra • 2. Sıra (Tahta Önü)"*.
   * *Tıklanınca:* Kuş bakışı interaktif sınıf yerleşim planına gider.

5. **🏆 Turnuva & Etkinlik Kartı (Aktif Turnuva Varsa):**
   * Çocuğun katıldığı turnuva durumu (örn. *"Mangala Turnuvası • Yarı Final Maçı Bekleniyor ⏳"* veya *"Kürsü: 1. Şampiyonluk 🥇"*).
   * Rakipler KVKK gereği anonimleştirilmiştir.
   * *Tıklanınca:* Turnuva Fikstürü ve Canlı Podyum Detayına gider.

6. **📢 Öğretmenden Son Duyuru & Anket Kartı:**
   * Sınıf öğretmeninin paylaştığı en son duyuru başlığı ve özeti.
   * Eğer açık bir veli anketi varsa *"Oyunuzu Kullanın 🗳️"* acil durum etiketi.
   * *Tıklanınca:* Tüm Duyurular ve Anket Katılım Ekranına gider.

7. **💬 Öğretmenle İletişim & Son Mesaj Kartı:**
   * Sınıf öğretmeninden gelen son mesaj önizlemesi ve tek dokunuşla hızlı cevap alanı.
   * *Tıklanınca:* Birebir Güvenli Sohbet Ekranına gider.

---

## 3. Detay Ekranları Mimarisi (Drill-Down Views)

Özet kartlarına veya alt gezinme çubuğundaki sekmelere tıklandığında açılacak zengin detay sayfaları:

```
Veli Portalı
├── 🏠 Ana Sayfa (Bento Dashboard)
├── ⭐ Gelişim & Yıldızlar Detayı
│   ├── Kategori Dağılımı Grafiği (Ders, Saygı, Beden Eğitimi vb.)
│   ├── Öğretmen Notları Zaman Çizelgesi
│   └── KVKK Uyumlu Sınıf Derecesi (Kendi Çocuğu Vurgulu, Diğerleri Anonim)
├── 📚 Kitaplık & Okuma Detayı
│   ├── Okunan Kitaplar Arşivi ve Sayfa Sayıları
│   ├── Kazanılan Okuma Yıldızları
│   └── Sınıf Kitap Kataloğu (Ödünç Alınabilir Kitaplar)
├── 📅 Ders Programı & Zaman Çizelgesi Detayı
│   ├── 5 Günlük Tam Haftalık Çizelge
│   ├── Ders Başlama, Teneffüs ve Öğle Arası Saatleri
│   └── Ders Öğretmenleri Listesi
├── 🪑 Oturma Planı Detayı
│   ├── Kuş Bakışı Sınıf Krokisi (Öğretmen Masası, Yazı Tahtası, Kapı, Pencereler)
│   ├── Çocuğun Sırası Yeşil/Turkuaz Vurgulu ("Çocuğunuz")
│   └── Diğer Sıralar Nötr Gri ("Öğrenci")
├── 🏆 Turnuva & Yarışma Detayı
│   ├── Çocuğun Eşleşmeleri, Maç Sonuçları ve Tur Atlama Durumu
│   └── Şampiyonluk Kürsüsü (Kendi Çocuğu Derecedeyse İsmiyle, Diğerleri "Öğrenci")
└── 💬 İletişim, Duyuru & Anket Detayı
    ├── Sınıf Duyuruları & Ekler
    ├── Veli Anketleri Oylama Alanı
    └── Öğretmenle Birebir Güvenli Mesajlaşma (Sohbet Balonları)
```

---

## 4. Uygulama Adımları (Teknik Yol Haritası)

### Adım 1: KVKK & Maskeleme Yardımcısı (`ParentPrivacyHelper.kt`)
* Öğrenci isimlerini ve numaralarını velinin bağlı çocuk kimliklerine (`linkedStudentIds`) göre süzen yardımcı sınıfın oluşturulması.
* Oturma planı, turnuva ve sıralama ekranlarında maskeleme fonksiyonlarının entegrasyonu.

### Adım 2: Bento Dashboard & Ana Sayfa Yeniden Tasarımı (`ParentPortalSubScreen.kt`)
* Mevcut tab yapısını modern bir **Bento Dashboard + Drill-Down Detay Mimarisi**ne dönüştürme.
* "Bugün Çocuğum" üst başlık kartı, hızlı durum göstergeleri ve canlı veri kartları.
* Kartlara tıklanınca detay sayfalarına pürüzsüz geçiş sağlama ve Android `BackHandler` ile ana sayfaya dönüşü güvenceye alma.

### Adım 3: Modül Detay Ekranlarının Güçlendirilmesi
* **Yıldızlar Detayı:** Zaman çizelgesi ve kategori filtreleme.
* **Oturma Planı Detayı:** `SeatingPlanHelper` verilerini okuyarak görsel sınıf yerleşim kroki çizimi (öğretmen tahtası referanslı).
* **Kitaplık Detayı:** Öğrencinin okuduğu kitaplar ve sınıf kütüphanesinden seçebileceği kitaplar listesi.
* **Ders Programı Detayı:** `ScheduleHelper` ile güncel haftalık tablo ve zil saatleri.
* **Turnuva Detayı:** `TournamentPersistenceHelper` ile aktif turnuvanın maçları ve çocuğun tur durumu.

### Adım 4: Birim Testler & Derleme Doğrulaması
* `ParentPrivacyHelperTest` ile farklı veli kimliklerinde diğer öğrencilerin kesinlikle maskelendiğinin test edilmesi.
* Kardeş geçişi ve detay navigasyonu testleri.
* `compile_applet` ile hatasız derlemenin doğrulanması.
