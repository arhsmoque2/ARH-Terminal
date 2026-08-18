# Recipes & Runbooks

## 🔨 Local Quality Gates & DevTooling

### 1. Run Detekt & Compose Rules
```bash
./gradlew detekt
```

### 2. Run All Unit Tests
```bash
./gradlew test
```

### 3. Assemble Debug APK
```bash
./gradlew :app:assembleDebug
```
* Built APK is located at: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Auto-Format Code (Spotless + ktlint)
```bash
./gradlew spotlessApply
```

### 5. Compose Compiler Stability Metrics
Generate composable stability and skippability reports:
```bash
./gradlew :app:assembleDebug -PcomposeCompilerReports=true -PcomposeCompilerMetrics=true
```
* Output metrics: `app/build/compose_metrics/`

---

## 🔗 Server Host Connection (psmux)

### 1. Launch a persistent psmux session on Windows
```powershell
psmux new -s arh-agent
```

### 2. Start Claude Code or Codex inside the session
```powershell
claude
```

### 3. Connect from ARH-Terminal
* Open the app, enter your PC's IP / Hostname, and tap **Connect (psmux -CC)**.
* Switch between **Agent Chat** and **Terminal Feed** via the top tabs.

---

## 🚀 CI/CD Automation (GitHub Actions)
* Workflow configured in `.github/workflows/ci.yml`.
* Automatically runs on every push and pull request:
  1. `Detekt & Compose Rules` (Enforces 0 static errors and Compose stability).
  2. `Unit Test Suites` (Executes full parser and ViewModel tests).
  3. `Assemble Debug APK` (Produces and attaches `arh-terminal-debug-apk` artifact).
