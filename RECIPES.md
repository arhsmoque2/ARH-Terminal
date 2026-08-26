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

### 7A. Run the Tier 1/2 UI Quality Gate (Robolectric + Roborazzi) — Primary, Blocking

This is the **primary** UI quality gate as of ADR-009 — it runs on every push/PR in the `unit-tests-and-doctor`
CI job, in-process, no emulator. Maestro (§7B below) is on-demand only.

```powershell
Set-Location 'D:\_ARH-AGENT-OS\_AGENT-WORKSPACE\projects\ARH-Terminal'

# Tier 1: semantics-tree assertions (AppUiQualityGateTest) — blocking, always a real gate
.\gradlew :app:testDebugUnitTest --tests "com.arh.terminal.ui.AppUiQualityGateTest"

# Tier 2: Roborazzi screenshot capture (AppUiVisualRegressionTest) — currently capture-only,
# see GOTCHAS.md #21 for why. Just running `testDebugUnitTest` captures images to
# app/build/outputs/roborazzi/ without asserting anything.
.\gradlew :app:testDebugUnitTest --tests "com.arh.terminal.ui.AppUiVisualRegressionTest"
```

**To turn Tier 2 into a real regression gate** (not yet done — no baselines are committed):
```powershell
# 1. Record baseline goldens from the current (known-good) UI state
.\gradlew :app:recordRoborazziDebug
# -> commit the resulting PNGs (default app/build/outputs/roborazzi/, or wherever
#    `roborazzi { outputDir.set(...) }` points) into version control.

# 2. From then on, verify against those goldens (fails on unintended visual drift)
.\gradlew :app:testDebugUnitTest -Proborazzi.test.verify=true

# Review a specific diff after a failure:
.\gradlew :app:testDebugUnitTest -Proborazzi.test.compare=true
# -> produces [original]_compare.png and a JSON diff under build/test-results/roborazzi
```
Once step 2 is adopted, add `-Proborazzi.test.verify=true` to the `Run Unit Tests with Timeout Ceiling`
step in `.github/workflows/ci.yml` so CI enforces it too.

**Adding a new Tier 1/2 test for a new screen or overlay component**: follow the pattern in
`AppUiQualityGateTest.kt` / `AppUiVisualRegressionTest.kt` — `createComposeRule()` (not
`createAndroidComposeRule`, no real Activity needed), mock every constructor dependency with
`mockk(relaxed = true)` except the ones under test, `setContent { ARHTerminalTheme { ... } }`.
For a real geometric "these two elements must not overlap" check (not just "both composed"),
compare `composeTestRule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot` rectangles directly
— see `verifyJoypadAndQuickActionBarDoNotVisuallyClash` for a worked example.

### 7B. Run Maestro Live UI & Visual Clash Testing (Local & On-Demand CI Only)

Maestro is **not** run on every push/PR (see ADR-009) — it's scoped to `workflow_dispatch` in
`maestro-ui-gate.yml`, reserved for scenarios that genuinely cross a real app/process boundary
(a browser-based OAuth redirect, the system SAF file picker) where §7A's in-process Robolectric
tests structurally can't reach. Read `GOTCHAS.md` #13–#21 before extending or re-enabling it —
several non-obvious things went wrong getting it working the first time.

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

