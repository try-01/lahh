# Build Instructions - Samsung TV Remote

## Prerequisites

### Required Software
- **Java JDK 11** atau lebih tinggi
- **Android SDK** dengan API Level 36
- **Gradle 9.3.1** (akan di-generate otomatis)
- **Git** (optional, untuk version control)

### Required Hardware
- **Android Device** dengan Android 7.0+ untuk testing
- **Samsung Smart TV** (2016+) dengan Tizen OS
- **WiFi Network** yang sama untuk HP dan TV

---

## Step-by-Step Build Process

### 1. Setup Environment

**Install Java JDK 11:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-11-jdk

# macOS (Homebrew)
brew install openjdk@11

# Windows
# Download dari https://adoptium.net/
```

**Verify Java Installation:**
```bash
java -version
# Output: openjdk version "11.0.x"
```

**Install Android SDK:**
```bash
# Option 1: Install Android Studio (Recommended)
# Download dari https://developer.android.com/studio

# Option 2: Command Line Tools only
# Download dari https://developer.android.com/studio#command-line-tools-only
```

**Set Environment Variables:**
```bash
# Linux/macOS - tambahkan ke ~/.bashrc atau ~/.zshrc
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows - System Environment Variables
ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
```

---

### 2. Generate Gradle Wrapper

```bash
cd /root/projects/myapp

# Generate wrapper dengan Gradle 9.3.1
gradle wrapper --gradle-version 9.3.1

# Verify wrapper generated
ls -la gradlew
ls -la gradle/wrapper/
```

**Expected Output:**
```
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

---

### 3. Build Debug APK

**Option A: Using Gradle Wrapper (Recommended)**
```bash
# Make gradlew executable (Linux/macOS)
chmod +x ./gradlew

# Build debug APK
./gradlew assembleDebug
```

**Option B: Using Build Script**
```bash
# Make script executable
chmod +x ./scripts/build.sh

# Run build script
./scripts/build.sh
```

**Build Output:**
```
BUILD SUCCESSFUL in 2m 15s
45 actionable tasks: 45 executed

APK location: app/build/outputs/apk/debug/app-debug.apk
```

---

### 4. Verify Build

```bash
# Check if APK exists
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Expected output:
# -rw-r--r-- 1 user user 8.5M Jun 23 15:30 app-debug.apk

# Get APK info
aapt dump badging app/build/outputs/apk/debug/app-debug.apk | grep package

# Expected output:
# package: name='com.example' versionCode='1' versionName='1.0'
```

---

### 5. Install to Device

**Connect Android Device:**
```bash
# Enable USB Debugging on device:
# Settings → About Phone → Tap "Build Number" 7x
# Settings → Developer Options → Enable "USB Debugging"

# Connect via USB cable

# Verify device connected
adb devices

# Expected output:
# List of devices attached
# ABC123XYZ    device
```

**Install APK:**
```bash
# Option A: Direct install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Option B: Using install script
./scripts/install.sh
```

**Install Output:**
```
Performing Streamed Install
Success
```

---

### 6. Launch and Test

**Launch App:**
```bash
# Launch app
adb shell am start -n com.example/.MainActivity

# Monitor logs
./scripts/logcat.sh
```

**Test Checklist:**
- [ ] App launches without crash
- [ ] UI loads properly
- [ ] Tap "Settings" button works
- [ ] Tap "Pindai TV lain" starts SSDP scan
- [ ] TV terdeteksi di list (jika TV ON dan di network sama)
- [ ] Connect ke TV menampilkan pairing dialog di TV
- [ ] Accept pairing di TV
- [ ] Status berubah menjadi "Terhubung"
- [ ] Tap remote buttons (Volume, Channel, etc)
- [ ] TV merespon command dari app

---

## Build Variants

### Debug Build (Development)
```bash
./gradlew assembleDebug
```
- Debuggable
- Tidak di-obfuscate
- Include debug symbols
- Faster build time

### Release Build (Production)
```bash
# Generate keystore (first time only)
keytool -genkey -v -keystore release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias

# Build release APK
./gradlew assembleRelease

# Sign APK (if not auto-signed)
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore release-key.jks app/build/outputs/apk/release/app-release-unsigned.apk my-key-alias

# Align APK
zipalign -v 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release.apk
```

**Release Build Features:**
- ProGuard obfuscation enabled
- Code optimization
- Debug symbols stripped
- Smaller APK size

---

## Troubleshooting Build Issues

### Issue: "Gradle wrapper not found"
**Solution:**
```bash
gradle wrapper --gradle-version 9.3.1
chmod +x ./gradlew
```

