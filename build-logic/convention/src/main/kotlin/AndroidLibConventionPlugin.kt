import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibConventionPlugin : AndroidBaseConventionPlugin() {

    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.library")

        super.apply(target)

        @Suppress("UnstableApiUsage")
        target.extensions.configure<LibraryExtension> {
            buildTypes {
                release {
                    isMinifyEnabled = true
                }
            }
        }
    }

}
