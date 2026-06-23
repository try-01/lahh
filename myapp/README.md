# Samsung TV Remote - Android

Aplikasi remote control untuk Samsung Smart TV dengan teknologi WebSocket dan SSDP discovery.

## Fitur Utama

- 🔍 **Auto-scan TV** dengan SSDP discovery (multicast + broadcast)
- 🔌 **Auto-connect** ke TV tersimpan saat aplikasi dibuka
- 🔐 **Auto-fallback** WSS (port 8002) → WS (port 8001)
- 🎨 **Glassmorphism UI** dengan dark theme & dynamic mesh gradient
- 📳 **Haptic feedback** pada setiap tombol
- 🔆 **Screen always-on** mode untuk penggunaan jangka panjang
- 🎛️ **Remote lengkap**: navigasi, volume, channel, numpad, media controls
- 🎬 **App shortcuts**: Netflix, Prime Video, YouTube
- 💾 **Token pairing** disimpan otomatis

## Requirements

- Android 7.0 (API 24) atau lebih tinggi
- Target SDK: Android 14 (API 36)
- Kotlin 2.1.0
- Jetpack Compose
- OkHttp 4.12.0

## Teknologi

- **MVVM Architecture** dengan AndroidViewModel
- **Kotlin Coroutines** & Flow untuk async operations
- **SharedPreferences** untuk persistent storage
- **SSDP (Simple Service Discovery Protocol)** untuk network discovery
- **WebSocket** (WSS/WS) untuk komunikasi real-time dengan TV
- **SSL Trust-All** untuk sertifikat self-signed TV Samsung

## Setup & Build

### 1. Clone atau Extract Project
```bash
cd /root/projects/myapp
```

### 2. Generate Gradle Wrapper
```bash
gradle wrapper --gradle-version 9.3.1
```

### 3. Build APK
```bash
./gradlew assembleDebug
```

APK akan tersedia di: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Install ke Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Testing & Debugging

### Logcat Monitoring
```bash
# Semua log aplikasi
adb logcat | grep -E "SamsungRemote|RemoteViewModel"

# SSDP discovery saja
adb logcat | grep SamsungRemoteDiscovery

# WebSocket connection saja
adb logcat | grep SamsungRemote
```

### Network Requirements
- TV dan HP harus terhubung ke **jaringan WiFi yang sama**
- TV harus dalam keadaan **ON** (tidak standby)
- Port 8001 atau 8002 harus terbuka di TV
- Multicast/Broadcast traffic harus diizinkan oleh router

### Troubleshooting

**TV tidak terdeteksi saat scan:**
- Pastikan TV dalam keadaan ON
- Cek apakah TV dan HP di jaringan WiFi yang sama
- Restart router jika perlu
- Coba manual connect dengan memasukkan IP TV

**Gagal connect setelah scan:**
- TV akan menampilkan dialog pairing saat koneksi pertama
- Terima pairing di layar TV
- Token akan disimpan otomatis untuk koneksi selanjutnya

**Connection dropped:**
- Aplikasi akan otomatis reconnect
- Cek koneksi WiFi HP
- Pastikan TV tidak sleep/standby

## Struktur Project

```
myapp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/example/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── model/TvDevice.kt
│   │   │   │   │   └── repository/SettingsRepository.kt
│   │   │   │   ├── network/
│   │   │   │   │   ├── SamsungTvWebSocket.kt   # WebSocket client
│   │   │   │   │   └── SsdpDiscovery.kt          # SSDP scanner
│   │   │   │   └── ui/
│   │   │   │       ├── components/               # Reusable UI
│   │   │   │       ├── screens/                  # Main screens
│   │   │   │       ├── theme/                    # Colors & theme
│   │   │   │       └── viewmodel/RemoteViewModel.kt
│   │   │   └── res/
│   │   ├── androidTest/                          # Instrumented tests
│   │   └── test/                                 # Unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml                        # Version catalog
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Key Classes

### SsdpDiscovery.kt
- Dual-mode scan: Multicast (239.255.255.250) + Broadcast (255.255.255.255)
- Auto-detect active network interface
- Retry mechanism: 2 attempts dengan delay 500ms
- Timeout: 5 detik per scan
- Parse LOCATION, USN, friendlyName dari SSDP response

### SamsungTvWebSocket.kt
- Auto-fallback: WSS (8002) → WS (8001)
- SSL trust-all untuk self-signed certificate
- Ping interval: 30 detik
- Max reconnect: 3 attempts
- Handle events: ms.channel.connect, ms.channel.ready, ms.error

### RemoteViewModel.kt
- Auto-connect ke saved TV di init
- Lifecycle-aware disconnect
- SSDP discovery dengan Flow collection
- Haptic feedback management
- Screen wake lock management

## Samsung TV Key Codes

Aplikasi mendukung semua key code standar Samsung:
- Navigation: `KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`, `KEY_ENTER`
- System: `KEY_POWER`, `KEY_SOURCE`, `KEY_HOME`, `KEY_EXIT`, `KEY_RETURN`
- Volume: `KEY_VOLUP`, `KEY_VOLDOWN`, `KEY_MUTE`
- Channel: `KEY_CHUP`, `KEY_CHDOWN`, `KEY_CH_LIST`, `KEY_PRECH`
- Numbers: `KEY_0` to `KEY_9`, `KEY_DASH`
- Media: `KEY_PLAY`, `KEY_PAUSE`, `KEY_STOP`, `KEY_FF`, `KEY_REWIND`
- Menu: `KEY_MENU`, `KEY_GUIDE`, `KEY_INFO`, `KEY_SETTINGS`
- Apps: `KEY_NETFLIX`, `KEY_AMAZON`, `KEY_YOUTUBE`
- Colors: `KEY_RED`, `KEY_GREEN`, `KEY_YELLOW`, `KEY_BLUE`

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## License

MIT License - Free to use and modify.

## Credits

Developed with Kotlin, Jetpack Compose, and OkHttp.
Samsung WebSocket Remote API implementation based on Samsung Tizen specifications.

---

**Build**: v1.0.0 (26062201)  
**Target**: Samsung Smart TV (2016+) with Tizen OS  
**Platform**: Android 7.0+
