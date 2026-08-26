# ARH-Terminal Gotchas & Failure Capsules 💡

### 1. Android Base64 in JVM Unit Tests
* **Symptom**: `NullPointerException` during unit test execution of cryptographic cipher routines.
* **Root Cause**: `android.util.Base64` is an Android framework stub when running on host JVM test suites (`testDebugUnitTest`).
* **Permanent Fix**: Use standard `java.util.Base64` (available on Android API 26+ and Java 8+) which runs identically in both Android runtime and JVM unit test runners.
* **Verification**: `./gradlew :core:core-relay:test` passes with 0 errors.

### 2. Detekt Submodule Plugin Resolution under AGP 9.1+
* **Symptom**: `Cannot add extension with name 'kotlin', as there is an extension already registered with that name.`
* **Root Cause**: AGP 9.1+ embeds Kotlin Android plugins natively; applying `alias(libs.plugins.kotlin.android)` alongside `alias(libs.plugins.android.library)` creates a collision.
* **Permanent Fix**: In submodules, apply only `alias(libs.plugins.android.library)` and `alias(libs.plugins.detekt)`.
* **Verification**: `./gradlew detekt` executes cleanly across all 6 modules.

### 3. Windows Gradle Daemon File Locking (`classes.jar`)
* **Symptom**: `FileSystemException: classes.jar: The process cannot access the file because it is being used by another process`.
* **Root Cause**: Windows process holding open file handles when parallel or cancelled gradle daemons are lingering.
* **Permanent Fix**: Run `.\gradlew --stop` before running full multi-module compilation tasks.

### 4. `android.util.Log` Mocking in JVM Library Unit Tests
* **Symptom**: `AssertionError` or `RuntimeException: Method not mocked` when tests hit methods that log via `android.util.Log`.
* **Root Cause**: AGP unit tests on JVM stubs throw exceptions for unmocked Android framework calls by default.
* **Permanent Fix**: Add `testOptions { unitTests.isReturnDefaultValues = true }` inside `android { ... }` in submodule `build.gradle.kts` files.
* **Verification**: `./gradlew :core:core-tmux:test` passes all 232 tests without stub crashes.

### 5. Coroutine Flow Collector Deadlocks in Test Fixtures
* **Symptom**: `withTimeout` hangs for 15s in test cases simulating event flood streams.
* **Root Cause**: Flow collector suspended on an uncompleted `CompletableDeferred` while the upstream producer saturated the SharedFlow buffer, stalling the underlying reader before it could feed the test barrier.
* **Permanent Fix**: Ensure test collector does not synchronously block the event loop, or complete gates before awaiting downstream barriers.
* **Verification**: `TmuxClientPaneOutputTest` passes in <10s.

### 6. Release Signing Fails with `BadPaddingException` When Secrets Are Missing or Empty
* **Symptom**: `:app:assembleRelease` fails deep in AGP packaging with `KeytoolException: ... keystore password was incorrect` / `javax.crypto.BadPaddingException: Given final block not properly padded`.
* **Root Cause**: When GitHub Actions secrets (`secrets.KEYSTORE_PASSWORD`) are unset, the workflow env block injects empty strings (`""`) rather than null. In Kotlin, `System.getenv(...) ?: fallback` evaluates to `""` instead of the fallback password, passing an empty password to the PKCS12 keystore decryptor and triggering `BadPaddingException`.
* **Permanent Fix**: Removed hardcoded fallback password. `storePassword`/`keyPassword` are read strictly from env vars; a `gradle.taskGraph.whenReady` check fails fast with an actionable message if release signing tasks run without `KEYSTORE_PASSWORD` and `KEY_PASSWORD`. Set `KEYSTORE_PASSWORD` and `KEY_PASSWORD` (`arhterminal2026`) as GitHub Actions repo secrets (`Settings -> Secrets and variables -> Actions`) and local env vars.
* **Verification**: All GitHub Actions CI jobs (`Assemble Debug APK & Verify Signing` and `Assemble Signed Release APK & Gate`) pass with green status, producing verified `arh-terminal-release-apk` and `arh-terminal-debug-apk` artifacts.

