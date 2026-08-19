# ARH-Terminal 📱⚡

**ARH-Terminal** is a native, unified Android Super-App designed for autonomous agent pairing and remote developer workflows on Windows dev machines running `psmux.exe` (`psmux 3.3.7`).

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
│  • Network Auto-Reconnect    │  • Pane Demuxing             │  • OpenCodeReader           │  • 17 Native Tools  │
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
│  • 1-Tap DPIK & ARH Workflow Macros Drawer & Android ACTION_SEND Share Target                                  │
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
| **E2EE Relay Client** | `:core:core-relay` | 🟢 Verified | `RelayCipherTest` AES-256-GCM roundtrip |
| **Network Roaming Observer** | `:app` (`NetworkMonitor`) | 🟢 Verified | Flow-based `ConnectivityManager` callback |
| **Profile Storage Persistence**| `:app` (`ProfileRepository`)| 🟢 Verified | `ProfileRepositoryTest` (Process-kill restart verification) |
| **Share Target & Macros** | `:app` (`MainActivity`) | 🟢 Verified | Android `ACTION_SEND` Intent Filter & Macros Sheet |
