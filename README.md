# ARH-Terminal

> **Voice-first, psmux/tmux-native, agent-aware Android terminal & coding workspace for ARH-OS.**

---

## 🏛️ Architecture & Highlights

- **`psmux` / `tmux -CC` Control Mode**: Natively attaches to Windows `psmux` or Linux `tmux` without full-screen ANSI scraping. Renders clean, readable turn-by-turn agent conversation cards on Android.
- **One-Tap Agent Approvals (HUD)**: Context-aware prompt detection for CLI permission and `[y/N]` confirmation prompts.
- **Mosh & UDP Roaming**: Resilient cellular-to-WiFi handover with native `mosh-client` JNI integration.
- **E2EE Relay Bridge**: Connects securely to workstation dev sessions behind NAT/firewalls without exposing open SSH ports.
- **Android MCP Provider**: Exposes on-device testing, notifications, and accessibility tools to desktop coding agents.

---

## 🛠️ Tech Stack & DevKit

- **Language**: Kotlin 2.3+ (Java 17 target)
- **UI**: Jetpack Compose + Material 3 (Dark / Light / AMOLED presets)
- **DI**: Hilt (`dagger.hilt.android`)
- **Quality Gates**:
  - `Android Lint` (Google correctness & performance)
  - `Detekt` + `compose-rules` (`io.nlopez.compose.rules:detekt:0.5.7`)
  - `Spotless` (ktlint + Gradle formatting)
  - `Robolectric` + `Turbine` + `MockK` (Unit testing)
