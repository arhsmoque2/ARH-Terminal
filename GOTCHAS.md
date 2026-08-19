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

### 6. Release Signing Fails with `BadPaddingException` When Secrets Are Missing/Wrong
* **Symptom**: `:app:assembleRelease` fails deep in AGP packaging with `KeytoolException: ... keystore password was incorrect` / `javax.crypto.BadPaddingException: Given final block not properly padded`.
* **Root Cause**: `app/build.gradle.kts` previously fell back to a hardcoded placeholder password (`"arhterminal2026"`) whenever the `KEYSTORE_PASSWORD` / `KEY_PASSWORD` env vars weren't set. That placeholder doesn't match the real password on the committed `arh-release.jks`, so it silently swapped a clear "secret missing" error for a confusing low-level crypto exception.
* **Permanent Fix**: No hardcoded fallback password. `storePassword`/`keyPassword` are read only from env vars; a `gradle.taskGraph.whenReady` check fails fast with an explicit message when a `*Release` task runs without both set. Set `KEYSTORE_PASSWORD` and `KEY_PASSWORD` to the actual `arh-release.jks` credentials — as local env vars for a local release build, or as GitHub Actions repo secrets for CI (`Settings → Secrets and variables → Actions`).
* **Verification**: `./gradlew :app:assembleDebug` (and all non-release tasks) are unaffected; `./gradlew :app:assembleRelease` without the env vars now fails immediately with the actionable message instead of the keystore stack trace.
