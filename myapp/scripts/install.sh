#!/bin/bash
# Install script untuk Samsung TV Remote

set -e

echo "📲 Installing Samsung TV Remote..."

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Check if APK exists
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK not found at $APK_PATH"
    echo "💡 Run './scripts/build.sh' first to build the APK"
    exit 1
fi

# Check if device connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    echo "💡 Connect your device via USB and enable USB debugging"
    exit 1
fi

# Install APK
echo "📦 Installing APK..."
adb install -r "$APK_PATH"

echo "✅ Installation complete!"
echo "🚀 Launching app..."
adb shell am start -n com.example/.MainActivity

echo "📊 View logs with: adb logcat | grep -E 'SamsungRemote|RemoteViewModel'"