### 7. Debug APK Fails `ci_apk_signing_doctor.py`'s Size Budget Gate
* **Symptom**: `Assemble Debug APK & Verify Signing` fails with `APK Size (23.25 MB) exceeds maximum allowed budget (8.00 MB)!` even though the debug build itself succeeded.
* **Root Cause**: `ci_apk_signing_doctor.py`'s `--max-size-mb` defaults to `8.0`, sized for the shipped **release** artifact. The debug variant is unminified, carries debug symbols, and pulls in `debugImplementation(libs.androidx.compose.ui.tooling)`, so it's routinely 3-4x larger — that's expected, not a regression.
* **Permanent Fix**: The `Run APK Signing & Alignment Doctor (Debug)` CI step passes `--max-size-mb 35` for the debug APK; the release step keeps the tight default `8.0` MB budget, since that's the artifact users actually install.
* **Verification**: `./gradlew :app:assembleDebug` output passes the doctor with the debug-scoped budget; the release budget is unchanged.

### 8. Maestro Blackbox Tests vs Compose Dynamic Node Matching
* **Symptom**: Maestro fails to find buttons or elements when using localized text or dynamic state labels across orientation/font scale changes.
* **Root Cause**: Relying solely on text matching (`assertVisible: "Connect"`) can fail when text wraps, abbreviates, or changes based on connection state.
* **Permanent Fix**: Apply explicit `Modifier.testTag(...)` to all interactive surfaces, inputs, and modals (`app_title`, `input_host`, `btn_connect`, `modal_workflow_macros`). Validate tag availability using `python scripts/ci_maestro_doctor.py`.
* **Verification**: `python scripts/ci_maestro_doctor.py` confirms 100% testTag resolution across all 4 `.maestro/` flows.

