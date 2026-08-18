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
