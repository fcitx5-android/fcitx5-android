import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

/**
 * The prototype of an Android Application
 *
 * * Configure dependency for [BuildMetadataPlugin] task and [DataDescriptorPlugin]  task (If have)
 * * Provide default configuration for `android {...}`
 * * Add desugar JDK libs
 */
class AndroidConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {

            // Apply android and kotlin plugins
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
            }

            val androidComponents =
                project.extensions.getByType(AndroidComponentsExtension::class.java)

            // Add dependency relationships for data descriptor task and metadata task if have
            androidComponents.onVariants { v ->
                // Evaluation should be delayed as we need be able to see other tasks
                afterEvaluate {
                    val generateDataDescriptor =
                        tasks.findByName("generateDataDescriptor")
                    val generateBuildMetadata =
                        tasks.findByName("generateBuildMetadata")
                    val variantName = v.name.capitalized()
                    tasks.getByName("merge${variantName}Assets")
                        .dependsOn(generateDataDescriptor)
                    tasks.getByName("lintAnalyze${variantName}").dependsOn(generateDataDescriptor)
                    tasks.getByName("lintReport${variantName}").dependsOn(generateDataDescriptor)
                    tasks.getByName("lintVitalAnalyzeRelease").dependsOn(generateDataDescriptor)
                    tasks.getByName("assemble${variantName}").dependsOn(generateBuildMetadata)
                }

            }

            // Make data descriptor depend on fcitx component if have
            // Since we are using finalizeDsl, there is no need to do afterEvaluate
            androidComponents.finalizeDsl {
                tasks.findByName("installFcitxComponent")?.let {
                    tasks.findByName("generateDataDescriptor")?.dependsOn(it)
                }
            }

            with(Versions) {
                extensions.configure<ApplicationExtension> {
                    compileSdk = compileSdkVersion
                    buildToolsVersion = Versions.buildToolsVersion
                    defaultConfig {
                        minSdk = minSdkVersion
                        targetSdk = targetSdkVersion
                        versionCode = calculateVersionCode(buildABI)
                        versionName = buildVersionName
                        setProperty("archivesBaseName", "$applicationId-$buildVersionName")
                    }
                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_11
                        targetCompatibility = JavaVersion.VERSION_11
                        isCoreLibraryDesugaringEnabled = true
                    }
                    (this as ExtensionAware).extensions.configure<KotlinJvmOptions>("kotlinOptions") {
                        jvmTarget = JavaVersion.VERSION_11.toString()
                    }
                    kotlinExtension.apply {
                        sourceSets.all {
                            languageSettings.optIn("kotlin.RequiresOptIn")
                        }
                    }
                }
            }
            dependencies {
                add(
                    "coreLibraryDesugaring",
                    versionCatalog.findLibrary("android.desugarJDKLibs").get()
                )
            }
        }
    }
}