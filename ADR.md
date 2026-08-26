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

---

## ADR-009: Robolectric + Roborazzi as the Primary UI Quality Gate; Maestro Demoted to On-Demand
* **Status**: Accepted & Implemented ([PR #6](https://github.com/arhsmoque2/ARH-Terminal/pull/6))
* **Context**: The original plan (below, struck through) was a headless-emulator Maestro E2E gate blocking every push/PR. In practice it hit a cascade of issues that were each individually fixable but collectively pointed at a mismatch between the tool and what was actually being tested: (1) `Modifier.testTag(...)` isn't exposed as an Android accessibility `resource-id` unless `testTagsAsResourceId = true` is set — a real gotcha, but even after fixing it the flows still failed; (2) the headless `aosp_atd`/SwiftShader emulator's own Maestro automation driver intermittently failed to start within its startup timeout — infra flake unrelated to the app; (3) mid-debug, a swap to a third-party tool confusingly named `maestro-runner` (installed via unauthenticated `curl | bash` from an unrecognized domain, and actually a different UIAutomator2-based stack, not Maestro) was attempted and also failed, and was reverted. Underlying all of this: the 4 flows in question (cold-launch/theme, form/modal lifecycle, HUD/joypad visual-clash, 200%-font-scale/overflow) are single-process Compose rendering concerns — they never needed a real emulator or cross-app UI automation to begin with. Maestro's actual differentiator — driving a real device across process/app boundaries (an OAuth browser handoff, a system file/document picker) — isn't exercised by any of them; ARH-Terminal's GitHub auth is the RFC 8628 Device Flow, which never leaves the app.
* **Decision**:
  1. **Tier 1 (primary, blocking)**: `AppUiQualityGateTest.kt` — Robolectric + Compose UI Testing (`createComposeRule`, `onNodeWithTag`, `assertIsDisplayed`, `RuntimeEnvironment.setFontScale`) asserting on the composed semantics tree, in-process, no emulator. Runs in the existing fast `unit-tests-and-doctor` CI job. Covers what all 4 original Maestro flows checked, including HUD/joypad-clash via `boundsInRoot()` overlap assertions (a real geometric check, not just "is composed").
  2. **Tier 2 (primary, non-blocking yet)**: `AppUiVisualRegressionTest.kt` — Roborazzi (Robolectric Native Graphics) pixel-level screenshot capture for the states semantics assertions can't fully cover (clipping, color, spacing regressions). No baseline goldens are committed yet, so these currently only capture (uploaded as a CI artifact); recording baselines locally (`./gradlew :app:recordRoborazziDebug`) and adding `-Proborazzi.test.verify=true` to CI turns this into a real diff-gate as a follow-up.
  3. **Maestro: demoted to on-demand** (`maestro-ui-gate.yml`, `workflow_dispatch` only). Kept for its actual strength — a real cross-app/process E2E check — reserved for if/when a feature needs one (a browser-based OAuth redirect, the SAF file-picker flow in Transfer Files), not run on every push/PR.
* **Consequences**: The blocking CI gate is fast, deterministic, and immune to the whole class of failures above (no emulator boot, no accessibility-tree mapping, no driver startup). Maestro's local-first workflow (`maestro studio`, `RECIPES.md`) remains valid for whatever narrower suite eventually needs it. Cost: Tier 2 isn't yet an enforced gate (no goldens recorded), and this ADR no longer describes the dual-tiered-Maestro design PR #6 originally shipped — superseded in-place rather than as a new ADR, since the original was never actually load-bearing in production.

<details>
<summary>Original decision (superseded, kept for history)</summary>

* **Context**: Static linting (Detekt) and unit tests (JUnit/Robolectric) cannot detect visual collisions between overlay components (Floating Approval HUD, Gamepad Joypad Bar, QuickActionBar, BottomSheets), modal dismissal lifecycles, or text clipping under accessibility font scaling (200%). However, executing headless Android emulators (`aosp_atd` API 34 with `-gpu swiftshader_indirect`) in cloud CI introduces severe CPU rasterization delays (8–15s for Compose initial composition), causing standard Maestro assertion timeouts (~2s) to fail prematurely.
* **Decision**:
  1. **Dual-Tiered Architecture**: Establish a fast static/unit gate (<3m) for routine PRs alongside a dedicated Maestro E2E workflow (`maestro-ui-gate.yml`).
  2. **SwiftShader Resilience**: Wrap initial cold-launch and navigation assertions with `extendedWaitUntil: { visible: { id: "..." }, timeout: 30000 }` and pre-warm app startup post-install in CI.
  3. **Local-First Developer Workflow**: Standardize on native Windows Maestro CLI + `maestro studio` (interactive web inspector at `localhost:9999`) for sub-second flow creation and debugging on real hardware / GPU-backed emulators without CI wait penalties.
* **Consequences**: Deterministic, flake-resistant E2E test runs in CI; visual collision and 200% font-scale regressions are automatically caught; developer authoring is accelerated via interactive live-hierarchy inspection.

</details>

