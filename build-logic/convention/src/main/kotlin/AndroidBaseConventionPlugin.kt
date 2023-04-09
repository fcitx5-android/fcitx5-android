import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

open class AndroidBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.android")

        @Suppress("UnstableApiUsage")
        target.extensions.configure(CommonExtension::class.java) {
            compileSdk = Versions.compileSdkVersion
            buildToolsVersion = Versions.buildToolsVersion
            defaultConfig {
                minSdk = Versions.minSdkVersion
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }
            (this as ExtensionAware).extensions.configure<KotlinJvmOptions>("kotlinOptions") {
                jvmTarget = JavaVersion.VERSION_11.toString()
            }
        }

        target.extensions.configure<KotlinProjectExtension> {
            sourceSets.all {
                languageSettings.optIn("kotlin.RequiresOptIn")
            }
        }
    }

}
