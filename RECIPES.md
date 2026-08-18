# ARH-Terminal Recipes & Runbooks 📖

### 1. Build & Assemble Debug APK
```powershell
Set-Location 'D:\_ARH-AGENT-OS\_AGENT-WORKSPACE\projects\ARH-Terminal'
.\gradlew :app:assembleDebug
# Output binary: app/build/outputs/apk/debug/app-debug.apk
```

### 2. Run Full Quality Gate (Detekt + Unit Tests)
```powershell
Set-Location 'D:\_ARH-AGENT-OS\_AGENT-WORKSPACE\projects\ARH-Terminal'
.\gradlew detekt :core:core-relay:test :core:core-mcp:test :app:test :core:core-agents:test
```

### 3. Start psmux Session on Host Dev Box
```powershell
# Start background psmux session with named socket
psmux -u -S arh-agent new-session -s arh-agent
```

### 4. Connect to On-Device MCP Server from PC
```python
import urllib.request
import json

url = "http://100.85.170.170:8070/mcp"
token = "ca48ffe8-cb63-45be-bfd5-1911e367fbcd"

req = urllib.request.Request(
    url,
    data=json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": "tools/call",
        "params": {"name": "android_get_screen_state", "arguments": {}}
    }).encode(),
    headers={
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream"
    }
)
resp = urllib.request.urlopen(req)
print(json.loads(resp.read()))
```
