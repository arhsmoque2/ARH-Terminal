# Gotchas & Failure Capsules

## 1. psmux Control Mode Protocol
* **Symptom**: Raw ANSI escape garbage displayed instead of structured conversation cards.
* **Root Cause**: Connecting via standard interactive shell instead of passing `-CC` control mode flag.
* **Permanent Fix**: Ensure the bootstrap command specifies `psmux -CC attach -t <session> || psmux -CC new -s <session>`.
* **Verification**: Verify `%output %1` control mode tokens are emitted during handshake.

## 2. Compose Recomposition in Terminal Feed
* **Symptom**: Frame drops when terminal emits high-frequency output lines.
* **Root Cause**: Unstable parameters triggering full composable tree recompositions.
* **Permanent Fix**: Use `@Immutable` / `@Stable` data wrappers on terminal line chunks and enforce `io.nlopez.compose.rules` via Detekt.
