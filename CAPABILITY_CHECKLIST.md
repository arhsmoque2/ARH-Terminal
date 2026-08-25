# End-to-End Capability Checklist 📋

Walks a single real session — "pick up phone, talk to the agent fleet on the Windows
box, get code back, copy it" — scenario by scenario, and marks what's actually
verified today vs. still a gap. Cross-referenced against [ADR.md](ADR.md).
Written after PR #7 (copyable code blocks + selectable terminal feed) landed.

Legend: ✅ Done & shipped · 🟡 Partially covered · ⬜ Gap (planned, not built)

## 1. Connect & attach

| # | Scenario | Status | Notes |
|---|---|---|---|
| 1.1 | Add a connection profile (host, user, PEM key/passphrase) | ✅ | `ConnectionProfile.kt`, hardware-backed AES-256-GCM storage (`CredentialCrypto.kt`) |
| 1.2 | Connect over LAN / Tailscale via direct SSH | ✅ | `:core:core-ssh` (`sshj` + BouncyCastle), TOFU host-key pinning (`TofuHostKeyVerifier.kt`) |
| 1.3 | Connect from outside LAN via E2EE relay (NAT/CGNAT) | ✅ | `:core:core-relay`, AES-256-GCM over OkHttp WebSocket — ADR-004 |
| 1.4 | Discover and 1-tap attach to an existing `psmux`/tmux session | ✅ | `TmuxSessionPickerModal` — ADR-001 |
| 1.5 | Create a new tmux session from the app | ✅ | "New session" flow in `SessionScreen.kt` |
| 1.6 | Connection survives screen-off / backgrounding | ✅ | `TerminalSessionService` foreground service keepalive |
| 1.7 | Network roams WiFi ↔ cellular without dropping | ✅ | `NetworkMonitor` (`ConnectivityManager` flow) |
| 1.8 | Reject/flag a changed or unknown host key | ✅ | TOFU pinning rejects mismatches, logs to audit journal |

## 2. Agent Chat tab (structured turns)

| # | Scenario | Status | Notes |
|---|---|---|---|
| 2.1 | See agent replies as distinct cards, not raw terminal noise | ✅ | `ClaudeCodeParser`/`CodexParser`/`OpenCodeReader` → `AgentTurn` — ADR-002 |
| 2.2 | Collapse/expand an agent's reasoning ("thinking") trace | ✅ | `AssistantMessageCard` reasoning drawer |
| 2.3 | Read a fenced code block (diff, shell, file) with syntax-tinted keywords/strings | ✅ | `MessageContentView`/`CodeBlock` — ADR-007, PR #7 |
| 2.4 | Copy a single code block in one tap | ✅ | `CodeBlock` copy button + "Copied" confirmation — PR #7 |
| 2.5 | See a pending tool call and its arguments | ✅ | `ToolInvocationCard` |
| 2.6 | Copy a tool call's arguments or output | ✅ | Routed through `CodeBlock` when expanded — PR #7 |
| 2.7 | Approve / reject a pending tool call (1-tap, or Y/N) | ✅ | `ToolInvocationCard` approve/reject buttons + `FloatingApprovalHud` |
| 2.8 | Approve a tool call from the lock screen / notification | 🟡 | `FloatingApprovalHud` covers in-app; no lock-screen notification action confirmed in this pass (see `vibeterm`'s pattern) |
| 2.9 | Chat with a general-purpose LLM (OpenRouter/Ollama), independent of the SSH-connected agent | ⬜ | Not built — ADR-006 scopes this as a future `:core:core-llm` module, not an `oxproxion` import |

## 3. Terminal Feed tab (raw PTY stream)

| # | Scenario | Status | Notes |
|---|---|---|---|
| 3.1 | See the live raw pane output as it streams | ✅ | `outputHistory` + `LazyColumn` |
| 3.2 | ANSI colors / TUI apps (vim, htop) render correctly, not as escape codes | ⬜ | Gap — ADR-008 proposes vendoring Termux's `terminal-emulator`/`terminal-view` |
| 3.3 | Long-press and drag to select arbitrary text | ✅ | `SelectionContainer` — PR #7 (generic Compose selection; not ANSI-aware, see 3.2) |
| 3.4 | Copy the entire visible feed in one tap | ✅ | "Copy all" button — PR #7 |
| 3.5 | Native selection handles like a real terminal app | ⬜ | Gap — requires `terminal-view`'s `TextSelectionCursorController`, part of ADR-008 |
| 3.6 | Hardened clipboard read (reject URI/ContentProvider payloads, cap paste size) | ⬜ | Not ported — `moke`'s `Clipboard.kt` pattern identified as reusable, not yet copied |

## 4. Sharing & workflow shortcuts

| # | Scenario | Status | Notes |
|---|---|---|---|
| 4.1 | Share a snippet/URL/log from another app straight into the active session | ✅ | Android `ACTION_SEND` target — ADR-005 |
| 4.2 | 1-tap macro to launch the URUS 4-agent fleet or a DPIK workflow | ✅ | `WorkflowMacrosModal` |
| 4.3 | Gamepad/D-pad navigation without the soft keyboard | ✅ | `GamepadJoypadBar` |
| 4.4 | Quick-action bar for common keys (Esc, Ctrl, arrows) | ✅ | `QuickActionBar` |

## 5. Trust, audit & ops

| # | Scenario | Status | Notes |
|---|---|---|---|
| 5.1 | Every tool approval/rejection is logged and survives a cold restart | ✅ | `AgentAuditJournal` |
| 5.2 | On-device MCP server exposes device tools to the PC-side agent | ✅ | `:core:core-mcp`, 17 native tools — ADR-003 |
| 5.3 | MCP server binds loopback-only with constant-time token check | ✅ | `McpServerEngine.kt` |
| 5.4 | Device logs / notification list tools return real data (not stubs) | ⬜ | Known stub, tracked in `gaps-to-revisit.md` #5 — irrelevant to the terminal use case, matters only for remote device control |
| 5.5 | Signed release APK build gate | ✅ | Gate exists (`build.gradle.kts` fails fast without `KEYSTORE_PASSWORD`/`KEY_PASSWORD`); actually cutting a signed release is a deploy-time action, not a code gap |

## 6. File transfer & session export

| # | Scenario | Status | Notes |
|---|---|---|---|
| 6.1 | Browse the remote host's filesystem over SFTP | 🟡 | Backend only — `RemoteEntry`/`SshSession.listDirectory` in `:core:core-ssh` (built for "issue #528 SFTP file explorer"); **no `FilesScreen`/browser UI exists in `:app` yet** |
| 6.2 | Upload a file from Android to the remote host, resumable across drops | 🟡 | Backend only — `ResumableUpload.kt`'s `QueueSidecarResumableUploader` (durable checkpoint + resume-from-offset + atomic remote `mv`) already exists in `:core:core-ssh`; **no picker/queue UI wired to it** |
| 6.3 | "Taildrop"-style 1-tap send straight to the PC | ⬜ | Real Tailscale Taildrop is Tailscale's own P2P protocol driven through their local daemon API (`tailscale file cp` / the Tailscale app's share target) — ARH-Terminal can't drive that without embedding Tailscale's own client. The SFTP upload in 6.2, run over the same Tailscale-carried SSH connection the app already uses, gets the same practical outcome ("share → lands in a folder on my PC") without that dependency — recommend building on 6.2 rather than chasing literal Taildrop |
| 6.4 | Export a chat/session transcript to a Markdown file (oxproxion pattern) | ⬜ | Not built — no export code exists; would walk `List<AgentTurn>` into Markdown (user/assistant turns → `**You:**`/`**Agent:**`, tool calls → fenced blocks) and save via `MediaStore`/scoped storage |
| 6.5 | The app's own export/downloads folder shows up as a source when uploading | ⬜ | Not built — depends on 6.1/6.2 (an upload source picker to exist) and 6.4 (something to have been saved there); once both exist this is a few lines (default the picker's start directory to the export folder) |

