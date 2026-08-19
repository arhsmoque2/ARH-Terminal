plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.arh.terminal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arh.terminal"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("${rootDir}/arh-release.jks")
            // No hardcoded fallback password here on purpose (see GOTCHAS.md #6): a wrong
            // baked-in default doesn't make the build succeed, it just turns a missing-secret
            // error into a much more confusing keystore BadPaddingException later. Leaving
            // these null when the env vars are unset lets the taskGraph check below fail fast
            // with an actionable message instead.
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "arh-terminal"
            keyPassword = System.getenv("KEY_PASSWORD")
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    source.setFrom("src/main/java")
}

// Fail fast with an actionable message when a release-signing task actually runs without
// real credentials, instead of letting AGP fail deep inside keystore decryption. Scoped to
// the task graph so debug/test/detekt/lint runs are never affected by missing release secrets.
gradle.taskGraph.whenReady {
    val needsReleaseSigning = allTasks.any { task ->
        task.project == project && task.name.contains("Release")
    }
    if (needsReleaseSigning) {
        val missing = listOfNotNull(
            "KEYSTORE_PASSWORD".takeIf { System.getenv("KEYSTORE_PASSWORD").isNullOrBlank() },
            "KEY_PASSWORD".takeIf { System.getenv("KEY_PASSWORD").isNullOrBlank() }
        )
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing requires ${missing.joinToString(" and ")} to be set to the " +
                    "actual arh-release.jks credentials (env var, or repo secret in CI). " +
                    "There is intentionally no fallback default: a wrong hardcoded password " +
                    "used to fail here with a confusing keystore BadPaddingException instead " +
                    "of this message. See GOTCHAS.md #6."
            )
        }
    }
}

dependencies {
    implementation(project(":core:core-ssh"))
    implementation(project(":core:core-tmux"))
    implementation(project(":core:core-agents"))
    implementation(project(":core:core-mcp"))
    implementation(project(":core:core-relay"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Detekt rules
    detektPlugins("io.nlopez.compose.rules:detekt:0.5.7")

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.compose.ui.tooling)
}
