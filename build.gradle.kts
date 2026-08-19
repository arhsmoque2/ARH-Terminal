plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover)
    alias(libs.plugins.spotless) apply false
}

dependencies {
    kover(project(":core:core-ssh"))
    kover(project(":core:core-tmux"))
    kover(project(":core:core-agents"))
    kover(project(":core:core-mcp"))
    kover(project(":core:core-relay"))
    kover(project(":app"))
}