### Issue: "SDK location not found"
**Solution:**
```bash
# Create local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Or set ANDROID_HOME environment variable
export ANDROID_HOME=/path/to/android/sdk
```

### Issue: "Java version mismatch"
**Solution:**
```bash
# Check Java version
java -version

# Should be Java 11 or higher
# If not, install Java 11 and set JAVA_HOME
export JAVA_HOME=/path/to/jdk-11
```

### Issue: "Build failed with OutOfMemoryError"
**Solution:**
```bash
# Increase Gradle memory in gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### Issue: "Dependency download failed"
**Solution:**
```bash
# Clean and retry
./gradlew clean
./gradlew assembleDebug --refresh-dependencies
```

### Issue: "Device not authorized"
**Solution:**
```bash
# Revoke and re-authorize
adb kill-server
adb start-server
adb devices
# Accept authorization dialog on device
```

---

## CI/CD Build (GitHub Actions)

Project sudah include `.github/workflows/build.yml` untuk automatic builds.

**Trigger Build:**
```bash
# Push to main/master branch
git push origin main

# Create pull request
git checkout -b feature/new-feature
git push origin feature/new-feature
# Create PR on GitHub
```

**GitHub Actions Workflow:**
1. Checkout code
2. Setup JDK 17
3. Setup Gradle 9.3.1
4. Build debug APK
5. Upload APK as artifact

**Download Artifact:**
- Go to GitHub Actions tab
- Select workflow run
- Download `app-debug-apk` artifact

---

## Build Optimization Tips

### Speed Up Build
```bash
# Enable Gradle daemon
echo "org.gradle.daemon=true" >> gradle.properties

# Enable parallel builds
echo "org.gradle.parallel=true" >> gradle.properties

# Enable build cache
echo "org.gradle.caching=true" >> gradle.properties

# Use configuration cache
./gradlew assembleDebug --configuration-cache
```

### Reduce APK Size
```bash
# Enable ProGuard/R8
buildTypes {
    release {
        minifyEnabled true
        shrinkResources true
    }
}

# Use APK splits
splits {
    abi {
        enable true
        include "armeabi-v7a", "arm64-v8a"
    }
}
```

---

## Build Scripts Summary

| Script | Purpose | Usage |
|--------|---------|-------|
| `build.sh` | Build debug APK | `./scripts/build.sh` |
| `clean.sh` | Clean project | `./scripts/clean.sh` |
| `install.sh` | Install to device | `./scripts/install.sh` |
| `test.sh` | Run tests | `./scripts/test.sh` |
| `logcat.sh` | Monitor logs | `./scripts/logcat.sh` |

---

## Post-Build Steps

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate test report
./gradlew testDebugUnitTest
# Report: app/build/reports/tests/testDebugUnitTest/index.html
```

### Code Quality
```bash
# Lint check
./gradlew lint

# Lint report
# app/build/reports/lint-results.html
```

### APK Analysis
```bash
# Android Studio: Build → Analyze APK
# Select: app/build/outputs/apk/debug/app-debug.apk

# Command line
aapt dump badging app/build/outputs/apk/debug/app-debug.apk
aapt list -v app/build/outputs/apk/debug/app-debug.apk
```

---

## Distribution

### Local Testing
```bash
# Share APK via email/messaging
cp app/build/outputs/apk/debug/app-debug.apk ~/Downloads/SamsungTVRemote-v1.0.0-debug.apk
```

### Google Play Store (Future)
1. Build release APK dengan signing
2. Create app listing di Google Play Console
3. Upload APK/AAB
4. Fill store listing info
5. Submit for review

### GitHub Releases
```bash
# Create release tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Upload APK to GitHub Releases
gh release create v1.0.0 app/build/outputs/apk/debug/app-debug.apk
```

---

## Quick Reference Commands

```bash
# Full clean build
./scripts/clean.sh && ./scripts/build.sh

# Build + Install + Monitor
./scripts/build.sh && ./scripts/install.sh && ./scripts/logcat.sh

# Test everything
./scripts/test.sh

# Generate release APK
./gradlew assembleRelease

# Check build configuration
./gradlew dependencies
./gradlew properties
```

---

**Build Time**: ~2-3 menit (first build), ~30 detik (incremental)  
**APK Size**: ~8-10 MB (debug), ~5-7 MB (release with ProGuard)  
**Min API**: 24 (Android 7.0)  
**Target API**: 36 (Android 14)

**Status**: ✅ Ready to build!
