import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.mikepenz.aboutlibraries.plugin.AboutLibrariesExtension
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure

/**
 * The prototype of an Android Application
 *
 * - Configure dependency for [DataDescriptorPlugin] task (If have)
 * - Provide default configuration for `android {...}`
 * - Add desugar JDK libs
 */
class AndroidAppConventionPlugin : AndroidBaseConventionPlugin() {

    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")

        super.apply(target)

        target.extensions.configure<BaseAppModuleExtension> {
            defaultConfig {
                targetSdk = Versions.targetSdk
                versionCode = Versions.calculateVersionCode(target.buildABI)
                versionName = target.buildVersionName
            }
            buildTypes {
                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                }
                debug {
                    applicationIdSuffix = ".debug"
                }
            }
            compileOptions {
                isCoreLibraryDesugaringEnabled = true
            }
        }

        target.extensions.configure<ApplicationAndroidComponentsExtension> {
            // Add dependency relationships for data descriptor task
            onVariants { v ->
                val variantName = v.name.capitalized()
                // Evaluation should be delayed as we need be able to see other tasks
                target.afterEvaluate {
                    tasks.findByName(DataDescriptorPlugin.TASK)?.also {
                        tasks.getByName("merge${variantName}Assets").dependsOn(it)
                        tasks.getByName("lintAnalyze${variantName}").dependsOn(it)
                        tasks.getByName("lintReport${variantName}").dependsOn(it)
                        tasks.getByName("lintVitalAnalyzeRelease").dependsOn(it)
                    }
                }
            }
            // Make data descriptor depend on fcitx component if have
            // Since we are using finalizeDsl, there is no need to do afterEvaluate
            finalizeDsl {
                target.tasks.findByName(FcitxComponentPlugin.INSTALL_TASK)?.also { componentTask ->
                    target.tasks.findByName(DataDescriptorPlugin.TASK)?.dependsOn(componentTask)
                }
                // applicationId is not set upon apply
                it.defaultConfig {
                    target.setProperty("archivesBaseName", "$applicationId-$versionName")
                }
            }
        }

        runCatching {
            target.pluginManager.apply(
                target.versionCatalog.findPlugin("aboutlibraries").get().get().pluginId
            )
        }.onFailure {
            it.printStackTrace()
        }

        target.configure<AboutLibrariesExtension> {
            excludeFields = arrayOf(
                "generated", "developers", "organization", "scm", "funding", "content"
            )
            fetchRemoteLicense = false
            fetchRemoteFunding = false
            includePlatform = false
        }

        runCatching {
            target.dependencies.add(
                "coreLibraryDesugaring",
                target.versionCatalog.findLibrary("android.desugarJDKLibs").get()
            )
        }.onFailure {
            it.printStackTrace()
        }
    }

}
