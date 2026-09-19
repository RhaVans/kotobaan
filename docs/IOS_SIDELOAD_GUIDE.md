# Panduan Pemasangan Kotoba iOS (.ipa) / iOS Sideloading Guide

Panduan ini menjelaskan cara mengunduh dan memasang aplikasi **KOTOBAAN (Kotoba iOS)** ke perangkat iPhone atau iPad tanpa memerlukan komputer Mac atau akun Apple Developer berbayar.

---

## Ringkasan Alur Kerja

```text
GitHub Actions (macOS Cloud)
       │
       ▼
Unduh "Kotoba.ipa" dari Tab Actions
       │
       ▼
Pasang ke iPhone via Sideloadly / AltStore (Windows / Mac)
       │
       ▼
Aktifkan "Developer Mode" & Verifikasi Profil di Pengaturan iPhone
       │
       ▼
KOTOBAAN Siap Digunakan Secara Offline
```

---

## Langkah 1: Mengunduh File `Kotoba.ipa`

> [!TIP]
> **Mengaktifkan Alur Kerja GitHub Actions:**
> File konfigurasi CI telah disiapkan di [`docs/ci/build-ios.yml`](ci/build-ios.yml).
> Untuk mengaktifkannya di GitHub Anda:
> 1. Buka [https://github.com/RhaVans/kotobaan](https://github.com/RhaVans/kotobaan) di browser.
> 2. Klik **Add file → Create new file**.
> 3. Beri nama path file: `.github/workflows/build-ios.yml`.
> 4. Salin seluruh isi dari [`docs/ci/build-ios.yml`](ci/build-ios.yml) lalu simpan (**Commit changes**).
> 5. GitHub Actions akan otomatis menjalankan build di cloud macOS Apple Silicon!

Setelah alur kerja aktif, setiap kali ada pembaruan kode pada branch `main`, GitHub Actions secara otomatis mengompilasi file instalasi iOS (`Kotoba.ipa`):

1. Buka repositori GitHub KOTOBAAN di browser:
   [https://github.com/RhaVans/kotobaan](https://github.com/RhaVans/kotobaan)
2. Klik tab **Actions** di bagian atas halaman repositori.
3. Klik proses kerja terbaru yang bernama **Build iOS App (.ipa)**.
4. Gulir ke bagian paling bawah ke bagian **Artifacts**.
5. Klik **Kotoba-iOS-IPA** untuk mengunduh arsip zip.
6. Ekstrak file zip tersebut di komputer Anda hingga mendapatkan file:
   `Kotoba.ipa`

---

## Langkah 2: Memasang ke iPhone Menggunakan Sideloadly (Metode Utama)

Sideloadly adalah utilitas gratis yang tersedia untuk **Windows** dan **macOS** yang memungkinkan pemasangan file `.ipa` menggunakan akun Apple ID standar (gratis).

### Persiapan:
1. Unduh dan pasang **Sideloadly** dari situs resminya: [https://sideloadly.io](https://sideloadly.io)
2. Pastikan **iTunes** dan **iCloud** (versi non-Microsoft Store jika di Windows) terpasang agar driver komunikasi perangkat Apple berfungsi.

### Proses Instalasi:
1. Hubungkan iPhone atau iPad Anda ke komputer menggunakan kabel USB (pilih *"Trust this computer"* pada layar iPhone jika muncul perintah konfirmasi).
2. Buka aplikasi **Sideloadly** di komputer.
3. Pastikan perangkat Anda terdeteksi pada kolom **iDevice**.
4. Tarik dan lepas (drag-and-drop) file `Kotoba.ipa` ke kotak berikon aplikasi di Sideloadly.
5. Masukkan alamat email **Apple ID** Anda pada kolom *Apple ID*.
6. Klik tombol **Start**.
7. Jika diminta, masukkan kata sandi Apple ID dan kode autentikasi dua faktor (2FA) yang muncul di layar iPhone Anda.
8. Tunggu proses signing dan instalasi hingga status menunjukkan `Done!`.

---

## Langkah 3: Mengaktifkan Izin Menjalankan Aplikasi di iOS

Apple menerapkan kebijakan keamanan ketat pada iOS 16, 17, dan 18 untuk aplikasi yang dipasang di luar App Store. Ikuti dua langkah verifikasi berikut:

### 1. Mengaktifkan Developer Mode (Wajib untuk iOS 16 ke atas)
1. Di iPhone Anda, buka **Pengaturan (Settings)**.
2. Buka menu **Privasi & Keamanan (Privacy & Security)**.
3. Gulir ke bagian paling bawah dan pilih **Mode Pengembang (Developer Mode)**.
4. Geser tombol ke posisi **Aktif (ON)**.
5. iPhone akan meminta untuk melakukan restart. Ketuk **Mulai Ulang (Restart)**.
6. Setelah menyala kembali, buka kunci iPhone dan ketuk **Aktifkan (Turn On)** pada dialog konfirmasi, lalu masukkan kode sandi (passcode) perangkat.

### 2. Mempercayai Sertifikat Pengembang (Trust Developer Profile)
1. Buka kembali **Pengaturan (Settings)**.
2. Masuk ke menu **Umum (General) → Manajemen VPN & Perangkat (VPN & Device Management)**.
3. Di bawah bagian *App Pengembang (Developer App)*, ketuk akun Apple ID Anda.
4. Ketuk **Percayai "[Apple ID Anda]" (Trust)**.
5. Konfirmasi dengan memilih **Percayai (Trust)**.

Aplikasi **KOTOBAAN** kini siap dibuka langsung dari layar utama iPhone Anda.

---

## Metode Alternatif: Menggunakan AltStore atau SideStore

Jika Anda telah menggunakan ekosistem AltStore:

1. Pastikan **AltServer** berjalan di komputer Anda pada jaringan Wi-Fi yang sama.
2. Kirim file `Kotoba.ipa` ke iPhone Anda (via AirDrop, iCloud Drive, atau email).
3. Buka file `Kotoba.ipa` menggunakan aplikasi **AltStore**.
4. AltStore akan menandatangani dan memasang aplikasi secara lokal.

Bagi pengguna **SideStore**, setelah instalasi awal, aplikasi dapat di-refresh langsung dari iPhone tanpa perlu terhubung ke komputer, selama terhubung ke VPN WireGuard lokal SideStore.

---

## Catatan Masa Berlaku Sertifikat & Data Pembelajaran

1. **Masa Berlaku 7 Hari (Akun Apple Gratis):**
   Aplikasi yang ditandatangani menggunakan akun Apple ID gratis memiliki masa aktif sertifikat selama **7 hari**.
   - Untuk memperpanjang masa aktif, cukup hubungkan kembali perangkat ke komputer dan klik **Start** di Sideloadly (atau ketuk refresh di AltStore/SideStore).
2. **Keamanan Data Kemajuan (User Progress):**
   Seluruh riwayat belajar, repetisi SRS, dan status penguasaan kosakata tersimpan dalam direktori lokal perangkat:
   `Application Support/kotoba_v2.db`
   Melakukan instalasi ulang atau refresh sertifikat **tidak akan menghapus** data kemajuan belajar Anda, selama Bundle Identifier tetap sama (`com.kotoba.app`).
