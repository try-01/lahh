# Troubleshooting Guide

## TV tidak terdeteksi saat scan

### Kemungkinan Penyebab

1. **TV dalam keadaan standby/sleep**
   - ✅ Pastikan TV dalam keadaan ON (layar menyala)
   - TV tidak merespon SSDP query saat standby

2. **TV dan HP tidak di jaringan WiFi yang sama**
   - ✅ Cek koneksi WiFi di HP: Settings → WiFi
   - ✅ Cek koneksi TV: Menu → Network → Network Status
   - Pastikan keduanya terhubung ke SSID WiFi yang sama

3. **Router memblock multicast traffic**
   - Beberapa router menonaktifkan multicast/broadcast
   - ✅ Coba gunakan **Manual Connect** dengan IP TV
   - ✅ Cari IP TV di: Menu → Network → Network Status

4. **Firewall di router atau TV**
   - Port 1900 (SSDP) mungkin diblock
   - ✅ Restart router
   - ✅ Coba disable firewall sementara untuk testing

5. **Android permission tidak diberikan**
   - ✅ Pastikan semua permission sudah granted
   - Check: Settings → Apps → Samsung TV Remote → Permissions

### Solusi

**Langkah 1: Manual Connect**
```
1. Buka Settings di aplikasi
2. Tap "Sambungkan manual"
3. Masukkan IP TV (contoh: 192.168.1.42)
4. Tap "Sambungkan"
```

**Langkah 2: Restart Network**
```
1. Restart router WiFi
2. Reconnect HP ke WiFi
3. Restart TV (unplug dan plug kembali)
4. Coba scan lagi
```

**Langkah 3: Check Logs**
```bash
adb logcat | grep SamsungRemoteDiscovery
```
Lihat error message untuk detail masalah.

## Gagal connect setelah scan berhasil

### Kemungkinan Penyebab

1. **Pairing belum di-accept di TV**
   - Saat koneksi pertama, TV akan menampilkan dialog
   - ✅ **Terima pairing di layar TV** (tombol muncul di TV)

2. **Port 8001/8002 diblock**
   - ✅ Coba fallback otomatis (aplikasi akan coba kedua port)
   - Tunggu ~10 detik untuk auto-fallback

3. **SSL certificate issue**
   - Aplikasi sudah handle trust-all SSL
   - Jika masih gagal, cek Android version compatibility

### Solusi

**Accept Pairing di TV:**
```
1. Koneksi pertama akan trigger dialog di TV
2. Dialog muncul dengan pesan "Allow SamsungRemote?"
3. Pilih "Allow" atau "Yes"
4. Token akan tersimpan otomatis
```

**Force Reconnect:**
```
1. Buka Settings
2. Tap "Lupakan TV ini"
3. Tap "Pindai TV lain" atau "Sambungkan manual"
4. Pairing ulang dengan TV
```

## Connection dropped saat digunakan

### Kemungkinan Penyebab

1. **WiFi tidak stabil**
   - ✅ Pindah lebih dekat ke router
   - ✅ Cek kualitas sinyal WiFi di HP

2. **TV masuk sleep mode**
   - ✅ Disable sleep timer di TV
   - Settings → General → System Manager → Time → Sleep Timer → Off

3. **HP masuk power saving mode**
   - ✅ Disable battery optimization untuk app
   - Settings → Battery → App Battery Usage → Samsung TV Remote → Unrestricted

4. **WebSocket timeout**
   - Aplikasi otomatis reconnect setelah timeout
   - ✅ Tunggu beberapa detik untuk auto-reconnect

### Solusi

**Disable Battery Optimization:**
```
Android Settings → Battery → Battery Optimization
→ All apps → Samsung TV Remote → Don't optimize
```

**Enable Screen Always-On:**
```
Samsung TV Remote → Settings → Toggle "Tetap terang di tangan"
```

**Manual Reconnect:**
```
Samsung TV Remote → Settings → "Hubungkan ulang TV"
```

## Tombol tidak berfungsi

### Kemungkinan Penyebab

1. **Tidak terhubung ke TV**
   - ✅ Cek status koneksi di header (harus "Terhubung")
   - Indikator hijau = terhubung, merah = terputus

2. **Key code tidak supported di TV model tertentu**
   - Beberapa key hanya tersedia di model TV tertentu
   - ✅ Coba tombol lain untuk verifikasi

3. **TV busy processing command lain**
   - ✅ Tunggu beberapa detik
   - Jangan spam tombol terlalu cepat

### Solusi

**Verify Connection:**
```
1. Lihat header app (bagian atas)
2. Status harus "Terhubung" dengan dot hijau
3. Jika "Terputus", tap Settings → "Hubungkan ulang TV"
```

**Test Basic Keys:**
```
1. Coba tombol Volume Up/Down
2. Coba tombol navigasi (Arrow Up/Down)
3. Jika basic keys work, key lain mungkin unsupported
```

## Haptic feedback tidak bekerja

### Solusi

