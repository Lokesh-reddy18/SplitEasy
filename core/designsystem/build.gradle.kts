plugins {
    alias(libs.plugins.spliteasy.android.library)
    alias(libs.plugins.spliteasy.android.compose)
}

android {
    namespace = "com.split.easy.core.designsystem"
}

// Theme, shared components, formatting and the UPI ID sheet.
dependencies {
    api(project(":core:domain"))
}
