import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized

/**
 * Add task installFcitxConfig and installFcitxTranslation, using a random variant's cxx dir
 */
class FcitxComponentPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            fun regTask(target: String, component: String) {
                val newTaskName = "installFcitx${component.capitalized()}"
                if (newTaskName in tasks.names)
                    return
                tasks.register("installFcitx${component.capitalized()}") {
                    doLast {
                        // FIXME: this is dirty
                        val buildCmakeTask = tasks.find {
                            it.name.startsWith("buildCMakeDebug[") || it.name.startsWith("buildCMakeRelWithDebInfo[")
                        }!!
                        val cmakeDir = buildCmakeTask.outputs.files.first().parentFile
                        exec {
                            workingDir = cmakeDir
                            commandLine(
                                "cmake",
                                "--build",
                                ".",
                                "--target",
                                target
                            )
                        }
                        exec {
                            workingDir = cmakeDir
                            environment("DESTDIR", file("src/main/assets").absolutePath)
                            commandLine("cmake", "--install", ".", "--component", component)
                        }
                    }
                }
            }
            regTask("generate-desktop-file", "config")
            regTask("translation-file", "translation")
        }
    }
}