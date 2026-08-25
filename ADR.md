# Architectural Decision Records (ADR) 🏛️

## ADR-001: Native `psmux -CC` Control Mode over Raw PTY Emulation
* **Status**: Accepted & Implemented
* **Context**: Traditional mobile SSH clients (Termius, ConnectBot) render VT100/ANSI terminal characters directly into raw terminal screens, making it difficult on mobile screens to review AI diffs, toggle tool executions, or collapse lengthy reasoning traces.
* **Decision**: Adopt `psmux.exe` / `tmux -CC` Control Mode protocol. The Windows host acts as the persistent session state manager, streaming structured `%output`, `%window-add`, and `%layout-change` events over SSH.
* **Consequences**: Enables polymorphic Jetpack Compose UI rendering while preserving 100% terminal multiplexer capabilities and detachment recovery.

---

## ADR-002: Polymorphic Agent Turn Cards with Live Tool Diff Invocations
* **Status**: Accepted & Implemented
* **Context**: Developers interacting with CLI AI agents (Claude Code, Antigravity, Codex) need immediate visual distinction between user prompts, agent thoughts, file diffs, and execution results.
* **Decision**: Parse incoming terminal character streams using modular agent parsers (`ClaudeCodeParser`, `CodexParser`, `OpenCodeReader`) into structured `AgentTurn` models rendered as dedicated Material 3 cards with collapsible reasoning drawers and diff previews.
* **Consequences**: Significant increase in legibility on mobile screens without losing the underlying raw terminal stream.

---

## ADR-003: Unified Super-App (Absorption of Android Remote Control MCP)
* **Status**: Accepted & Implemented
* **Context**: Rather than running two separate Android apps (one terminal client for the user, and one background MCP server daemon for PC agents), we evaluated embedding the MCP server directly into `ARH-Terminal`.
* **Decision**: Absorb the 56-tool MCP server into `:core:core-mcp` with an embedded HTTP/SSE JSON-RPC 2.0 daemon and a dedicated UI dashboard tab (`McpBridgeDashboard`).
* **Consequences**: Single APK to install and manage; bidirectional synergy (phone controls PC agent $\leftrightarrow$ PC agent commands phone).

---

## ADR-004: End-to-End Encrypted (AES-256-GCM) WebSocket Relay for Cloud NAT Traversal
* **Status**: Accepted & Implemented
* **Context**: When outside LAN or Tailscale networks, developers need remote access to devbox agents behind strict NAT/CGNAT firewalls without configuring port forwarding.
* **Decision**: Implement `:core:core-relay` utilizing AES-256-GCM encryption with secure random 12-byte IVs and pre-shared passphrases communicating over WebSocket relay channels.
* **Consequences**: Zero-trust remote connection capability with full payload encryption.

---

## ADR-005: Android Share Target & Fast Workflow Macros for DPIK Tender Operations
* **Status**: Accepted & Implemented
* **Context**: Copying error logs, document text, or URLs from mobile apps (WhatsApp, Gmail, Chrome, DPIK portals) and switching between apps to paste into the terminal creates unnecessary friction.
* **Decision**: Register `ARH-Terminal` as an Android `ACTION_SEND` target in `AndroidManifest.xml` and provide a 1-tap Workflow Macros modal for DPIK tender quality gates, git diffs, and board lookups.
* **Consequences**: Frictionless zero-paste sharing of snippets directly into active agent sessions.

---