### 9. `Secret Leak Scan` Fails Only on `workflow_dispatch` (Manual/Remote-Triggered) Runs
* **Symptom**: A normal `push` to `main` passes `Secret Leak Scan` cleanly, but manually dispatching the same workflow on the same commit (`gh workflow run` / the GitHub API / an agent using `actions_run_trigger`) fails it with a `generic-api-key` finding in `RECIPES.md`, pointing at a commit from days earlier.
* **Root Cause**: `gitleaks/gitleaks-action@v2` scans only the incremental diff on `push`/`pull_request` events (it has a before/after SHA to diff), but has no baseline on a manual `workflow_dispatch` run, so it falls back to scanning **full git history**. That surfaced a real bearer-token example value committed in `RECIPES.md` on 2026-08-18 — already redacted to a placeholder on current `main`, but still sitting in git history forever, since no push has ever re-touched that exact line since.
* **Permanent Fix**: Rather than rewriting history to purge one already-dead credential (disruptive to every existing clone/fork, for a token that's dynamically regenerated per `McpServerEngine` session anyway — see `asbuilt.md`), added `.gitleaksignore` with the exact `commit:file:rule:line` fingerprint. This is a deliberate exception, not a blanket exemption: every other rule and every other file/line stays fully scanned, including future edits to `RECIPES.md` itself. Revisit with a real history rewrite only if this pattern (live secrets landing in docs) recurs — one dead historical finding doesn't justify it.
  * First attempt used `.gitleaks.toml` with `[allowlist] fingerprints = [...]` — gitleaks loaded the config fine (confirmed in the debug log) but the finding still fired: `fingerprints` isn't a real key in gitleaks' `[allowlist]` schema, so it was silently ignored rather than erroring. `.gitleaksignore` (a plain file, one fingerprint per line, gitleaks' actual purpose-built mechanism for this) is what's verified working — see below.
* **Verification**: Manually dispatched `ci.yml` (the trigger that forces the full-history scan) three times against the real workflow: (1) before any fix — reproduced the failure; (2) with the `.gitleaks.toml`-only attempt — same failure, same fingerprint, confirming that approach was a no-op; (3) with `.gitleaksignore` — `32 commits scanned... no leaks found`, job green. Each step confirmed by reading that run's own log, not assumed from the previous one.

### 10. Headless ATD Emulator Cannot Install ARM-Only APK
* **Symptom**: CI emulator installation fails with `INSTALL_FAILED_NO_MATCHING_ABIS`.
* **Root Cause**: The workflow runs an `x86_64` Android emulator while the debug APK was packaged only for ARM ABIs.
* **Permanent Fix**: Package `x86` and `x86_64` alongside the ARM variants in the debug build's `abiFilters`.
* **Verification**: The corrected CI run assembled the APK and reported `adb install -r ... Success` on the x86_64 emulator.

### 11. Maestro CLI Installer Flag Case
* **Symptom**: The emulator installs the APK successfully but the workflow exits 127 with `maestro: not found`.
* **Root Cause**: The installer used curl's uppercase `-F` form flag (`-FsSL`) instead of lowercase `-f` fail flag, so Maestro was never installed.
* **Permanent Fix**: Use `curl -fsSL`, then verify the installed executable and print its version before adding its directory to `GITHUB_PATH`.
* **Verification**: The workflow must pass the explicit executable/version check before reaching the emulator test step.

### 12. Maestro Does Not Provide a `setFontScale` Flow Command
* **Symptom**: Maestro rejects a flow with `Invalid Command: setFontScale` before running assertions.
* **Root Cause**: Font scale is an Android system setting, not a supported Maestro flow command.
* **Permanent Fix**: Set `system/font_scale` in the emulator runner script and restore `1.0` with an EXIT trap; keep the flow focused on layout assertions.
* **Verification**: The flow parses under the installed Maestro CLI and the emulator script applies 2.0 before tests, restoring 1.0 on exit.

### 13. Jetpack Compose `Modifier.testTag` Invisible to Maestro Without `testTagsAsResourceId = true`
* **Symptom**: Maestro flows fail with `Assertion is false: id: <test_tag> is visible` (timing out after 30+ seconds), even though the app cold-launches cleanly without any crashes or errors in logcat.
* **Root Cause**: In Jetpack Compose, `Modifier.testTag(...)` sets internal Compose Semantics (`SemanticsProperties.TestTag`). By default, Compose does NOT expose `testTag` as an Android `resource-id` / `viewIdResourceName` in the OS Accessibility hierarchy (`AccessibilityNodeInfo`). External test drivers (Maestro, UIAutomator, Accessibility Services) inspecting the view hierarchy by `id:` cannot see any Compose test tags.
* **Permanent Fix**: Enable `testTagsAsResourceId = true` in the root Composable's semantics in `MainActivity.kt`:
  ```kotlin
  Surface(
      modifier = Modifier
          .fillMaxSize()
          .semantics {
              @OptIn(ExperimentalComposeUiApi::class)
              testTagsAsResourceId = true
          }
  ) { ... }
  ```
  Also added static enforcement in `scripts/ci_maestro_doctor.py`.
* **Verification**: `python scripts/ci_maestro_doctor.py` validates `testTagsAsResourceId = true` presence and 100% testTag resolution.
* **Caveat that cost real time (see #16)**: this fix is necessary but was **not sufficient** on this codebase — after applying it, real CI runs where the Maestro driver started cleanly and the flows genuinely executed (1–1.5 min each, not an instant fail) still failed on the identical `id: ... is visible` assertions, for a reason never fully root-caused. Don't treat this fix alone as proof the flake is solved; verify against a run where the flows actually ran (see #18 for telling that apart from a driver-startup no-op).

---

## Lessons from Retiring the Maestro-Blocking CI Gate (PR #6)

The gotchas below were all hit chasing down why 4 Maestro E2E flows kept failing in CI, across several distinct and unrelated root causes. The eventual resolution wasn't "fix Maestro harder" — it was recognizing that none of the 4 flows actually needed a real emulator or cross-app UI automation (they were all single-process Compose rendering checks), and replacing them with Robolectric + Compose UI Testing (blocking) and Roborazzi (non-blocking screenshot capture), demoting Maestro to on-demand (`workflow_dispatch`) for if/when a *genuine* cross-app scenario (an OAuth browser redirect, a system file picker) needs it. Full rationale in `ADR.md` (ADR-009). If you're standing up a similar Compose + emulator-based E2E CI gate, read #14–#21 before you start — most of this is generic to any Robolectric/Compose/Maestro-on-GitHub-Actions stack, not ARH-Terminal-specific.

### 14. `reactivecircus/android-emulator-runner`'s Multi-Line `script:` Block Runs Each Line as a Separate `sh -c` Call
* **Symptom**: A `script:` block like:
  ```yaml
  script: |
    maestro test .maestro/ || {
      adb logcat -d -t 500
      exit 1
    }
  ```
  fails with a shell syntax error (unclosed brace / unexpected EOF) even though the YAML/shell looks correct.
* **Root Cause**: This action executes the `script:` input **one line at a time**, each as its own `sh -c "<line>"` invocation — not as a single multi-line script. A `{ ... }` block spanning multiple lines breaks because the opening `{` and its body land in separate shell invocations.
* **Permanent Fix**: Collapse any `{ }` fallback/error-handling block onto a single line: `maestro test .maestro/ || { adb logcat -d -t 500; exit 1; }`.
* **Verification**: The step executes past the `maestro test` line without a shell syntax error, whether it passes or fails on the actual test assertions.

### 15. Wrong Activity Class Path in `adb shell am start -n`
* **Symptom**: `adb shell am start -n com.example.app/.ui.MainActivity` fails silently or the app never actually launches, so every subsequent Maestro assertion times out waiting for a screen that never appeared.
* **Root Cause**: The intent-component path has to match the actual manifest `android:name` and the class's real `package` declaration exactly — `.ui.MainActivity` is a plausible-looking guess if the class *feels* like it belongs under a `ui` package, but doesn't match where it actually lives.
* **Permanent Fix**: Verify against the source of truth before trusting the CI script: `MainActivity.kt`'s `package` declaration and `AndroidManifest.xml`'s `<activity android:name="...">` entry must agree with the `-n` argument.
* **Verification**: `adb shell am start -n <verified-path>` in the CI log actually returns a launch confirmation, and the very next assertion (even a trivial "is anything on screen" check) succeeds.

### 16. Chasing a Real Bug Fix Doesn't Guarantee the Symptom Resolves — Verify Against a Run Where the Test Actually Executed
* **Symptom**: A correct, well-diagnosed fix (see #13) is applied and CI still shows the exact same failure message on the next run — easy to misread as "the fix didn't work."
* **Root Cause**: Two independent failure modes were stacked on top of each other: the accessibility-mapping bug (#13) *and* an unrelated CI driver-startup flake (#18) that made the very next run fail before the fix could even be exercised. The failure *message* looked identical in both cases (`Assertion is false: id: ... is visible`), but the *duration* of the failing flow told the real story — an instant (~3s) fail is the flow never running at all; a fail after 1+ minutes means it genuinely executed and asserted.
* **Permanent Fix**: When investigating a stubborn CI test failure, use flow/test duration (and whether any output artifact was actually produced — #19) to distinguish "my fix didn't work" from "an unrelated infra issue prevented my fix from being tested at all," before concluding either way.
* **Verification**: A CI run where the target flow's reported duration is consistent with real execution (not near-instant), and where a resulting report/artifact was actually produced.

### 17. Never Pipe an Unfamiliar `curl | bash` Installer Into CI, Especially One With a Name Close to a Well-Known Tool
* **Symptom**: Mid-debugging, a CI step is changed to install a differently-named tool (e.g. `maestro-runner` instead of `maestro`) via `curl -fsSL https://<unfamiliar-domain>/install/<tool> | bash`, and it either fails outright or silently behaves like a *different* product than intended.
* **Root Cause**: The near-identical name invited treating it as a drop-in replacement without verifying what it actually was. In this case the installed tool provisioned a completely different automation stack (UIAutomator2/Appium-style) under the hood — not Maestro at all — and the install itself was an unauthenticated, unpinned (no checksum/signature) third-party script.
* **Permanent Fix**: Don't substitute a CI dependency for an unfamiliar one under debugging pressure without checking: (a) is this a project you actually recognize/trust, (b) does its own output match what you expect it to be doing, (c) is there a checksum/signature you can pin. Revert immediately if any of those don't check out — a supply-chain risk is not worth the time saved. Prefer the original, well-known tool's official install source (e.g. `get.maestro.mobile.dev` for real Maestro) even when actively debugging why it's failing.
* **Verification**: `grep` the CI workflow for every `curl | bash` (or equivalent) and confirm each one's domain and installed binary is the tool you actually intend.

### 18. Maestro's Own Android Automation Driver Can Fail to Start Under Headless-Emulator Resource Contention
* **Symptom**: `maestro test` fails almost immediately with `MaestroDriverStartupException$AndroidDriverTimeoutException: Maestro Android driver did not start up in time on emulator [ ... ] (driver port 42567)` — no flow assertion ever ran.
* **Root Cause**: Maestro pushes and starts its own on-device automation service (bound to a local `adb forward`-ed port) before running any flow. On a resource-constrained headless CI runner, that service can fail to come up within Maestro's own startup timeout — independent of anything in the app or the flows. A tell-tale sign: the emulator's own boot time (from launch to install-ready) is also abnormally slow on that run (e.g. 6 min vs. the usual 1–2).
* **Permanent Fix**: Treat this as infra flake, not an app/flow bug — re-run once (see the repo's PR-driving conventions on when a re-run is warranted) rather than immediately re-diagnosing app code. If it recurs frequently, consider a smaller/faster AVD image or fewer parallel CI jobs contending for the runner.
* **Verification**: A re-run of the same commit either passes, or fails with a *different*, flow-specific error — either outcome confirms this run's failure wasn't caused by your last change.

### 19. `actions/upload-artifact@v4` Needs `if-no-files-found: ignore` (or `warn`) for Directories That May Not Exist
* **Symptom**: An upload-artifact step for a debug/screenshot output directory (e.g. `app/build/outputs/roborazzi/`) that may not exist on every run — either because the relevant tests didn't run, or a step upstream failed before producing it.
* **Root Cause**: `upload-artifact@v4`'s default `if-no-files-found: warn` still logs a warning and (depending on other settings) can be noisy or, with `error`, fail the job outright for a legitimately-empty/absent path.
* **Permanent Fix**: Set `if-no-files-found: ignore` on any artifact-upload step whose source directory is expected to sometimes be empty or absent by design (as opposed to a real build artifact that should always exist, like an APK).
* **Verification**: A CI run where the underlying tests didn't produce that directory still completes the upload step cleanly (`No files were found... No artifacts will be uploaded.` in the log, not a failure).

### 20. Compose UI Testing API Surface Isn't Fully Stable Across Compose BOM Versions — Prefer the Lower-Level Primitives
* **Symptom**: `Unresolved reference 'assertDoesNotExist'` (or similarly, another `androidx.compose.ui.test.*` convenience extension) at compile time, despite `testImplementation(libs.androidx.compose.ui.test.junit4)` being present and other Compose UI Testing calls in the same file resolving fine.
* **Root Cause**: Some convenience extension functions in the Compose UI Testing API have moved, been renamed, or aren't present in every BOM version's resolved artifact set. The BOM guarantees *compatible* versions across Compose artifacts, not that every convenience function is available everywhere it's used elsewhere in the ecosystem/documentation.
* **Permanent Fix**: For "asserts nothing matches" checks, use the lower-level, long-stable primitive instead of the convenience wrapper: `composeTestRule.onAllNodesWithTag("...").fetchSemanticsNodes(atLeastOneRootRequired = false).isEmpty()` rather than `onNodeWithTag("...").assertDoesNotExist()`.
* **Verification**: `./gradlew :app:testDebugUnitTest` (or equivalent) compiles and the assertion behaves correctly for both the present and absent case.

### 21. Roborazzi's `captureRoboImage()` Captures Unconditionally by Default — It Isn't a Gate Until You Turn On Verify Mode
* **Symptom**: A newly-added Roborazzi screenshot test always "passes" in CI, even when the rendered output has clearly changed or is wrong — it feels like the test isn't doing anything.
* **Root Cause**: Without `-Proborazzi.test.record=true` or `-Proborazzi.test.verify=true` (as a Gradle property, either on the command line or in `gradle.properties`), `captureRoboImage()` just writes the current render to `build/outputs/roborazzi/` every run — no comparison against a previous baseline happens, so there's nothing to fail on.
* **Permanent Fix**: This is a two-step rollout, not a one-shot add: (1) add the test and let it capture for a while / review the images manually or via the uploaded CI artifact; (2) once satisfied, run `./gradlew :app:recordRoborazziDebug` locally to commit baseline goldens (default `app/build/outputs/roborazzi/`, can be relocated via the `roborazzi { outputDir.set(...) }` DSL), then add `-Proborazzi.test.verify=true` to the CI test step so future runs actually fail on unintended visual drift.
* **Verification**: After recording goldens and enabling verify mode, a deliberate visual change (e.g. a padding tweak) makes the CI run fail with a `[original]_compare.png` diff artifact — confirming the gate is live, not just capturing.

