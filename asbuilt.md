# As-Built Architecture & Handover Record 📋

**Project**: ARH-Terminal  
**Repository**: `https://github.com/arhsmoque2/ARH-Terminal`  
**Current Release / Branch**: `main`  
**Platform**: Android 8.0+ (API 26+) / Kotlin 2.3.10 / AGP 9.1.0 / Compose BOM 2026.03.00  

---

## 1. System Overview
`ARH-Terminal` is an Android agent cockpit and on-device tool provider. It connects to Windows host machines running `psmux.exe` (`psmux 3.3.7`) and exposes 56 native Android tools to PC agents.

---

## 2. Component Directory & Status

```
ARH-Terminal/
├── app/                                  # Main Android Application
│   ├── src/main/java/com/arh/terminal/
│   │   ├── di/TerminalModule.kt          # Hilt DI provider for Tmux, MCP, & Relay
│   │   ├── data/profiles/                # Saved DevBox Connection Profiles Repository
│   │   ├── util/NetworkMonitor.kt        # Network Roaming & Auto-Reconnect Engine
│   │   ├── ui/
│   │   │   ├── conversation/             # Polymorphic Agent Turn Cards & Diff Views
│   │   │   ├── components/               # QuickActionBar, FloatingApprovalHud, MacrosModal
│   │   │   └── session/                  # SessionScreen, SessionViewModel, SessionUiState
│   │   └── MainActivity.kt               # Single-top Activity with ACTION_SEND handler
├── core/
│   ├── core-ssh/                         # sshj transport & BouncyCastle crypto
│   ├── core-tmux/                        # psmux -CC Control Mode protocol engine
│   ├── core-agents/                      # Parsers for Claude Code, Codex, OpenCode (118 tests)
│   ├── core-mcp/                         # Embedded HTTP/SSE JSON-RPC 2.0 MCP daemon (56 tools)
│   └── core-relay/                       # AES-256-GCM E2EE WebSocket client for cloud NAT
├── config/detekt/detekt.yml              # Strict Detekt static analysis rules
├── .github/workflows/ci.yml              # Multi-tier GitHub Actions CI/CD workflow
├── README.md                             # System overview and component diagram
├── STACK.md                              # Technology stack matrix
├── ADR.md                                # Architectural Decision Records
├── RECIPES.md                            # Copy-pasteable runbooks and test commands
└── GOTCHAS.md                            # Standardized failure capsules
```

---

## 3. Verified Capability Matrix

| Capability | Module / Layer | Verification Method | Status |
|---|---|---|---|
| **psmux -CC Stream** | `:core:core-tmux` | `testDebugUnitTest` | 🟢 Verified |
| **Agent Parser Suite** | `:core:core-agents` | 118 Unit Tests (`ClaudeCodeParserTest`, `CodexParserTest`) | 🟢 Verified |
| **On-Device MCP Server** | `:core:core-mcp` | Live tailscale query (`android_get_screen_state` 200 OK) + `McpServerEngineTest` | 🟢 Verified |
| **E2EE Relay Client** | `:core:core-relay` | `RelayCipherTest` AES-256-GCM roundtrip | 🟢 Verified |
| **Network Auto-Reconnect** | `:app` (`NetworkMonitor`) | Flow emitter verification | 🟢 Verified |
| **Share Target & Macros** | `:app` (`MainActivity`) | Intent dispatch verification | 🟢 Verified |
| **APK Assembler** | `:app` | `./gradlew :app:assembleDebug` produces valid APK | 🟢 Verified |

---

## 4. Handover & Next Steps
* The codebase is clean, modular, and passes all Detekt static analysis checks with 0 errors.
* Future enhancements can expand the DPIK macros catalog or add biometric authentication (Fingerprint / Face Unlock) to the saved profile credentials.
