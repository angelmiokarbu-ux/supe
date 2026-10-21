# Supe 🎙️⚡

> **"Supe"** — Diambil dari bahasa Jawa yang berarti *"lupa"*. Aplikasi asisten pintar yang memastikan Anda tidak pernah lagi lupa akan tagihan, jadwal, atau catatan penting.

Aplikasi Android modern berbasis **Jetpack Compose (Material 3)** dengan arsitektur reaktif, input multimodal, deteksi tagihan otomatis dari status bar, widget home screen, dan Quick Settings Tile.

---

## ✨ Fitur Utama

1. **Input Multimodal Lengkap**
   - **Teks**: Input cepat pengingat, catatan, dan perkiraan jatuh tempo.
   - **Suara**: Perekam suara interaktif dengan visualizer gelombang audio real-time dan konversi ucapan ke teks (*Speech-to-Text*).
   - **Kamera**: Pengambilan foto nota, struk, surat tagihan langsung menggunakan kamera perangkat.
   - **Galeri**: Pilih bukti transfer atau invoice digital dari galeri foto.

2. **Akses Cepat Status Bar (Quick Settings Tile)**
   - Tombol **Supe Suara** di panel setelan cepat (sejajar dengan WiFi, Senter, Bluetooth).
   - 1-tap langsung membuka jendela rekam suara tanpa perlu mencari dan membuka aplikasi secara manual. Kompatibel penuh dengan Android 14 (`PendingIntent` & `TileService`).

3. **Home Screen App Widget**
   - Widget ringkas untuk layar utama.
   - Tampilan status pengingat terdekat, tombol pintas rekam suara (**Bicara**), dan tombol pintas catat teks (**Catat**).

4. **Pemantau Notifikasi Tagihan (`NotificationListenerService`)**
   - Secara pintar mendeteksi dan membaca notifikasi masuk dari aplikasi perbankan (BCA, Mandiri, BRI, BNI), e-wallet (GoPay, OVO, DANA, ShopeePay), dan penyedia utilitas (PLN Mobile, BPJS, PDAM, Indihome, dll.).
   - Ekstraksi otomatis nominal tagihan dalam Rupiah dan tanggal jatuh tempo.
   - Otomatis mencatat pengingat sebelum jatuh tempo tiba.

5. **Tanya Supe (Smart Assistant Q&A)**
   - Asisten dialog interaktif offline-first.
   - Tanyakan informasi seputar tagihan dan catatan:
     - *"Berapa total tagihan saya?"*
     - *"Ada jadwal apa hari ini?"*
     - *"Pengingat untuk besok"*
     - *"Cari tagihan listrik"*

6. **Desain Modern Jetpack Compose**
   - Transisi layar halus (*Directional Slide & Fade*).
   - Skeleton Screen ber-shimmer (bebas dari spinner layar penuh kuno).
   - Mode gelap & terang elegan dengan aksen Crimson Ruby.

---

## 🛠️ Arsitektur & Teknologi

- **Bahasa**: Kotlin 2.2
- **Framework UI**: Jetpack Compose Material 3
- **Database**: SQLite Database Helper terpusat (Thread-safe & Transactional)
- **Image Loading**: Coil 3 Compose
- **Notifikasi & Alarm**: `NotificationCompat` + `AlarmManager` (`RTC_WAKEUP`)
- **Quick Settings**: Android `TileService`
- **Widget**: Android `AppWidgetProvider`
- **CI/CD**: GitHub Actions Automated Build & Signed APK Release
- **Target OS**: Android 8.0 (API 26) hingga Android 15/16 (API 35/36)
