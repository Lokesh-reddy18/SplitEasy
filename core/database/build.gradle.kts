plugins {
    alias(libs.plugins.spliteasy.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.split.easy.core.database"
    // DatabaseTest and the entity fixtures are shared with :core:data's repository tests.
    testFixtures.enable = true
    sourceSets {
        // MigrationTestHelper reads the exported schemas as assets; tests use the debug assets.
        getByName("debug").assets.srcDir("$projectDir/schemas")
    }
}

room {
    // Exported schemas are committed so future migrations can be written and tested.
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:domain"))
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testFixturesImplementation(libs.androidx.room.runtime)
    testFixturesImplementation(libs.robolectric)
    testFixturesImplementation(libs.androidx.test.core)
    testFixturesImplementation(libs.junit)
    testFixturesImplementation(project(":core:domain"))

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
