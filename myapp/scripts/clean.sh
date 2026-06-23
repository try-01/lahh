#!/bin/bash
# Clean script untuk Samsung TV Remote

set -e

echo "🧹 Cleaning Samsung TV Remote project..."

# Check if gradlew exists
if [ ! -f "./gradlew" ]; then
    echo "⚠️  Gradle wrapper not found"
    exit 1
fi

chmod +x ./gradlew

# Clean build
echo "🗑️  Removing build artifacts..."
./gradlew clean

# Remove additional files
echo "🗑️  Removing additional build files..."
rm -rf .gradle
rm -rf app/build
rm -rf build
rm -rf .idea/caches
rm -rf .idea/libraries

# Remove local.properties if exists
if [ -f "local.properties" ]; then
    echo "🗑️  Removing local.properties..."
    rm -f local.properties
fi

echo "✅ Project cleaned successfully!"
echo "💡 Run './scripts/build.sh' to rebuild"
