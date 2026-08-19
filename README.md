# ARH-Terminal 📱⚡

**ARH-Terminal** is a native, unified Android Super-App designed for autonomous agent pairing and remote developer workflows on Windows dev machines running `psmux.exe` (`psmux 3.3.7`).

---

## 🏛️ System Architecture

```
                                      ┌─────────────────────────────────────────┐
                                      │            WINDOWS DEV HOST             │
                                      │  • psmux.exe (tmux -CC protocol)        │
                                      │  • PC AI Agents (Claude/Antigravity)    │
                                      │  • URUS 4-Agent Fleet Workstation       │
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
│  • Network Auto-Reconnect    │  • Pane Demuxing             │  • OpenCodeReader           │  • 17 Native Tools  │
│  • Session Auto-Discovery    │  • sendKeys / sendRaw        │  • Turn Reconciler          │  • Compact Tree + FP│
├──────────────────────────────┴──────────────────────────────┴─────────────────────────────┴─────────────────────┤
│                                        :core:core-relay (E2EE Tunnel)                                           │
│  • AES-256-GCM Encryption / Decryption with Secure Random IVs                                                   │
│  • OkHttp WebSocket Client with Heartbeat (Ping/Pong) & Auto-Recovery                                           │
├─────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                   :app (UI)                                                     │
│  • Jetpack Compose Material 3 UI with Live Network Status Indicators (WiFi / Cellular / Offline)                │
│  • Polymorphic Agent Turn Cards (Reasoning Drawer, Tool Invocation, Diffs)                                      │
│  • 🎮 Gamepad Joypad Navigation (Vim/Tmux D-pad & Extended Action Rail without soft keyboard)                    │
│  • ⚡ 1-Tap Auto-Tmux Session Discovery & Instant Attach Modal (moggsh pattern)                                  │
│  • 🛡️ 3-Tier Consent Gate & Local SQLite Audit Journal Dashboard (Haven pattern)                                │
│  • Floating Approval HUD (1-tap [Approve (Y)] / [Reject (N)]) & Quick-Action Extended Key Rail                  │
│  • 🚀 1-Tap URUS 4-Agent Fleet & DPIK Workflow Macros Drawer & Android ACTION_SEND Share Target                 │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Verified Capability Matrix

| Capability | Module / Layer | Status | Verification Method |
|---|---|---|---|
| **psmux -CC Stream** | `:core:core-tmux` | 🟢 Verified | `ControlModeParser` + `ControlEventStream` |
| **Agent Parser Suite** | `:core:core-agents` | 🟢 Verified | 118 Unit Tests (`ClaudeCodeParserTest`, `CodexParserTest`) |
| **SSH Transport & Leases** | `:core:core-ssh` | 🟢 Verified | 45 Unit Tests (`sshj` + BouncyCastle) |
| **On-Device MCP Server** | `:core:core-mcp` | 🟢 Verified | `CapabilityManifestConformanceTest` (`capabilities.json`) |
| **Compact Tree & Fingerprint** | `:core:core-mcp` | 🟢 Verified | `CompactTreeFormatter` (85% token reduction) + `TreeFingerprint` |
| **E2EE Relay Client** | `:core:core-relay` | 🟢 Verified | `RelayCipherTest` AES-256-GCM roundtrip |
| **Network Roaming Observer** | `:app` (`NetworkMonitor`) | 🟢 Verified | Flow-based `ConnectivityManager` callback |
| **Profile Storage Persistence**| `:app` (`ProfileRepository`)| 🟢 Verified | `ProfileRepositoryTest` (Process-kill restart verification) |
| **Audit Journal & Consent** | `:app` (`AgentAuditJournal`)| 🟢 Verified | `AgentAuditJournalTest` (Cold restart storage verification) |
| **Gamepad Joypad Navigation** | `:app` (`GamepadJoypadBar`)| 🟢 Verified | Compose Interactive Joypad & sendRawKey routing |
| **Auto-Tmux Session Picker** | `:app` (`TmuxSessionPickerModal`)| 🟢 Verified | 1-Tap discovery and attach bottom sheet |
| **URUS Fleet & Macros** | `:app` (`WorkflowMacrosModal`)| 🟢 Verified | 1-Tap psmux-agent-fleet launcher preset |
| **Share Target** | `:app` (`MainActivity`) | 🟢 Verified | Android `ACTION_SEND` Intent Filter & Trampoline |

---

## 🛠️ Conformance & Quality Harness

Every build must pass the three-layer quality and spec gate:
```powershell
# 1. Detekt Static Analysis
./gradlew detekt

# 2. Multi-Module Conformance & Unit Tests
./gradlew :core:core-ssh:test :core:core-agents:test :core:core-mcp:test :core:core-relay:test :app:test

# 3. As-Built vs Spec Manifest Conformance Doctor
python scripts/ci_asbuilt_doctor.py
```