1. **Enable di Settings:**
   ```
   Samsung TV Remote → Settings → Toggle "Getar saat tombol ditekan"
   ```

2. **Check System Vibration:**
   ```
   Android Settings → Sound & Vibration → Enable Vibrate
   ```

3. **Permission Issue:**
   ```
   Reinstall app atau grant VIBRATE permission manually
   ```

## App crash atau force close

### Data Collection

```bash
# Capture crash log
adb logcat > crash.log

# Filter for errors
adb logcat *:E | grep com.example

# Clear and monitor fresh
adb logcat -c && adb logcat
```

### Common Fixes

1. **Clear App Data:**
   ```
   Settings → Apps → Samsung TV Remote → Storage → Clear Data
   ```

2. **Reinstall App:**
   ```
   adb uninstall com.example
   adb install -r app-debug.apk
   ```

3. **Check Android Version:**
   - Minimum: Android 7.0 (API 24)
   - Recommended: Android 10+

## Network Issues

### Check Network Interface

```bash
# From Android terminal
ip addr show wlan0

# Check multicast support
cat /proc/net/dev_mcast

# Ping TV from HP
ping 192.168.1.42
```

### Router Configuration

**Enable Multicast:**
- Router settings → Advanced → Multicast → Enable IGMP Snooping

**Disable AP Isolation:**
- Router settings → Wireless → AP Isolation → Disable
- AP Isolation mencegah device saling komunikasi

**Port Forwarding (Optional):**
- Biasanya tidak perlu karena same network
- Hanya jika TV di subnet berbeda

## Debug Mode

### Enable Verbose Logging

Edit `SamsungTvWebSocket.kt` dan `SsdpDiscovery.kt`:
```kotlin
// Change Log.d to Log.v for verbose
Log.v("SamsungRemote", "Verbose message")
```

### Logcat Filters

```bash
# All app logs
adb logcat | grep -E "SamsungRemote|RemoteViewModel"

# Only errors
adb logcat *:E | grep SamsungRemote

# SSDP discovery only
adb logcat | grep SamsungRemoteDiscovery

# WebSocket only
adb logcat | grep "SamsungRemote:"

# With timestamp
adb logcat -v time | grep SamsungRemote
```

## Known Limitations

1. **MAC Address Detection**
   - Currently placeholder (AA:BB:CC:DD:EE:FF)
   - Real MAC detection requires ARP lookup (future update)

2. **Signal Strength**
   - Currently placeholder ("Bagus")
   - Real measurement requires WiFi API access (future update)

3. **Multiple TVs**
   - App hanya menyimpan 1 TV
   - Multi-TV support planned untuk v2.0

4. **Older TV Models**
   - TV sebelum 2016 mungkin tidak support WebSocket API
   - Coba gunakan Samsung SmartThings app sebagai alternatif

## Getting Help

### Check Logs First
```bash
./scripts/logcat.sh
```

### Information to Provide

Saat melaporkan bug, sertakan:
1. **Device info**: Model HP, Android version
2. **TV info**: Model TV, tahun produksi
3. **Network info**: Router model, WiFi frequency (2.4GHz/5GHz)
4. **Logs**: Output dari `adb logcat`
5. **Steps to reproduce**: Langkah-langkah memicu bug
6. **Expected vs Actual**: Apa yang diharapkan vs yang terjadi

### Report Issue

GitHub Issues: (URL repository jika ada)

### Community Support

- Discord/Telegram: (jika ada)
- Email: (jika ada)

## Quick Reference

| Problem | Quick Fix |
|---------|-----------|
| TV tidak terdeteksi | Manual connect dengan IP TV |
| Gagal pairing | Accept dialog di layar TV |
| Connection drop | Enable screen always-on |
| Tombol tidak work | Verify status "Terhubung" |
| App crash | Clear app data & reinstall |
| Haptic tidak work | Enable di Settings app |

## Advanced Troubleshooting

### Test WebSocket Manually

```bash
# Install websocat
cargo install websocat

# Connect to TV
websocat -k wss://192.168.1.42:8002/api/v2/channels/samsung.remote.control?name=VGVzdEFwcA==

# Send test command
{"method":"ms.remote.control","params":{"Cmd":"Click","DataOfCmd":"KEY_MUTE","Option":"false","TypeOfRemote":"SendRemoteKey"}}
```

### SSDP Manual Test

```bash
# Send SSDP query
echo -ne "M-SEARCH * HTTP/1.1\r\nHOST: 239.255.255.250:1900\r\nMAN: \"ssdp:discover\"\r\nMX: 3\r\nST: urn:samsung.com:device:RemoteControlReceiver:1\r\n\r\n" | nc -u 239.255.255.250 1900

# Listen for responses
nc -u -l 1900
```

### Packet Capture

```bash
# Capture network traffic
adb shell tcpdump -i wlan0 -w /sdcard/capture.pcap

# Download and analyze with Wireshark
adb pull /sdcard/capture.pcap
wireshark capture.pcap
```

---

**Last Updated**: 2026-06-23
