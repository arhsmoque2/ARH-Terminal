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
