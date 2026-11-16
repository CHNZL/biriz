# Cloudflare AI İçin Hazır Prompt (cihanogretmen.com/tahta — Error 1101 Çözümü)

Aşağıdaki metni olduğu gibi kopyalayıp Cloudflare AI'a (veya Cloudflare projenizdeki yapay zekaya) verin:

---

`https://cihanogretmen.com/tahta` adresine girildiğinde **Cloudflare Error 1101 (Worker threw exception)** hatası alıyorum. Amacım bu Worker hatasını kalıcı olarak çözmek ve `/tahta` yolunun `public/tahta.html` dosyasını eksiksiz, hatasız bir şekilde sunmasını sağlamak.

Lütfen mevcut Cloudflare Worker / Pages / Wrangler yapılandırmamı incele ve aşağıdaki kurallara **harfiyen** uyarak düzelt:

### 1. Kesin Kurallar (Dokunulmayacaklar)
- **`public/tahta.html` dosyasının HTML, CSS ve JavaScript mantığını KESİNLİKLE değiştirme, sadeleştirme veya eski sürüme döndürme.**
  - Bu dosya zaten hazır; Firebase Modular SDK (`10.12.2`) + isimli Firestore veritabanı (`ai-studio-android-biriz-caa1dae7-0ec2-4613-b717-5606838b083e`) + `(default)` fallback + REST API fallback içeriyor, rastgele 8 karakterli kod üretiyor ve içinde hiç backtick (`` ` ``) veya `${...}` şablon dizgisi barındırmıyor.
- Sitedeki (`/`, `/api/*` vb.) diğer çalışan rotaları bozma.

### 2. Error 1101 Nedenini Tespit Et ve Gider
Mevcut Worker kodunda `Error 1101` fırlatan hatayı bul (genellikle `env.ASSETS` tanımsız olması, `fetch(...)` sırasında yakalanmayan exception, HTML'in backtick/template literal içine gömülürken sözdizimi/runtime hatası vermesi veya yanlış URL yeniden yazımı).

Projede kullandığın mimariye göre aşağıdaki yöntemlerden uygun olanı uygula:

#### Durum A: Projede Cloudflare Workers Static Assets (`wrangler.toml` / `wrangler.jsonc`) kullanılıyorsa
1. `wrangler.toml` / `wrangler.jsonc` içinde statik varlık klasörünü `./public` olarak bağla (`assets = { directory = "./public", binding = "ASSETS" }`).
2. Worker `fetch(request, env, ctx)` fonksiyonunu mutlaka `try / catch` içine al.
3. `/tahta` ve `/tahta/` isteklerini içsel olarak `/tahta.html` dosyasına yönlendir (URL tarayıcıda değişmesin, doğrudan HTML sunulsun).
4. `env.ASSETS` yoksa veya alt fetch hata verirse `Error 1101` fırlatmak yerine kontrollü HTTP durum kodu döndür.
5. HTML yanıtlarında şu başlıkları kullan:
   - `Content-Type: text/html; charset=UTF-8`
   - `Cache-Control: no-cache, no-store, must-revalidate` (güncellemelerin anında görünmesi için)

Örnek güvenli Worker yapısı:
```js
export default {
  async fetch(request, env) {
    try {
      const url = new URL(request.url);

      if (url.pathname === "/tahta" || url.pathname === "/tahta/") {
        url.pathname = "/tahta.html";
      }

      if (!env.ASSETS || typeof env.ASSETS.fetch !== "function") {
        return new Response("Static ASSETS binding bulunamadi.", {
          status: 500,
          headers: { "content-type": "text/plain; charset=UTF-8" }
        });
      }

      const assetResponse = await env.ASSETS.fetch(new Request(url.toString(), request));

      // HTML yanitlarinda tarayici onbellegini engelle ve UTF-8 garanti et
      if (url.pathname === "/tahta.html" || url.pathname === "/index.html" || url.pathname === "/") {
        const headers = new Headers(assetResponse.headers);
        headers.set("Content-Type", "text/html; charset=UTF-8");
        headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
        return new Response(assetResponse.body, {
          status: assetResponse.status,
          statusText: assetResponse.statusText,
          headers
        });
      }

      return assetResponse;
    } catch (err) {
      return new Response("Worker hatasi: " + (err && err.message ? err.message : String(err)), {
        status: 500,
        headers: { "content-type": "text/plain; charset=UTF-8" }
      });
    }
  }
};
```

#### Durum B: Worker doğrudan harici bir HTML / Raw URL veya KV / R2 kaynağı çekiyorsa
- `fetch(...)` sonucunu ve `res.ok` durumunu mutlaka kontrol et, `try / catch` ile sar, boş/undefined gövdeyi işleme.
- Eğer `public/tahta.html` dosyasını Worker içine import ediyorsan (`import tahtaHtml from "./public/tahta.html";`), Wrangler'da `.html` dosyalarını `Text` modülü olarak tanımla (`[[rules]] type = "Text" globs = ["**/*.html"]`) ve HTML'i `new Response(tahtaHtml, { status: 200, headers: { "Content-Type": "text/html; charset=UTF-8", "Cache-Control": "no-cache, no-store, must-revalidate" } })` şeklinde döndür; HTML metnini string birleştirme veya şablon dizgisi (`...`) ile bozma.

#### Durum C: Cloudflare Pages kullanılıyorsa
- **Build output directory** ayarının `public` olduğundan emin ol.
- Eğer çakışan eski bir Worker Route (`cihanogretmen.com/tahta*` veya `cihanogretmen.com/*`) Pages'in önüne geçip `Error 1101` üretiyorsa, bu Worker'ı düzelt ya da `/tahta` rotasını doğrudan Pages statik dosyasına bırak.

### 3. Güvenlik Başlıkları (CSP Kontrolü)
Worker yanıtına `Content-Security-Policy` (CSP) başlığı ekliyorsan, `tahta.html`'in canlı tahta ve QR kod özelliklerinin çalışması için şu kaynaklara izin verdiğinden emin ol (CSP eklemiyorsan ekstra kısıtlama ekleme):
- `script-src`: `'self' 'unsafe-inline' https://www.gstatic.com https://cdn.jsdelivr.net`
- `connect-src`: `'self' https://firestore.googleapis.com https://*.googleapis.com https://www.gstatic.com https://*.firebaseio.com wss://*.firestore.googleapis.com`
- `style-src`: `'self' 'unsafe-inline' https://fonts.googleapis.com`
- `font-src`: `'self' https://fonts.gstatic.com data:`
- `img-src`: `'self' data: https://api.qrserver.com`

### 4. Son Doğrulama
Değişiklikten sonra şunları test edip bana sonucu raporla:
1. `/tahta` ve `/tahta/` istekleri `HTTP 200` ve `Content-Type: text/html; charset=UTF-8` dönüyor mu?
2. `Error 1101` tamamen kalktı mı?
3. Dönen HTML gövdesinde `BİRİZ` Akıllı Tahta sayfası eksiksiz geliyor mu?
