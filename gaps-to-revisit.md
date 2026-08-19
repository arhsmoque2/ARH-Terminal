# Gaps to Revisit

Findings from a code-level architecture review (2026-08-19), not caught by CI or
`asbuilt.md`'s "verified" claims — the quality gates check that things are *wired*,
not that they're *safe* or *durable*. Logged here for personal tracking, prioritized
against the actual use case: personal daily driver replacing ConnectBot, talking to
`psmux`/tmux on a Windows PC, over Tailscale, own devices only.

## P0 — blocks "replace ConnectBot" today

### 1. No foreground service — session likely dies when backgrounded
- **Where**: no `Service` with `startForeground()` anywhere in the app. `FOREGROUND_SERVICE`
  is declared in `AndroidManifest.xml` but nothing uses it. The MCP server / SSH session
  run in an application-scoped coroutine (`TerminalModule.kt`, `Dispatchers.Default`)
  with no OS signal telling Android to keep the process alive.
- **Why it's P0 for this use case**: ConnectBot's whole value is a session that survives
  screen-off, app-switch, and lock — backed by a persistent notification / foreground
  service. Without that here, expect the SSH/tmux session to drop within seconds-to-minutes
  of backgrounding on stock Doze, and much sooner on aggressive OEM battery management.
  A terminal you have to keep in the foreground with the screen on isn't a ConnectBot
  replacement yet.
- **Fix shape**: a foreground `Service` (persistent low-priority notification, e.g.
  "Connected to <profile>") owning the SSH/tmux/MCP lifecycle, started on connect and
  stopped on explicit disconnect — plus a battery-optimization exemption prompt on first run.

## P1 — worth fixing before trusting it with real keys, cheap either way

### 2. SSH private keys are Base64-"encoded", not encrypted, at rest
- **Where**: `ConnectionProfile.kt` — `toSerialized()` wraps the PEM in
  `Base64.getEncoder()` and prefixes it `"enc:"`, which reads as encryption but isn't.
  Stored in a plain TSV (`arh_profiles.tsv`) in app-internal storage.
- **Compounding factor**: `android:allowBackup="true"` in the manifest — pre-Android-12
  `adb backup`, or any rooted/file-manager access, gets the key trivially (one Base64 decode).
- **Risk in your scenario**: lower than it looks, since exfiltrating the key only helps an
  attacker who can *also* reach your Tailscale tailnet or the PC directly — but phone
  loss/theft or a shady backup tool is a realistic path, and this is cheap to fix.
- **Fix shape**: Android Keystore-backed key (EncryptedFile / Jetpack Security, or
  wrap with a Keystore AES key), and set `allowBackup="false"` or scope backup rules to
  exclude the profiles/audit files.

### 3. SSH host key verification defaults to accept-all
- **Where**: `SessionViewModel.kt:182` explicitly passes `KnownHostsPolicy.AcceptAll`
  (`PromiscuousVerifier`) to `SshConnection.connect(...)`. A real `KnownHostsFile` policy
  already exists in `:core:core-ssh` — the UI just never uses it.
- **Risk in your scenario**: meaningfully lower than general internet exposure, since
  Tailscale already gives you an authenticated, encrypted WireGuard tunnel between your
  own devices — a MITM would need to already be inside your tailnet. Still a real
  regression from ConnectBot's TOFU + pinning model, and someone else briefly on the
  same tailnet (shared/family Tailscale account, guest node, etc.) would be enough.
- **Fix shape**: wire the existing `KnownHostsFile` policy with TOFU-and-pin-on-first-connect
  UX (store fingerprint per profile, warn loudly on change) — same UX ConnectBot gives you.

## P2 — lower priority for this use case, note and move on

### 4. On-device MCP control server is plaintext + LAN-wide + single bearer token
- **Where**: `McpServerEngine.kt` — raw `ServerSocket(port)` binds all interfaces (not
  loopback-only), plain HTTP, auth is one bearer token compared with `!=` (not
  constant-time). Anyone on the same WiFi/LAN with the token can drive the phone's UI
  (tap, swipe, type, launch apps, read clipboard).
- **Why P2 here**: this is a separate feature (remote agent controls the *phone's own
  screen* via Accessibility) from the ConnectBot-replacement use case (SSH into the PC).
  You likely won't turn this on for daily terminal use. Still worth tightening
  (bind loopback + require it over Tailscale only, or add TLS) before flipping it on,
  especially on shared/public WiFi.

### 5. Two of the 17 advertised MCP tools are non-functional stubs
- `android_get_device_logs` returns a hardcoded string, not real logcat output.
- `android_notification_list` always returns an empty array — no
  `NotificationListenerService` is implemented.
- Not caught by `ci_asbuilt_doctor.py`, which only checks that a tool name is
  *registered*, not that it *does* anything. Irrelevant to the terminal/ConnectBot
  use case; matters only if you lean on the remote-control side.

### 6. Debug-only, unsigned APK from CI
- CI runs `assembleDebug`, not a signed release build. Fine for personal sideloading,
  but expect debug-build overhead (larger, unoptimized, debuggable) — build a signed
  release APK yourself once the app is your daily driver.

---

## Bottom line: is it "good to use as if I'm using ConnectBot" today?

**Not yet — one real blocker, plus two cheap follow-ups.**

Given your actual threat model (Tailscale-only, own devices, personal use), items
**#2 and #3** are lower-stakes than they'd be on an open network — Tailscale is already
doing a lot of the job ConnectBot's host-key pinning does. They're worth fixing because
they're cheap and this is meant to hold real credentials, but they're not what stands
between you and switching over.

**Item #1 is the actual blocker.** ConnectBot's core promise — lock the screen, switch
apps, come back, session's still there — isn't implemented. As built, this behaves more
like "a terminal you keep open and on-screen" than "a background SSH client." For your
specific case (talking to Windows psmux/tmux instead of installing WSL2), the
connection layer itself (SSH → psmux → tmux) is solid and well-tested; what's missing
is the durability layer that makes it a ConnectBot replacement rather than a terminal
you have to babysit.

**Suggested order**: fix #1 (foreground service) first — it's the functional
difference between "usable daily" and "not yet." Then #3 (host key pinning) and #2
(encrypted key storage) as a pair, since both touch the same connect/profile flow.
#4–#6 only matter once you start using the remote-control side or ship this beyond
yourself.
