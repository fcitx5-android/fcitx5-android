import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class NativeAppConventionPlugin : NativeBaseConventionPlugin() {

    override fun apply(target: Project) {
        super.apply(target)

        target.pluginManager.apply("com.android.application")

        @Suppress("UnstableApiUsage")
        target.extensions.configure<ApplicationExtension> {
            packagingOptions {
                jniLibs {
                    useLegacyPackaging = true
                }
            }
            buildFeatures {
                prefab = true
            }
        }
    }

}