## 7. Multi-session & in-app links

| # | Scenario | Status | Notes |
|---|---|---|---|
| 7.1 | Attach/detach the active psmux session via a toggle | ✅ | `SessionScreen`'s "Interactive psmux Attach/Detach Slider Card" — a real `Switch` (`Icons.Link`/`LinkOff`) wired to `viewModel.attachTmux()`/`detachTmux()`, already logged to the audit journal |
| 7.2 | Open several sessions/windows at once, each independently attachable | ⬜ | Gap — `SessionUiState` models exactly one `activeSessionName` + one `isAttached` flag; there's no session-list/tab model behind it. The attach toggle in 7.1 is real but singular — extending it to N parallel windows is a state-model change (`SessionUiState` → a list of per-window states), not just a UI tab bar |
| 7.3 | URLs in Agent Chat / Terminal Feed render as tappable hyperlinks | ⬜ | Not built — no linkify/URL-pattern code exists anywhere in the app today |

## Net gaps to close, in priority order

1. **ADR-008 — ANSI-aware Terminal Feed.** The single biggest remaining gap for
   "use this as my daily driver" — colored output and TUI apps don't render.
2. **Wire up the SFTP explorer + resumable uploader that already exist** (6.1,
   6.2) — this is mostly UI work on top of a backend that's already been built
   and tested (`RemoteEntry`, `ResumableUpload.kt`, `UploadAtomicityIntegrationTest`,
   `ResumableUploadIntegrationTest`); the highest-leverage gap on this list.
3. **`moke`'s hardened `Clipboard.kt` pattern** — cheap to port, closes a real
   (if narrow) main-thread DoS/leak vector once terminal paste is wired up.
4. **Lock-screen tool approval** (2.8) — nice-to-have, `vibeterm` has a working
   reference implementation (notification actions + optional biometric gate).
5. **Session-transcript Markdown export** (6.4) — small, self-contained, no
   dependency on anything else on this list.
6. **URL hyperlinking** (7.3) — small, self-contained.
7. **Multi-session windows** (7.2) — real architecture change (`SessionUiState`
   → per-window list), biggest single item on this list; worth its own ADR
   before starting.
8. **ADR-006 — OpenRouter/local-LLM chat module** — only if you actually want a
   second, agent-independent chat surface in the app; not required for the SSH/
   agent-pairing use case this app is built around.
