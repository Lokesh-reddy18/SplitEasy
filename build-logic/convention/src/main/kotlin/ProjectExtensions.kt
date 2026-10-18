import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** The shared version catalog (gradle/libs.versions.toml), for use inside convention plugins. */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()

internal const val COMPILE_SDK = 36
internal const val MIN_SDK = 26
internal val JAVA_VERSION = JavaVersion.VERSION_17

/** Settings every Android module shares: SDK levels, Java 17 and Robolectric-ready unit tests. */
internal fun CommonExtension.configureAndroid() {
    compileSdk = COMPILE_SDK
    defaultConfig.minSdk = MIN_SDK
    compileOptions.sourceCompatibility = JAVA_VERSION
    compileOptions.targetCompatibility = JAVA_VERSION
    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.all {
        // Robolectric reads JDK internals for file descriptors (robolectric/robolectric#11434).
        it.jvmArgs("--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED")
    }
    lint.abortOnError = true
    lint.warningsAsErrors = false
}
