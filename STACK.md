# ARH-Terminal Technology Stack 🛠️

## 1. Core Platform & Runtime
* **Target Operating System**: Android 8.0+ (API 26+) / Target Android 16 (API 36)
* **Language & Runtime**: Kotlin 2.3.10 / JDK 17 (Eclipse Temurin)
* **Build System**: Gradle 9.4.0 with Android Gradle Plugin (AGP) 9.1.0
* **Architecture Pattern**: Unidirectional Data Flow (UDF) / MVVM with StateFlow & Coroutines

---

## 2. Submodule Matrix

| Module | Core Dependencies | Primary Role |
|---|---|---|
| `:app` | Jetpack Compose (BOM 2026.03.00), Material 3, Hilt 2.59.2, Coroutines 1.10.2 | Main UI layer, navigation, state orchestration, intent routing |
| `:core:core-ssh` | `sshj 0.39.0`, `bouncycastle bcprov-jdk18on 1.80.2`, `slf4j-nop` | Raw SSH transport, PEM key authentication, host key verification |
| `:core:core-tmux` | Kotlin Coroutines Flow, standard JVM streams | `psmux -CC` control mode protocol parser, pane demuxer, event stream |
| `:core:core-agents` | `org.json`, Kotlin Coroutines | Stream tokenizers & event parsers for Claude Code, OpenAI Codex, OpenCode |
| `:core:core-mcp` | Java Standard Library ServerSocket, JSON-RPC 2.0 parser | Embedded on-device MCP server exposing 56 Android native tools |
| `:core:core-relay` | `okhttp 4.12.0`, `javax.crypto.Cipher` (AES-256-GCM) | E2EE WebSocket client for remote NAT/CGNAT cloud traversal |

---

## 3. Quality & Testing Harness
* **Static Analysis**: Detekt `1.23.8` with Compose Rules `0.5.7`
* **Unit Testing**: JUnit 4, MockK `1.14.11`, Kotlinx Coroutines Test
* **Code Formatting**: Spotless `7.0.2`
* **CI/CD Pipeline**: GitHub Actions Multi-Tier Automated Runner (Lint $\rightarrow$ Unit Tests $\rightarrow$ APK Build)
