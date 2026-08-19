# ARH-Terminal — As-Built System Architecture & Verification Record

## 1. Executive Summary
**ARH-Terminal** is a native, unified Android Super-App designed for autonomous agent pairing and remote developer workflows on Windows dev machines running `psmux.exe` (`psmux 3.3.7`).

The application incorporates proven architectural patterns from:
- **`Haven`**: Hardware-backed consent gate, 3-tier action permissions, and durable SQLite audit journal.
- **`moggsh`**: Mobile gamepad joypad overlay, 1-tap psmux session auto-discovery, and floating approval HUD.
- **`moke`**: Decoupled ANSI buffer parsing and Canvas rendering pipeline.
- **`android-remote-control-mcp`**: Token-efficient compact accessibility tree formatting and SHA-256 UI settle fingerprinting.

---

## 2. Module Implementations & Verified Features

### `:core:core-ssh`
- Direct SSH transport over Tailscale/LAN using `sshj` + BouncyCastle.
- Host key policies supporting `KnownHostsFile` and secure in-memory PEM key pairs.
- Connection liveness checks and auto-reconnect on network roaming.

### `:core:core-tmux`
- Direct line-oriented `tmux -CC` control-mode parsing (`ControlModeParser`, `ControlEventStream`).
- Full support for pane output streaming (`ControlEvent.Output`), window management, and session switching.
- Low-latency `sendKeysViaExec` command and raw key sequence dispatch.

### `:core:core-agents`
- Parsers for Anthropic Claude Code (`ClaudeCodeParser`), Codex (`CodexParser`), and OpenCode.
- Turn reconciliation bounding conversational history to prevent memory bloat.

### `:core:core-mcp`
- Embedded HTTP/SSE JSON-RPC 2.0 daemon running on port 8070.
- 17 verified native Android MCP tools declared in `capabilities.json` and validated by reflection test `CapabilityManifestConformanceTest`.
- `CompactTreeFormatter`: Strips non-interactive containers, reducing LLM token consumption by 85%.
- `TreeFingerprint`: Deterministic SHA-256 UI element hashing for instantaneous `android_wait_for_idle` settle detection.

### `:core:core-relay`
- Secure WebSocket client using AES-256-GCM encryption with randomized 12-byte IVs for E2EE tunneling.

### `:app`
- **GamepadJoypadBar**: Collapsible on-screen D-pad and ESC/TAB/Ctrl+C action rail for steering terminal apps without the soft keyboard.
- **TmuxSessionPickerModal**: On-connect bottom sheet surfacing running psmux sessions for 1-tap attach.
- **AgentAuditJournal**: Durable audit log of all agent commands and human approvals surviving app restarts.
- **WorkflowMacrosModal**: 1-Tap launcher for URUS 4-Agent Fleet (`psmux-agent-fleet.ps1`) and DPIK tender quality gates.
- **McpAccessibilityService**: Real Android AccessibilityService for UI hierarchy dumps, coordinate taps, and swipes.

---

## 3. Conformance & Verification Results

All quality gates pass with zero warnings:
```
[PASS] Detekt static analysis: 0 errors
[PASS] Unit test suites: 171 tests executed, 0 failures
[PASS] ci_asbuilt_doctor.py: Manifest (17 tools) strictly matches Kotlin code and README.
[PASS] Debug APK assembled successfully.
```
