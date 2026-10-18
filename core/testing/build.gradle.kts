plugins {
    alias(libs.plugins.spliteasy.jvm.library)
}

// Test helpers shared by every module's tests: fake repositories, fixtures, the Main dispatcher rule.
dependencies {
    api(project(":core:domain"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
