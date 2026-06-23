#!/bin/bash
# Test script untuk Samsung TV Remote

set -e

echo "🧪 Running tests for Samsung TV Remote..."

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "⚠️  Gradle wrapper not found. Generating..."
    gradle wrapper --gradle-version 9.3.1
fi

chmod +x ./gradlew

# Run unit tests
echo "📝 Running unit tests..."
./gradlew test

# Run instrumented tests (if device connected)
if adb devices | grep -q "device$"; then
    echo "📱 Running instrumented tests..."
    ./gradlew connectedAndroidTest
else
    echo "⚠️  No device connected. Skipping instrumented tests."
fi

echo "✅ All tests completed!"