## ADR-006: Reject Merging `oxproxion`; Scope Any LLM-Chat Feature as a New Module
* **Status**: Accepted
* **Context**: `oxproxion` (a fork of `stardomains3/oxproxion`) is an OpenRouter/Ollama/LM Studio chat client for Android. It was evaluated as a merge candidate with ARH-Terminal since both are Android SSH/agent-adjacent apps. They diverge on every axis that matters for a merge: UI toolkit (View Binding vs. Jetpack Compose), DI (none vs. Hilt), module layout (single `:app` vs. multi-module), and domain (chat-with-an-LLM vs. SSH/tmux pairing with a remote agent). `oxproxion`'s repo also carries unrelated Python automation scripts and large retrospective dumps at its root.
* **Decision**: Do not merge or fork `oxproxion` code into ARH-Terminal. If OpenRouter/local-LLM chat is wanted as an ARH-Terminal feature, port only the OpenRouter HTTP client behavior (model list, streaming, presets, credits lookup) into a new `:core:core-llm` (or `:feature:chat`) module, rebuilt against ARH-Terminal's own conventions (Compose, StateFlow, Hilt) rather than importing `oxproxion`'s View-Binding/Ktor implementation wholesale.
* **Consequences**: Keeps ARH-Terminal on one architecture and one UI toolkit; avoids dragging in `oxproxion`'s unrelated Python tooling and license surface. Costs a from-scratch implementation port rather than a copy-paste import — no code has been written for this yet.

---

## ADR-007: Claude/ChatGPT-Style Copyable Code Blocks in Agent Chat
* **Status**: Accepted & Implemented ([PR #7](https://github.com/arhsmoque2/ARH-Terminal/pull/7))
* **Context**: `AssistantMessageCard` rendered agent replies as a single plain `Text`, so any fenced code the agent returned (diffs, shell commands, file contents) was unformatted and had no per-block copy affordance — a materially worse mobile experience than the Claude and ChatGPT apps' chat UIs.
* **Decision**: Add `MessageContentView`/`CodeBlock` (`ui/conversation/MessageContent.kt`) — a small, dependency-free Compose parser that splits assistant text on ` ``` ` fences into prose and code segments, rendering each code segment as its own dark, horizontally-scrolling panel with a language label and a 1-tap copy button (`LocalClipboardManager`, brief checkmark confirmation). `ToolInvocationCard`'s expanded arguments/output route through the same `CodeBlock`. No Markdown-rendering library was added — the parser only needs to recognize fences, not full Markdown.
* **Consequences**: Assistant code is readable and copyable per-block, matching user expectations set by Claude/ChatGPT. Does not add ANSI-escape parsing (see ADR-008) — this only covers agent-authored fenced code, not the raw terminal stream.

---

## ADR-008: Vendor Termux's `terminal-emulator`/`terminal-view` for an ANSI-Aware Terminal Feed
* **Status**: Proposed (not yet implemented)
* **Context**: The "Terminal Feed" tab renders raw `outputHistory: List<String>` chunks through a plain `Text` composable — no ANSI escape parsing, so colored output, cursor movement, and TUI apps (vim, htop, Claude Code's own box-drawing UI) show as raw escape codes instead of rendering correctly. A survey of comparable Android SSH-terminal apps (`metoo2008/vibeterm`, `briqt/moke`) found both solve this the same way: vendoring Termux's `terminal-emulator` (VT100/ANSI state machine) and `terminal-view` (the rendering `View`, which also provides native long-press-drag text selection via `TextSelectionCursorController`) rather than writing a parser from scratch.
* **Decision**: Vendor `terminal-emulator` + `terminal-view` as new Apache-2.0 modules, following `moke`'s isolation pattern — not `vibeterm`'s, which modified the vendored files directly and became GPL-3.0 for the entire app as a result (and consequently can't ship on Google Play). Keep the vendored modules untouched beyond what's strictly required (matching `moke`'s own contribution rule), and wire ARH-Terminal's existing `:core:core-tmux`/`:core:core-ssh` output into a transport-agnostic `TerminalSession`/`TerminalView` pairing, replacing the current `outputHistory` + plain `Text` rendering in the Terminal Feed tab. The `Agent Chat` tab (ADR-007) is unaffected — it stays Compose-native since it renders structured turns, not a live PTY stream.
* **Consequences**: Real ANSI colors, correct TUI rendering, and native drag-handle text selection/copy in the Terminal Feed tab, matching the UX of `moke`/`vibeterm`. Adds two new vendored modules, an Apache-2.0 `NOTICE` obligation, and non-trivial wiring work (resize-on-rotate, scrollback, transport SPI) — scoped as a follow-up, no code written yet.
