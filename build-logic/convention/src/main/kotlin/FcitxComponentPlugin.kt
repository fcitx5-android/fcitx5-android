import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.task
import kotlin.io.path.isSymbolicLink

/**
 * Add task installFcitxConfig and installFcitxTranslation, using a random variant's cxx dir
 */
class FcitxComponentPlugin : Plugin<Project> {

    abstract class FcitxComponentExtension {
        var installFcitx5Data: Boolean = false
    }

    companion object {
        const val INSTALL_TASK = "installFcitxComponent"
        const val CLEAN_TASK = "cleanFcitxComponents"
    }

    override fun apply(target: Project) {
        target.pluginManager.apply("org.fcitx.fcitx5.android.android-sdk-path")
        target.pluginManager.apply("org.fcitx.fcitx5.android.cmake-dir")
        registerCMakeTask(target, "generate-desktop-file", "config")
        registerCMakeTask(target, "translation-file", "translation")
        registerCleanTask(target)
        target.extensions.create<FcitxComponentExtension>("fcitxComponent")
        target.afterEvaluate {
            val ext = extensions.getByName<FcitxComponentExtension>("fcitxComponent")
            if (ext.installFcitx5Data) {
                val libFcitx5 = rootProject.project(":lib:fcitx5")
                registerCMakeTask(target, "generate-desktop-file", "config", libFcitx5)
                registerCMakeTask(target, "translation-file", "translation", libFcitx5)
            }
        }
    }

    /**
     * build [sourceProject]'s cmake [target], and install its [component] to [project]'s assets
     */
    private fun registerCMakeTask(
        project: Project,
        target: String,
        component: String,
        sourceProject: Project = project
    ) {
        val dependencyTask = project.tasks.findByName(INSTALL_TASK) ?: project.task(INSTALL_TASK)
        val taskName = if (project === sourceProject) "installProject" else "installFcitx5"
        project.task("${taskName}${component.capitalized()}") {
            runAfterNativeConfigure(sourceProject)

            doLast {
                project.exec {
                    workingDir = sourceProject.cmakeDir
                    commandLine(project.cmakeBinary, "--build", ".", "--target", target)
                }
                project.exec {
                    workingDir = sourceProject.cmakeDir
                    environment("DESTDIR", project.assetsDir.absolutePath)
                    commandLine(project.cmakeBinary, "--install", ".", "--component", component)
                }
            }
        }.also {
            dependencyTask.dependsOn(it)
        }
    }

    private fun registerCleanTask(project: Project) {
        project.task<Delete>(CLEAN_TASK) {
            delete(project.assetsDir.resolve("usr/share/locale"))
            // delete all non symlink dirs
            delete(project.assetsDir.resolve("usr/share/fcitx5").listFiles()?.filter {
                !it.toPath().isSymbolicLink()
            })
        }.also {
            project.cleanTask.dependsOn(it)
        }
    }

}
