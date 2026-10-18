plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.google.services) apply false
}

// ktlint settings shared by every module.
subprojects {
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            version.set(libs.versions.ktlint)
            android.set(true)
        }
    }
}
