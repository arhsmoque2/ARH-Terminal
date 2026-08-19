# Gaps to Revisit

Findings from a code-level architecture review (2026-08-19), prioritized against the actual use case: personal daily driver replacing ConnectBot, talking to `psmux`/tmux on a Windows PC, over Tailscale, own devices only.

## ✅ Landed Improvements (2026-08-19)

### 1. Foreground Service keepalive (`TerminalSessionService`) — [FIXED]
- **Implementation**: [`TerminalSessionService.kt`](file:///D:/_ARH-AGENT-OS/_AGENT-WORKSPACE/projects/ARH-Terminal/app/src/main/java/com/arh/terminal/service/TerminalSessionService.kt) registered as `FOREGROUND_SERVICE_DATA_SYNC` with ongoing low-priority notification and disconnect action.
- **Wiring**: Started on successful SSH connection and stopped on disconnect in [`SessionViewModel.kt`](file:///D:/_ARH-AGENT-OS/_AGENT-WORKSPACE/projects/ARH-Terminal/app/src/main/java/com/arh/terminal/ui/session/SessionViewModel.kt). Keeps connection alive across screen-off, app switching, and Android Doze.

### 2. Android Keystore Hardware-Backed Key Encryption — [FIXED]
- **Implementation**: [`CredentialCrypto.kt`](file:///D:/_ARH-AGENT-OS/_AGENT-WORKSPACE/projects/ARH-Terminal/app/src/main/java/com/arh/terminal/data/security/CredentialCrypto.kt) uses `AndroidKeyStore` AES-256-GCM (`AES/GCM/NoPadding`) with 12-byte random IV + 128-bit authentication tag.
- **Persistence**: Integrated into [`ConnectionProfile.kt`](file:///D:/_ARH-AGENT-OS/_AGENT-WORKSPACE/projects/ARH-Terminal/app/src/main/java/com/arh/terminal/data/profiles/ConnectionProfile.kt) (keys saved with `ks:` prefix, with automatic backwards-compatibility for legacy `enc:` Base64 entries). `android:allowBackup="false"` enforced in `AndroidManifest.xml`.

### 3. SSH Host Key TOFU & Pinning — [FIXED]
- **Implementation**: [`TofuHostKeyVerifier.kt`](file:///D:/_ARH-AGENT-OS/_AGENT-WORKSPACE/projects/ARH-Terminal/app/src/main/java/com/arh/terminal/data/security/TofuHostKeyVerifier.kt) & `KnownHostsStore.kt` implement Trust-On-First-Use SHA-256 host key fingerprint pinning, rejecting mismatches and recording audit events.
- **Wiring**: Added `KnownHostsPolicy.Custom` to `:core:core-ssh` and wired in `SessionViewModel.kt` to replace `AcceptAll`.

### 4. Hardened MCP Daemon — [FIXED]
- **Implementation**: [`McpServerEngine.kt`](file:///D:/_ARH-AGENT-OS/_AGENT-WORKSPACE/projects/ARH-Terminal/core/core-mcp/src/main/java/com/arh/terminal/core/mcp/server/McpServerEngine.kt) binds to loopback (`127.0.0.1`) by default and uses constant-time token comparison (`MessageDigest.isEqual`).

---

## P2 — remaining backlog / remote-control polish

### 5. Two stub MCP tools
- `android_get_device_logs` returns a hardcoded string, not real logcat output.
- `android_notification_list` always returns an empty array — no `NotificationListenerService` is implemented.
- Irrelevant to the terminal/ConnectBot use case; matters only if using remote agent device control.

### 6. Signed Release APK
- Build a signed release APK when ready for permanent daily driver use.

