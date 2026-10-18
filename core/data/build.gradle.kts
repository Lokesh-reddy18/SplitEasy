plugins {
    alias(libs.plugins.spliteasy.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.split.easy.core.data"
}

// Repositories (Room first, then sync), preferences, the current user and demo data.
dependencies {
    api(project(":core:domain"))
    implementation(project(":core:database"))
    api(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Google sign-in (FR-1): Firebase Auth, plus Credential Manager to forget the account on sign-out.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":core:database")))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
