plugins {
    alias(libs.plugins.spliteasy.jvm.library)
    alias(libs.plugins.kover)
}

// Pure Kotlin: models, split / balance / settlement / UPI logic and repository interfaces.
dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.javax.inject)
    testImplementation(project(":core:testing"))
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
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
                    "com.split.easy.domain.upi",
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
