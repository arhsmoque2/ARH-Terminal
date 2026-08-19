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
