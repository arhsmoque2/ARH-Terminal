# ARH-Terminal Recipes & Runbooks 📖

### 1. Build & Assemble Debug APK
```powershell
Set-Location 'D:\_ARH-AGENT-OS\_AGENT-WORKSPACE\projects\ARH-Terminal'
.\gradlew :app:assembleDebug
# Output binary: app/build/outputs/apk/debug/app-debug.apk
```

### 2. Run Full Quality Gate (Detekt + 6-Module Unit Tests + As-Built Conformance)
```powershell
Set-Location 'D:\_ARH-AGENT-OS\_AGENT-WORKSPACE\projects\ARH-Terminal'
# Execute Detekt & all unit tests (569 tests) across 6 modules
.\gradlew :core:core-ssh:test :core:core-tmux:test :core:core-agents:test :core:core-mcp:test :core:core-relay:test :app:test detekt --no-daemon

# Execute As-Built vs Spec Manifest Conformance Doctor
python scripts/ci_asbuilt_doctor.py
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
token = "<DYNAMIC_GENERATED_SESSION_BEARER_TOKEN>"

# Call screen state inspection (READ_ONLY tier - auto executes)
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

# Call mutative action (MUTATIVE tier - triggers Floating Consent HUD on phone)
req_tap = urllib.request.Request(
    url,
    data=json.dumps({
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/call",
        "params": {"name": "android_tap", "arguments": {"x": 500, "y": 1000}}
    }).encode(),
    headers={
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
)
# Will suspend until operator taps [Approve (Y)] or [Reject (N)] on device HUD
resp_tap = urllib.request.urlopen(req_tap)
print(json.loads(resp_tap.read()))
```
