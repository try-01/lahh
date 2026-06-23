# Samsung TV WebSocket API Documentation

## Overview

Samsung Smart TV menggunakan WebSocket API untuk remote control. Protokol ini tersedia di TV Samsung 2016+ dengan Tizen OS.

## Connection

### Endpoints

**Secure WebSocket (Preferred)**
```
wss://{TV_IP}:8002/api/v2/channels/samsung.remote.control?name={APP_NAME_BASE64}&token={TOKEN}
```

**Insecure WebSocket (Fallback)**
```
ws://{TV_IP}:8001/api/v2/channels/samsung.remote.control?name={APP_NAME_BASE64}&token={TOKEN}
```

### Parameters

- `{TV_IP}`: IP address TV di network lokal (contoh: `192.168.1.42`)
- `{APP_NAME_BASE64}`: Nama aplikasi di-encode Base64 (contoh: `U2Ftc3VuZ1JlbW90ZQ==` untuk "SamsungRemote")
- `{TOKEN}`: Pairing token (opsional untuk koneksi pertama, required setelah pairing)

### SSL Certificate

TV Samsung menggunakan self-signed certificate. Client harus disable SSL verification atau trust all certificates.

## Message Format

### Sending Remote Key

```json
{
  "method": "ms.remote.control",
  "params": {
    "Cmd": "Click",
    "DataOfCmd": "KEY_POWER",
    "Option": "false",
    "TypeOfRemote": "SendRemoteKey"
  }
}
```

### Events from TV

#### Channel Connect Event
```json
{
  "event": "ms.channel.connect",
  "data": {
    "token": "12345678",
    "name": "SamsungRemote"
  }
}
```

#### Channel Ready Event
```json
{
  "event": "ms.channel.ready"
}
```

#### Error Event
```json
{
  "event": "ms.error",
  "data": {
    "message": "Unauthorized"
  }
}
```

## Key Codes

### Navigation
- `KEY_UP`, `KEY_DOWN`, `KEY_LEFT`, `KEY_RIGHT`
- `KEY_ENTER`, `KEY_RETURN`

### System
- `KEY_POWER`, `KEY_SOURCE`, `KEY_HOME`
- `KEY_MENU`, `KEY_EXIT`, `KEY_SLEEP`

### Volume & Channel
- `KEY_VOLUP`, `KEY_VOLDOWN`, `KEY_MUTE`
- `KEY_CHUP`, `KEY_CHDOWN`, `KEY_CH_LIST`, `KEY_PRECH`

### Numbers
- `KEY_0` to `KEY_9`, `KEY_DASH`

### Media
- `KEY_PLAY`, `KEY_PAUSE`, `KEY_STOP`
- `KEY_REWIND`, `KEY_FF`

### Apps
- `KEY_NETFLIX`, `KEY_AMAZON`, `KEY_YOUTUBE`

### Color Keys
- `KEY_RED`, `KEY_GREEN`, `KEY_YELLOW`, `KEY_BLUE`

### Other
- `KEY_GUIDE`, `KEY_INFO`, `KEY_SETTINGS`
- `KEY_PICTURE_SIZE`, `KEY_SUBTITLE`

## Connection Flow

1. Client connects to wss://TV_IP:8002/...
2. TV sends: `{"event": "ms.channel.connect", "data": {"token": "..."}}`
3. Client saves token
4. TV sends: `{"event": "ms.channel.ready"}`
5. Client can now send remote keys

## Pairing Flow (First Connection)

**First Time (No Token):**
1. Client connects without token parameter
2. TV displays pairing dialog on screen
3. User accepts pairing on TV
4. TV sends token in ms.channel.connect event
5. Client saves token for future use

**Subsequent Connections:**
1. Client connects with saved token parameter
2. No pairing dialog shown
3. Connection established immediately

## Keep-Alive

- Client harus mengirim WebSocket ping setiap 30 detik
- Jika tidak ada ping/pong, koneksi akan timeout setelah ~60 detik

## Error Handling

### Connection Failures

**Port 8002 (WSS) tidak available:**
- Fallback ke port 8001 (WS)

**SSL Handshake Failed:**
- Pastikan SSL verification disabled

**Connection Timeout:**
- TV mungkin dalam standby mode
- TV harus dalam keadaan ON

**Unauthorized:**
- Token expired atau invalid
- Hapus token dan lakukan pairing ulang

### Best Practices

1. Auto-Reconnect: Retry mechanism (max 3x)
2. Graceful Degradation: Fallback WSS → WS
3. Token Persistence: Simpan di SharedPreferences
4. Connection Timeout: 10 detik
5. Keep-Alive: Ping interval 30 detik

## Discovery (SSDP)

```
M-SEARCH * HTTP/1.1
HOST: 239.255.255.250:1900
MAN: "ssdp:discover"
MX: 3
ST: urn:samsung.com:device:RemoteControlReceiver:1
```

Multicast ke `239.255.255.250:1900` untuk menemukan TV di network.

## Testing

```bash
# Monitor logs
adb logcat | grep SamsungRemote

# Test with websocat
websocat -k wss://192.168.1.42:8002/api/v2/channels/samsung.remote.control?name=VGVzdEFwcA==
```

## References

- Samsung Tizen TV Web API
- Samsung SmartThings API
- Community reverse-engineered protocol

## Notes

- API tidak official dan dapat berubah
- Tested pada Samsung TV 2016-2024 dengan Tizen 3.0+
- Port 8001/8002 harus terbuka di firewall TV
