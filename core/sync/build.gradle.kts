plugins {
    alias(libs.plugins.spliteasy.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.split.easy.core.sync"
}

// Moves data between Room and Firestore (FR-10). Upload and download arrive in the next steps.
dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.kotlinx.coroutines.test)
}
