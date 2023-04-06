import Versions.cmakeVersion
import Versions.ndkVersion
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

open class LibNativeConventionPlugin : NativeConventionPlugin() {

    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.library")
        @Suppress("UnstableApiUsage")
        target.extensions.configure<LibraryExtension> {
            compileSdk = Versions.compileSdkVersion
            buildToolsVersion = Versions.buildToolsVersion
            defaultConfig {
                minSdk = Versions.minSdkVersion
                externalNativeBuild {
                    cmake {
                        arguments("-DANDROID_STL=c++_shared")
                    }
                }
            }
            ndkVersion = target.ndkVersion
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
            buildFeatures {
                prefabPublishing = true
            }
            packagingOptions {
                jniLibs {
                    excludes.add("**/*.so")
                }
            }
        }
        registerCleanCxxTask(target)
    }

}
