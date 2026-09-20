# StickLy

Aplikasi Android untuk membuat paket stiker WhatsApp langsung dari teks.
Stiker dibuat & diedit di dalam aplikasi, disimpan secara lokal, lalu diekspor ke WhatsApp
melalui `ContentProvider` (API stiker pihak ketiga resmi WhatsApp).

## Fitur

- Membuat stiker dari teks: pilih sumber teks, atur font/ukuran/warna, posisi, dan outline.
- Multi-baris teks dalam satu stiker.
- Simpan stiker ke dalam paket (3–30 stiker per paket sebagai syarat ekspor WhatsApp).
- Kelola paket: ubah nama/publisher, ganti emoji stiker, hapus stiker/paket.
- Ekspor ke WhatsApp: tombol "Tambah ke WhatsApp" memanggil `com.whatsapp.intent.action.ENABLE_STICKER_PACK`.
- Deteksi WhatsApp/WhatsApp Business sudah terpasang dan menyegarkan UI setiap kali app kembali ke foreground (`ON_RESUME`).

## Persyaratan Build

- JDK 21
- Android SDK dengan platform 35
- Koneksi ke repo Maven (proyek ini memakai mirror Huawei Cloud)

> Catatan lingkungan pengembangan saat ini: `services.gradle.org` dan `dl.google.com` tidak
> bisa diakses dari jaringan lokal, sehingga Semua repo Maven memakai
> `https://mirrors.huaweicloud.com/repository/maven/` (lihat `settings.gradle.kts`).
> SDK platform 35 menggunakan shim lokal (jangan di-upgrade ke 36).

### Build (Windows, PowerShell)

```powershell
$env:JAVA_HOME = "D:\mobile-project\.tools\jdk-21.0.2"
$env:GRADLE_USER_HOME = "D:\mobile-project\.tools\gradle-home"
& "D:\mobile-project\gradlew.bat" -p D:\mobile-project assembleDebug --console=plain --no-daemon
```

Hasil: `app/build/outputs/apk/debug/app-debug.apk`

Install ke perangkat:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

## Profil Aplikasi

| Item | Nilai |
|---|---|
| `applicationId` / `namespace` | `com.padil.stickly` |
| `minSdk` / `targetSdk` / `compileSdk` | 26 / 35 / 35 |
| Authority ContentProvider stiker | `com.padil.stickly.stickercontentprovider` (dari `manifestPlaceholders` di `app/build.gradle.kts`) |
| Posisi penyimpanan data | `files/stickly_store.json` (deskripsi paket) + `files/stickers/<packId>/*.webp` (file aset) |

## Struktur Proyek (inti)

```
app/src/main/java/com/padil/stickly/
├── MainActivity.kt              # entry point + intent-filter ADD_STICKER_PACK
├── StickLyApplication.kt        # inisialisasi repository
├── data/
│   ├── StickerRepository.kt     # CRUD paket & stiker, persistensi JSON + file WebP
│   └── StickerContentProvider.kt# provider stiker yang dibaca WhatsApp
├── sticker/
│   ├── StickerProject.kt        # model proyek editor
│   ├── StickerRenderer.kt       # render teks → Bitmap
│   ├── StickerCodec.kt          # encode/decode WebP, tray 96x96
│   └── StickerValidator.kt      # validasi WebP 512x512 ≤100KB
├── ui/                          # layar Compose (Home, Editor, Pack, Info)
└── whatsapp/
    └── WhatsAppApi.kt           # deteksi install, intent ekspor, cek status whitelist
```

## Integrasi WhatsApp

### Persyaratan paket yang diterima WhatsApp

| Aturan | Nilai |
|---|---|
| Jumlah stiker per paket | 3 – 30 |
| Format stiker | WebP statis, **512×512 px**, ≤ 100 KB |
| File tray (ikon paket) | WebP **96×96 px**, ≤ 50 KB |
| Emoji stiker | Minimal 1 emoji per stiker |

Data yang disimpan memenuhi aturan ini akan lolos validasi internal WhatsApp saat menekan
"Tambahkan ke WhatsApp".

### Manifest

- `<queries>` mendeklarasikan `com.whatsapp` dan `com.whatsapp.w4b` supaya resolusi intent dan
  query whitelist berfungsi di Android 11+.
- `StickerContentProvider` diekspor dengan `android:readPermission="com.whatsapp.sticker.READ"`
  (izin resmi yang diminta WhatsApp).
- `MainActivity` menyediakan intent-filter `com.whatsapp.intent.action.ADD_STICKER_PACK`.

### Kontrak kolom ContentProvider

`StickerContentProvider` menyajikan endpoint resmi seperti kontrak di
repo resmi [WhatsApp/stickers](https://github.com/WhatsApp/stickers):

| Endpoint | Keterangan |
|---|---|
| `/metadata` | Semua paket |
| `/metadata/<identifier>` | Metadata satu paket |
| `/stickers/<identifier>` | Daftar stiker satu paket |
| `/stickers_asset/<identifier>/<file>` | Konten biner WebP |

Kolom metadata (nama resmi yang dibaca WhatsApp):

```
sticker_pack_identifier, sticker_pack_name, sticker_pack_publisher, sticker_pack_icon,
android_play_store_link, ios_app_download_link, sticker_pack_publisher_email,
sticker_pack_publisher_website, sticker_pack_privacy_policy_website,
sticker_pack_license_agreement_website, image_data_version,
whatsapp_will_not_cache_stickers, animated_sticker_pack
```

Kolom stiker:

```
sticker_file_name, sticker_emoji, sticker_accessibility_text
```

> Catatan: kolom emoji resmi adalah **`sticker_emoji`**, bukan `sticker_file_emoji`.
> Menggunakan nama kolom yang salah membuat WhatsApp menolak paket secara diam-diam
> (probe menunjukkan WA berhenti di query kedua tanpa membuka file stiker).
> Provider juga tetap menyajikan alias kolom lama (`identifier`, `name`, `publisher`,
> `tray_image_file`, `avoid_cache`, `image_file`, `emojis`, dll.) untuk kompatibilitas ke belakang.
> Emoji per stiker dinormalisasi menjadi token emoji pertama (fallback `😊` bila kosong)
> demi kompatibilitas parser emoji WhatsApp.

### Status "sudah terpasang"

`WhatsAppApi.isPackAdded()` mengecek provider whitelist WhatsApp
(`content://com.whatsapp.provider.sticker_whitelist_check/is_whitelisted`) dan membaca kolom
`result`. Catatan: pada WhatsApp modern (2.26+), paket stiker pihak ketiga buatan sendiri umumnya
tidak tercatat di provider ini, sehingga status ini bersifat opsional/best-effort.

## Catatan Khas Perangkat Uji

- Perangkat uji (vivo, emulator lokal) memiliki layanan Google Play yang tidak stabil
  (mencatat `Long live credential not available`, dan Play Store sempat OOM sendiri).
  Gangguan ini tidak berkaitan dengan aplikasi; gunakan perangkat yang sehat
  (WhatsApp aktif) untuk validasi akhir ekspor paket.
- Error `Failed to find provider info for com.whatsapp.w4b.provider.sticker_whitelist_check`
  di logcat sudah dihilangkan — query whitelist sekarang hanya menyentuh provider `com.whatsapp`.