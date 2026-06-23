#!/bin/bash
# Logcat monitoring script untuk Samsung TV Remote

echo "📡 Monitoring Samsung TV Remote logs..."
echo "Press Ctrl+C to stop"
echo ""

# Clear logcat buffer
adb logcat -c

# Start monitoring
adb logcat | grep --color=auto -E "SamsungRemote|RemoteViewModel|SamsungRemoteDiscovery"
