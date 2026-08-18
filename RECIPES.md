# Recipes & Runbooks

## 🔨 Local Quality Gates

### Run Detekt & Compose Rules
```bash
./gradlew detekt
```

### Auto-Format Code (Spotless + ktlint)
```bash
./gradlew spotlessApply
```

### Run Unit Tests
```bash
./gradlew test
```

### Assemble Debug APK
```bash
./gradlew :app:assembleDebug
```

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
