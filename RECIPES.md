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

### 5. Build & Sideload Signed Release APK (Taildrop)
```powershell
Set-Location 'D:\_ARH-AGENT-OS\_AGENT-WORKSPACE\projects\ARH-Terminal'
.\gradlew :app:assembleRelease
# Output binary: app/build/outputs/apk/release/app-release.apk

# Sideload directly to Android phone via Taildrop
tailscale file cp app/build/outputs/apk/release/app-release.apk arh-f7:
```

### 6. Remote CI Build & Auto-Download via Cloud Agent
```bash
# Trigger remote GitHub Actions build, stream logs, download APKs, and verify signatures:
python scripts/remote_apk_builder.py --type release --verify --output-dir ./build-outputs

# Or quickly download and verify the latest prebuilt green release APK:
python scripts/remote_apk_builder.py --from-latest --type release --verify --output-dir ./build-outputs
```

### 7. Run Maestro Live UI & Visual Clash Testing (Local & CI)

#### A. Static Conformance Gate
```powershell
# Validates all .maestro/*.yaml flows against testTags in Compose codebase & detects anti-patterns
python scripts/ci_maestro_doctor.py
```

#### B. Local Native Windows Setup & Execution
1. **Prerequisites**: Ensure Java 17+ and ADB are installed:
   ```powershell
   winget install EclipseAdoptium.Temurin.17.JDK
   adb devices
   ```
2. **Install Maestro CLI (Native Windows)**:
   ```powershell
   Invoke-WebRequest -Uri "https://github.com/mobile-dev-inc/maestro/releases/latest/download/maestro.zip" -OutFile "$env:TEMP\maestro.zip"
   Expand-Archive -Path "$env:TEMP\maestro.zip" -DestinationPath "C:\maestro" -Force
   [Environment]::SetEnvironmentVariable("Path", $env:Path + ";C:\maestro\bin", [EnvironmentVariableTarget]::User)
   ```
3. **Build & Install Debug APK to Device**:
   ```powershell
   .\gradlew.bat :app:installDebug
   ```
4. **Run Automated Test Suite**:
   ```powershell
   # Run all flows
   maestro test .maestro/

   # Run individual flow (e.g. 200% accessibility font scale & overflow audit)
   maestro test .maestro/04_font_scale_and_overflow_audit.yaml
   ```
5. **Launch Maestro Studio (Interactive Web Inspector)**:
   ```powershell
   # Launches live UI hierarchy inspector on http://localhost:9999
   maestro studio
   ```

#### C. CI Headless ATD Runner Configuration
* Headless Android Test Development (`aosp_atd` API 34) runs with `-gpu swiftshader_indirect`.
* All flows include `extendedWaitUntil: { timeout: 30000 }` on cold launch to accommodate CPU software rasterization on virtualized runners before assertions fire.

