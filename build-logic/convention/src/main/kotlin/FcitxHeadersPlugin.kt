import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.task

class FcitxHeadersPlugin : Plugin<Project> {

    companion object {
        const val INSTALL_TASK = "installFcitxHeaders"
        const val CLEAN_TASK = "cleanFcitxHeaders"
    }

    private val Project.headersInstallDir
        get() = file("build/headers")

    override fun apply(target: Project) {
        target.pluginManager.apply("cmake-dir")
        registerInstallTask(target)
        registerCleanTask(target)
    }

    private fun registerInstallTask(project: Project) {
        val installHeadersTask = project.task(INSTALL_TASK) {
            runAfterNativeConfigure(project)

            doLast {
                project.exec {
                    workingDir = project.cmakeDir
                    environment("DESTDIR", project.headersInstallDir.absolutePath)
                    commandLine("cmake", "--install", ".", "--component", "header")
                }
            }
        }

        val androidComponents =
            project.extensions.getByType(AndroidComponentsExtension::class.java)

        androidComponents.onVariants { v ->
            val variantName = v.name.capitalized()
            project.afterEvaluate {
                project.tasks.findByName("prefab${variantName}ConfigurePackage")
                    ?.dependsOn(installHeadersTask)
            }
        }

        val libraryAndroidComponents =
            project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)

        @Suppress("UnstableApiUsage")
        libraryAndroidComponents.finalizeDsl {
            it.prefab.forEach { library ->
                library.headers?.let { path -> project.file(path).mkdirs() }
            }
        }
    }

    private fun registerCleanTask(project: Project) {
        val cleanTask = project.tasks.getByName("clean")
        project.task<Delete>(CLEAN_TASK) {
            delete(project.headersInstallDir)
        }.also {
            cleanTask.dependsOn(it)
        }
    }

}
