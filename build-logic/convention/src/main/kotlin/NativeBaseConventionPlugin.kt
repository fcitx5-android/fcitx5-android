import Versions.cmakeVersion
import Versions.ndkVersion
import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.task

open class NativeBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        @Suppress("UnstableApiUsage")
        target.extensions.configure<CommonExtension<*, *, *, *, *>>("android") {
            ndkVersion = target.ndkVersion
            defaultConfig {
                minSdk = Versions.minSdk
                externalNativeBuild {
                    cmake {
                        arguments(
                            "-DANDROID_STL=c++_shared",
                            "-DANDROID_USE_LEGACY_TOOLCHAIN_FILE=OFF"
                        )
                    }
                }
            }
            externalNativeBuild {
                cmake {
                    version = target.cmakeVersion
                    path("src/main/cpp/CMakeLists.txt")
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
        }
        registerCleanCxxTask(target)
    }

    private fun registerCleanCxxTask(project: Project) {
        project.task<Delete>("cleanCxxIntermediates") {
            delete(project.file(".cxx"))
        }.also {
            project.cleanTask.dependsOn(it)
        }
    }

}
