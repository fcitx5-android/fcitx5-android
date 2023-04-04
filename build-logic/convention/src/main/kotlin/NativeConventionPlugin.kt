import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

/**
 * Add cmake configuration.
 * This should be used with [AndroidConventionPlugin] if there is native code in the project.
 */
class NativeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            with(Versions) {
                extensions.configure<ApplicationExtension> {
                    ndkVersion = target.ndkVersion
                    packagingOptions {
                        jniLibs {
                            useLegacyPackaging = true
                        }
                    }
                    splits {
                        abi {
                            isEnable = true
                            reset()
                            include(buildABI)
                            isUniversalApk = false
                        }
                    }
                    externalNativeBuild {
                        cmake {
                            version = cmakeVersion
                            path("src/main/cpp/CMakeLists.txt")
                        }
                    }
                }
            }
            tasks.register<Delete>("cleanCxxIntermediates") {
                delete(file(".cxx"))
            }.also { tasks.named("clean").dependsOn(it) }
        }
    }
}