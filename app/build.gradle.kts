plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.split.easy"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.split.easy"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Errors fail the build; warnings are reported but don't block it.
        abortOnError = true
        warningsAsErrors = false
    }
}

room {
    // Exported schemas are committed so future migrations can be written and tested.
    schemaDirectory("$projectDir/schemas")
}

ktlint {
    version.set(libs.versions.ktlint)
    android.set(true)
}

kover {
    reports {
        filters {
            includes {
                // PRD metric: split and settlement logic at 90% coverage or higher.
                packages(
                    "com.split.easy.domain.money",
                    "com.split.easy.domain.split",
                    "com.split.easy.domain.balance",
                    "com.split.easy.domain.settlement",
                )
            }
        }
        verify {
            rule("Split and settlement logic") {
                minBound(90)
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
}
