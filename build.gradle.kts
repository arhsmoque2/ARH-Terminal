plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.spotless)
}

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "com.diffplug.spotless")

    detekt {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
    }

    dependencies {
        detektPlugins("io.nlopez.compose.rules:detekt:0.5.7")
    }

    spotless {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**")
            ktlint()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
    }
}
