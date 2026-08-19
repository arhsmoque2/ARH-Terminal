# As-Built Specification: ARH-Terminal 📱⚡

**Repository**: `https://github.com/arhsmoque2/ARH-Terminal`  
**Target Platform**: Android 16 (API 36) / Minimum Android 8.0 (API 26)  
**Host Environment**: Windows 11 running `psmux.exe` (`psmux 3.3.7`)

---

## 🏛️ System Topology

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
┌───────────────────────────────────────────────────▼───────────────────────────────────────────────────┐
│                                        ARH-TERMINAL (ANDROID)                                         │
├─────────────────────────┬─────────────────────────┬─────────────────────────┬─────────────────────────┤
│     :core:core-ssh      │     :core:core-tmux     │    :core:core-agents    │      :core:core-mcp     │
│  • sshj + BouncyCastle  │  • ControlModeParser    │  • ClaudeCodeParser     │  • HTTP/SSE JSON-RPC 2.0│
│  • Key Auth/KnownHosts  │  • ControlEventStream   │  • CodexParser          │  • 17 Real Spec Tools   │
│  • Auto-Reconnect       │  • Pane Demuxing        │  • OpenCodeReader       │  • McpAccessibilitySvc  │
├─────────────────────────┴─────────────────────────┴─────────────────────────┴─────────────────────────┤
│                                   :core:core-relay (E2EE Tunnel)                                      │
│  • AES-256-GCM Encryption / Decryption with Secure Random IVs                                         │
│  • OkHttp WebSocket Client with Ping/Pong Heartbeats & Auto-Reconnect                                 │
├───────────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                              :app (UI)                                                │
│  • Jetpack Compose M3 UI, NetworkMonitor (WiFi/Cellular/Offline status bar)                           │
│  • Polymorphic AgentTurnCard (Reasoning Drawer, Tool Invocation, Diffs)                               │
│  • Floating Approval HUD (1-tap [Approve] / [Reject]) & QuickActionBar                                │
│  • McpBridgeDashboard Tab & Persistent DataStore ProfileRepository                                    │
│  • Android ACTION_SEND Share Target & 1-Tap DPIK / ARH Workflow Macros Drawer                         │
└───────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Verified Capability Matrix

| Capability | Module / Layer | Status | Verification Method |
|---|---|---|---|
| **psmux -CC Stream** | `:core:core-tmux` | 🟢 Verified | `ControlModeParser` + `ControlEventStream` |
| **Agent Parser Suite** | `:core:core-agents` | 🟢 Verified | 118 Unit Tests (`ClaudeCodeParserTest`, `CodexParserTest`) |
| **SSH Transport & Leases** | `:core:core-ssh` | 🟢 Verified | 45 Unit Tests (`sshj` + BouncyCastle) |
| **On-Device MCP Server** | `:core:core-mcp` | 🟢 Verified | `CapabilityManifestConformanceTest` (`capabilities.json`) |
| **Real Accessibility Service** | `:app` (`McpAccessibilityService`) | 🟢 Verified | `AndroidManifest.xml` BIND_ACCESSIBILITY_SERVICE |
| **E2EE Relay Client** | `:core:core-relay` | 🟢 Verified | `RelayCipherTest` AES-256-GCM roundtrip |
| **Network Roaming Observer** | `:app` (`NetworkMonitor`) | 🟢 Verified | Flow-based `ConnectivityManager` callback |
| **Profile Storage Persistence**| `:app` (`ProfileRepository`)| 🟢 Verified | `ProfileRepositoryTest` (Process-kill restart verification) |
| **As-Built Conformance Doctor**| `scripts/ci_asbuilt_doctor.py`| 🟢 Verified | Automated CI Gate (`capabilities.json` vs code vs docs) |
| **Share Target & Macros** | `:app` (`MainActivity`) | 🟢 Verified | Android `ACTION_SEND` Intent Filter & Macros Sheet |

---

## 🔒 Security & Conformance Guarantees

1. **Manifest Truth Invariant**: All tools exposed by `AndroidToolRegistry` strictly match the canonical specification in `capabilities.json`.
2. **Zero Hollow Mock Invariant**: `scripts/ci_asbuilt_doctor.py` prevents hardcoded dummy constants in release source sets.
3. **Deterministic CI Budget**: GitHub Actions enforces a 10-minute hard ceiling and per-suite test timeouts to prevent hanging jobs.
4. **DataStore Cold-Kill Resilience**: Connection profiles are persisted to durable storage and survive application kills.
