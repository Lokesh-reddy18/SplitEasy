import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** An Android library module with the shared SDK, Java and test settings, plus ktlint. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        extensions.configure<LibraryExtension> {
            configureAndroid()
        }
        dependencies {
            add("testImplementation", libs.lib("junit"))
        }
    }
}
