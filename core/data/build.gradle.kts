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

    testImplementation(project(":core:testing"))
    testImplementation(testFixtures(project(":core:database")))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
