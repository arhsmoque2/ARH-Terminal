# Gotchas & Failure Capsules

## 1. psmux Control Mode Protocol
* **Symptom**: Raw ANSI escape garbage displayed instead of structured conversation cards.
* **Root Cause**: Connecting via standard interactive shell instead of passing `-CC` control mode flag.
* **Permanent Fix**: Ensure the bootstrap command specifies `psmux -CC attach -t <session> || psmux -CC new -s <session>`.
* **Verification**: Verify `%output %1` control mode tokens are emitted during handshake.

## 2. Agent JSONL Streaming vs Partial Chunks
* **Symptom**: Agent messages get cut in half when receiving partial network buffers.
* **Root Cause**: Reading raw byte packets as individual messages rather than buffering by newline delimiter.
* **Permanent Fix**: `ClaudeCodeParser` operates strictly on completed lines (`text.lines()`), buffering unfinished fragments.

## 3. Compose Recomposition in Terminal Feed
* **Symptom**: Frame drops when terminal emits high-frequency output lines.
* **Root Cause**: Unstable parameters triggering full composable tree recompositions.
* **Permanent Fix**: Use `@Immutable` data classes (`AgentTurn`, `PaneData`, `SessionUiState`) and enforce `io.nlopez.compose.rules` via Detekt.
