import Versions.cmakeVersion
import Versions.ndkVersion
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Add cmake configuration.
 * This should be used with [AndroidConventionPlugin] if there is native code in the project.
 */
class AppNativeConventionPlugin : NativeConventionPlugin() {

    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")
        @Suppress("UnstableApiUsage")
        target.extensions.configure<ApplicationExtension> {
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
                    include(target.buildABI)
                    isUniversalApk = false
                }
            }
            defaultConfig {
                externalNativeBuild {
                    cmake {
                        arguments("-DANDROID_STL=c++_shared")
                    }
                }
            }
            externalNativeBuild {
                cmake {
                    version = target.cmakeVersion
                    path("src/main/cpp/CMakeLists.txt")
                }
            }
        }
        registerCleanCxxTask(target)
    }

}
