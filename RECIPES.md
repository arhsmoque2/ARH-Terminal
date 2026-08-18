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
On Windows dev machine:
```powershell
psmux new -s arh-agent
```
Attach from ARH-Terminal:
```bash
psmux -CC attach -t arh-agent
```
