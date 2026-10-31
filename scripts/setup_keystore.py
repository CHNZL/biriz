#!/usr/bin/env python3
import base64
import os
import subprocess

# BİRİZ resmi Firebase onaylı debug.keystore Base64 metni
# SHA1: A6:6B:73:6F:8C:70:69:9F:7A:69:5B:27:54:7E:1D:E7:7F:D5:08:9D
OFFICIAL_KEYSTORE_B64 = "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"

def main():
    b64_content = ""
    for path in ["debug.keystore.base64", "scripts/biriz_keystore.b64"]:
        if os.path.exists(path):
            print(f"Mevcut {path} dosyası okunuyor...")
            with open(path, "r") as f:
                b64_content = f.read().strip()
            if b64_content:
                break
    
    if not b64_content:
        print("Base64 dosyası bulunamadı, gömülü resmi BİRİZ anahtarı kullanılıyor...")
        b64_content = OFFICIAL_KEYSTORE_B64

    # Whitespace ve satır sonlarını temizle
    b64_clean = "".join(b64_content.split())
    rem = len(b64_clean) % 4
    if rem != 0:
        b64_clean += "=" * (4 - rem)

    try:
        keystore_bytes = base64.b64decode(b64_clean)
    except Exception as e:
        print(f"Base64 çözme uyarısı ({e}), gömülü resmi BİRİZ anahtarı kullanılıyor...")
        keystore_bytes = base64.b64decode(OFFICIAL_KEYSTORE_B64)

    with open("debug.keystore", "wb") as f:
        f.write(keystore_bytes)

    print(f"debug.keystore oluşturuldu ({len(keystore_bytes)} bayt).")

    try:
        res = subprocess.run(
            ["keytool", "-list", "-v", "-keystore", "debug.keystore", "-storepass", "android", "-alias", "androiddebugkey"],
            capture_output=True,
            text=True,
            check=True
        )
        for line in res.stdout.splitlines():
            if "SHA1:" in line:
                print(f"Doğrulanan Sertifika Parmak İzi: {line.strip()}")
                break
    except Exception as e:
        print(f"keytool kontrol uyarısı: {e}")

if __name__ == "__main__":
    main()
