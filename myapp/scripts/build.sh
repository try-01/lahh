#!/bin/bash
# Build script untuk Samsung TV Remote

set -e

echo "🔨 Building Samsung TV Remote..."

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "⚠️  Gradle wrapper not found. Generating..."
    gradle wrapper --gradle-version 9.3.1
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean build
echo "🧹 Cleaning previous build..."
./gradlew clean

# Build debug APK
echo "📦 Building debug APK..."
./gradlew assembleDebug

# Check if build successful
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Build successful!"
    echo "📱 APK location: app/build/outputs/apk/debug/app-debug.apk"
    
    # Get APK size
    SIZE=$(du -h app/build/outputs/apk/debug/app-debug.apk | cut -f1)
    echo "📊 APK size: $SIZE"
    
    # Ask if user wants to install
    read -p "🤔 Install to connected device? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "📲 Installing..."
        adb install -r app/build/outputs/apk/debug/app-debug.apk
        echo "✅ Installed successfully!"
    fi
else
    echo "❌ Build failed!"
    exit 1
fi
