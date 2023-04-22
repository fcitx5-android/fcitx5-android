import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

open class AndroidBaseConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.android")

        @Suppress("UnstableApiUsage")
        target.extensions.configure<CommonExtension<*, *, *, *>>("android") {
            compileSdk = Versions.compileSdk
            buildToolsVersion = Versions.buildTools
            defaultConfig {
                minSdk = Versions.minSdk
            }
            compileOptions {
                sourceCompatibility = Versions.java
                targetCompatibility = Versions.java
            }
        }

        // https://youtrack.jetbrains.com/issue/KT-55947
        target.tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = Versions.java.toString()
        }

        target.extensions.configure<KotlinProjectExtension> {
            sourceSets.all {
                languageSettings.optIn("kotlin.RequiresOptIn")
            }
        }
    }

}
