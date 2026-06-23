# Contributing to Samsung TV Remote

Terima kasih atas minat Anda untuk berkontribusi! Berikut panduan untuk membantu Anda memulai.

## Code of Conduct

- Bersikap hormat kepada semua kontributor
- Gunakan bahasa yang sopan dan profesional
- Terima kritik konstruktif dengan terbuka
- Fokus pada apa yang terbaik untuk project

## How to Contribute

### Reporting Bugs

Sebelum membuat bug report:
1. Periksa apakah bug sudah dilaporkan di Issues
2. Pastikan Anda menggunakan versi terbaru
3. Coba reproduksi bug dengan langkah minimal

Bug report yang baik harus mencakup:
- **Deskripsi jelas** tentang masalahnya
- **Langkah reproduksi** yang detail
- **Hasil yang diharapkan** vs **hasil aktual**
- **Logcat output** (gunakan `adb logcat`)
- **Device info**: Model HP, Android version, TV model
- **Screenshot/video** jika membantu

### Suggesting Features

Feature request harus mencakup:
- **Use case** yang jelas
- **Mengapa fitur ini berguna** untuk banyak user
- **Mockup/wireframe** jika ada
- **Alternatif yang sudah dicoba**

### Pull Requests

1. **Fork repository** dan buat branch baru:
   ```bash
   git checkout -b feature/nama-fitur
   ```

2. **Follow coding conventions**:
   - Gunakan Kotlin idiomatic code
   - Follow existing code style (indent 4 spaces)
   - Tambahkan dokumentasi untuk public functions
   - Gunakan meaningful variable names
   - JANGAN tambahkan comment kecuali diperlukan untuk logika kompleks

3. **Test your changes**:
   ```bash
   ./gradlew test
   ./gradlew assembleDebug
   ```

4. **Commit dengan conventional commits**:
   ```bash
   git commit -m "feat: tambah dark mode toggle"
   git commit -m "fix: perbaiki SSDP timeout di Android 12+"
   git commit -m "docs: update README dengan troubleshooting"
   ```

   Prefix yang digunakan:
   - `feat:` - fitur baru
   - `fix:` - bug fix
   - `docs:` - dokumentasi
   - `style:` - formatting, typo
   - `refactor:` - refactoring code
   - `perf:` - performance improvement
   - `test:` - menambah/perbaiki test
   - `chore:` - maintenance task

5. **Push dan buat Pull Request**:
   ```bash
   git push origin feature/nama-fitur
   ```

6. **PR Description** harus mencakup:
   - Apa yang diubah dan mengapa
   - Screenshot/video untuk perubahan UI
   - Testing yang sudah dilakukan
   - Issue reference jika ada (Fixes #123)

### Code Style Guidelines

#### Kotlin
```kotlin
// GOOD
fun connectToTv(device: TvDevice) {
    Log.d(TAG, "Connecting to ${device.name}")
    repository.saveTvDevice(device)
    setupWebSocket(device)
}

// BAD
fun connectToTv(device:TvDevice){
  // Connect to the TV device
  Log.d(TAG,"Connecting to "+device.name);
  repository.saveTvDevice(device);
  setupWebSocket(device);
}
```

#### Compose
```kotlin
// GOOD
@Composable
fun RemoteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = text)
    }
}

// BAD
@Composable
fun RemoteButton(text:String,onClick:()->Unit,modifier:Modifier=Modifier){
  Button(onClick=onClick,modifier=modifier){Text(text=text)}
}
```

#### Architecture
- **ViewModel**: Business logic dan state management
- **Repository**: Data layer (SharedPreferences, network)
- **Network**: WebSocket dan SSDP discovery
- **UI**: Composable functions, screens, components
- **Theme**: Colors, typography, shapes

Jangan memasukkan business logic di Composable atau UI layer.

### Testing

- Unit tests untuk logic di ViewModel dan Repository
- Instrumented tests untuk UI dengan Compose Testing
- Screenshot tests dengan Roborazzi
- Manual testing di real device sebelum PR

### Performance

- Hindari heavy computation di main thread
- Gunakan `Dispatchers.IO` untuk network operations
- Gunakan `remember` dan `derivedStateOf` dengan bijak
- Avoid unnecessary recomposition di Compose

### Security

- JANGAN commit secrets atau API keys
- JANGAN commit debug keystore
- Gunakan ProGuard rules untuk release build
- Validate user input untuk manual IP entry

## Development Setup

1. Install Android Studio Ladybug atau lebih baru
2. Install Android SDK API 36
3. Clone repository:
   ```bash
   git clone https://github.com/yourusername/samsung-tv-remote.git
   cd samsung-tv-remote
   ```
4. Generate Gradle wrapper:
   ```bash
   gradle wrapper --gradle-version 9.3.1
   ```
5. Build project:
   ```bash
   ./gradlew assembleDebug
   ```
6. Connect Android device dan install:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Project Structure

```
app/src/main/java/com/example/
├── MainActivity.kt                    # Entry point
├── data/
│   ├── model/TvDevice.kt             # Data class
│   └── repository/SettingsRepository.kt  # SharedPreferences wrapper
├── network/
│   ├── SamsungTvWebSocket.kt         # WebSocket client
│   └── SsdpDiscovery.kt              # SSDP discovery
└── ui/
    ├── components/                    # Reusable UI components
    ├── screens/                       # Screen-level composables
    ├── theme/                         # Colors, typography, theme
    └── viewmodel/RemoteViewModel.kt   # Main ViewModel
```

## Questions?

- Open a GitHub Discussion untuk pertanyaan umum
- Tag @maintainer di PR untuk review
- Join Discord/Telegram group (jika ada)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
