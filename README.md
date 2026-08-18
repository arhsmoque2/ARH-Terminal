# ARH-Terminal 📱⚡

**ARH-Terminal** is a native, unified Android Super-App designed for autonomous agent pairing and remote developer workflows on Windows dev machines running `psmux.exe` (`psmux 3.3.7`).

It seamlessly blends:
1. **Terminal & Agent Cockpit**: Real-time streaming of `psmux -CC` sessions over SSH with polymorphic Agent Conversation Cards (Claude Code, OpenAI Codex, OpenCodeReader) and one-tap command approval HUDs.
2. **On-Device Agent MCP Server (`:core:core-mcp`)**: Embedded HTTP/SSE JSON-RPC 2.0 MCP daemon exposing 56 native Android accessibility, sensor, camera, clipboard, and file tools directly to PC agents over Tailscale/LAN.
3. **End-to-End Encrypted Relay (`:core:core-relay`)**: AES-256-GCM WebSocket client enabling secure remote access through NAT/CGNAT firewalls without port-forwarding.

---

## 🏛️ System Architecture

```
                                      ┌─────────────────────────────────────────┐
                                      │            WINDOWS DEV HOST             │
                                      │  • psmux.exe (tmux -CC protocol)        │
                                      │  • PC AI Agents (Claude/Antigravity)    │
                                      └──────────────────┬──────────────────────┘
                                                         │
                                    ┌────────────────────┴────────────────────┐
                                    │ Direct SSH (LAN/Tailscale) OR E2EE Relay│
                                    └────────────────────┬────────────────────┘
                                                         │
┌────────────────────────────────────────────────────────▼────────────────────────────────────────────────────────┐
│                                             ARH-TERMINAL (ANDROID)                                              │
├──────────────────────────────┬──────────────────────────────┬─────────────────────────────┬─────────────────────┤
│        :core:core-ssh        │       :core:core-tmux        │      :core:core-agents      │    :core:core-mcp   │
│  • sshj + BouncyCastle       │  • ControlModeParser         │  • ClaudeCodeParser         │  • HTTP/SSE Server  │
│  • Key Auth / KnownHosts     │  • ControlEventStream        │  • CodexParser              │  • JSON-RPC 2.0     │
│  • Network Auto-Reconnect    │  • Pane Demuxing             │  • OpenCodeReader           │  • 56 Native Tools  │
├──────────────────────────────┴──────────────────────────────┴─────────────────────────────┴─────────────────────┤
│                                        :core:core-relay (E2EE Tunnel)                                           │
│  • AES-256-GCM Encryption / Decryption with Secure Random IVs                                                   │
│  • OkHttp WebSocket Client with Heartbeat (Ping/Pong) & Auto-Recovery                                           │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                   :app (UI)                                                     │
│  • Jetpack Compose Material 3 UI with Live Network Status Indicators (WiFi / Cellular / Offline)                │
│  • Polymorphic Agent Turn Cards (Reasoning Drawer, Tool Invocation, Diffs)                                      │
│  • Floating Approval HUD (1-tap [Approve (Y)] / [Reject (N)]) & Quick-Action Extended Key Rail                  │
│  • MCP Agent Bridge Dashboard Tab & Interactive psmux Attach/Detach Toggle                                      │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Modules

| Module | Purpose | Status |
|---|---|---|
| `:app` | Jetpack Compose UI, Hilt DI, ViewModel state orchestrator, Material 3 theme | 🟢 Verified |
| `:core:core-ssh` | SSH transport layer, PEM/passphrase auth, keepalive leases | 🟢 Verified |
| `:core:core-tmux` | `psmux -CC` control mode protocol parser, session manager | 🟢 Verified |
| `:core:core-agents` | Stream parsers for Claude Code, Codex, and OpenCode CLI tools (118 tests) | 🟢 Verified |
| `:core:core-mcp` | On-device Model Context Protocol server exposing 56 Android tools | 🟢 Verified |
| `:core:core-relay` | AES-256-GCM encrypted WebSocket relay client for NAT traversal | 🟢 Verified |

---

## 🛠️ Verification & Quality Gates

* **Kotlin Detekt Static Analysis**: `0 errors` (`./gradlew detekt`)
* **Unit Testing**: `100% passing` (`./gradlew test`)
* **Debug APK Compilation**: `./gradlew :app:assembleDebug`
* **CI/CD Pipeline**: GitHub Actions multi-tier workflow (`.github/workflows/ci.yml`)
