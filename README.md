# ARH-Terminal

> **Voice-first, psmux/tmux-native, agent-aware Android terminal & coding workspace for ARH-OS.**

---

## 🏛️ Architecture & Highlights

- **`psmux` / `tmux -CC` Control Mode**: Natively attaches to Windows `psmux` or Linux `tmux` without full-screen ANSI scraping. Renders clean, readable turn-by-turn agent conversation cards on Android.
- **Agent Conversation Engine (`core:core-agents`)**:
  - `ClaudeCodeParser`: Real-time JSONL event stream parsing for Claude Code turns (user prompt, assistant response, thinking, tool calls, tool results).
  - `CodexParser` & `OpenCodeReader`: Turn extractors for OpenAI Codex and OpenCode sessions.
- **Dual View Modes**:
  - **Agent Chat**: Clean Compose cards for human prompts, agent reasoning, and collapsible tool invocations with embedded outputs.
  - **Terminal Feed**: Hardware-accelerated 120Hz raw stream for interactive CLI control.
- **One-Tap Agent Approvals (HUD)**: Context-aware prompt detection for CLI permission and `[y/N]` confirmation prompts.
- **Mosh & UDP Roaming**: Resilient cellular-to-WiFi handover with native `mosh-client` JNI integration.
- **E2EE Relay Bridge**: Connects securely to workstation dev sessions behind NAT/firewalls without exposing open SSH ports.

---

## 🛠️ Modules & Tech Stack

```
ARH-Terminal/
├── app/                  # Main Android Application (Compose UI, ViewModels, Hilt DI)
│   ├── ui/conversation/  # AgentTurnCard, ToolInvocationCard, Message bubbles
│   ├── ui/session/       # SessionScreen, SessionViewModel, ViewMode switcher
│   └── di/               # Hilt TerminalModule
├── core/
│   ├── core-ssh/         # SSH client transport (sshj + BouncyCastle + keepalive)
│   ├── core-tmux/        # psmux / tmux -CC control-mode protocol parser & TmuxClient
│   └── core-agents/      # Agent parsers (ClaudeCodeParser, CodexParser, OpenCode)
└── config/detekt/        # Kotlin static analysis + Jetpack Compose rules
```
