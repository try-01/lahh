# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-06-23

### Added
- Initial release Samsung TV Remote
- Auto-scan TV dengan SSDP discovery (dual-mode: multicast + broadcast)
- Auto-connect ke saved TV saat app startup
- WebSocket connection dengan auto-fallback WSS → WS
- SSL trust-all untuk Samsung self-signed certificate
- Glassmorphism UI dengan dark theme
- Dynamic mesh gradient background (dapat dinonaktifkan)
- Haptic feedback untuk semua tombol
- Screen always-on mode
- Remote control lengkap:
  - D-pad navigation dengan tombol OK di tengah
  - Power, Source, Sleep buttons
  - Volume dan Channel controls
  - Numeric keypad (0-9, dash, delete)
  - Media playback controls (play, pause, stop, FF, rewind)
  - Menu & Info buttons (MENU, GUIDE, INFO, SETTINGS, P.SIZE, CC/VD)
  - Color keys (Red, Green, Yellow, Blue)
  - App shortcuts (Netflix, Prime Video, YouTube)
- Settings screen dengan fitur:
  - Reconnect TV
  - Scan for other TVs
  - Manual IP connection
  - Forget TV (hapus token & data)
  - Toggle haptics
  - Toggle screen always-on
  - Toggle dynamic background
  - Remote size selector (compact, fit, large)
  - About & feedback
  - Exit app
- TV info card menampilkan:
  - TV name & model
  - Connection status (terhubung/terputus)
  - IP address
  - WebSocket port (8001/8002)
  - MAC address
  - WiFi signal strength
- Token pairing otomatis tersimpan di SharedPreferences
- Custom toast notifications
- Screen transition animations

### Technical Details
- MVVM architecture dengan AndroidViewModel
- Kotlin Coroutines & StateFlow untuk reactive state management
- Jetpack Compose UI dengan Material 3
- OkHttp WebSocket client dengan ping/pong keepalive
- SSDP discovery dengan timeout 5s dan retry 2x
- Auto-detect network interface untuk multicast
- Regex parsing untuk SSDP response (LOCATION, USN, friendlyName)
- Lifecycle-aware connection management
- Comprehensive logging untuk debugging

### Network Protocol
- Port 8002 (WSS) sebagai default, fallback ke port 8001 (WS)
- App name "SamsungRemote" di-encode Base64
- Message format: JSON dengan method "ms.remote.control"
- Event handling: ms.channel.connect, ms.channel.ready, ms.error
- Ping interval 30 detik untuk keep-alive
- Max reconnect attempts: 3x

### Supported TV Models
- Samsung Smart TV 2016+ dengan Tizen OS
- Mendukung semua TV dengan Samsung WebSocket Remote API
- Tested pada: Tizen 3.0+

### Known Limitations
- TV harus dalam keadaan ON untuk SSDP discovery
- TV dan HP harus di jaringan WiFi yang sama
- Beberapa router memblock multicast traffic (gunakan manual connect)
- MAC address detection belum diimplementasi (placeholder)
- Signal strength detection belum diimplementasi (placeholder)

### Dependencies
- Kotlin 2.1.0
- Compose BOM 2024.12.01
- OkHttp 4.12.0
- Lifecycle 2.8.7
- Material Icons Extended 1.7.6
- Robolectric 4.14 (testing)
- Roborazzi 1.32.1 (screenshot testing)

### Build Configuration
- Gradle 9.3.1
- Android Gradle Plugin 8.8.0
- Target SDK 36 (Android 14)
- Min SDK 24 (Android 7.0)
- Java 11 compatibility
- Kotlin JVM target 11

---

## Future Roadmap

### Planned for v1.1.0
- [ ] Real MAC address detection via ARP lookup
- [ ] Real WiFi signal strength measurement
- [ ] Voice control integration
- [ ] Keyboard input support
- [ ] Mouse/touchpad mode
- [ ] Multi-TV support (switch between saved TVs)
- [ ] Custom button mapping
- [ ] Shortcuts/macros (multi-key sequences)
- [ ] Dark/Light theme toggle
- [ ] Custom accent colors

### Planned for v1.2.0
- [ ] TV screen mirroring preview
- [ ] App launcher grid
- [ ] Recent apps quick access
- [ ] Notification dari TV
- [ ] Volume/Brightness slider
- [ ] Picture mode quick toggle
- [ ] Sound mode quick toggle

### Planned for v2.0.0
- [ ] Multi-device support (control multiple TVs)
- [ ] Room/location grouping
- [ ] Schedule power on/off
- [ ] Automation rules
- [ ] Home Assistant integration
- [ ] Google Home / Alexa integration
- [ ] Widget support
- [ ] Wear OS companion app

---

**Note**: This project is in active development. Feature requests and bug reports are welcome via GitHub Issues.
