plugins {
    alias(libs.plugins.spliteasy.android.feature)
}

android {
    namespace = "com.split.easy.feature.auth"
}

// Sign in with Google through Android's Credential Manager (FR-1, design 01).
dependencies {
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}
