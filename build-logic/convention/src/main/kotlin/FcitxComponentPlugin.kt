import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Add task installFcitxConfig and installFcitxTranslation, using a random variant's cxx dir
 */
class FcitxComponentPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            /**
             * Note *Graph*
             * Installing fcitx components depends .cxx dir.
             * Since the native task `buildCMake$Variant$ABI` depend on the current variant and ABI,
             * we should have registered installFcitxComponent tasks for the cartesian product of $Variant and $ABI, e.g. installFcitxComponentDebug\[x86\]
             * However, this would be way more tedious, as the build variant and ABI actually do not affect components we are going to install.
             * The essential cause of this situation is that it's impossible for gradle to handle dynamic dependencies,
             * where we cannot add dependency when running a task. So a trick is used here: when the task graph
             * is evaluated, we look into it to find out the name of the native task which will be executed, and then store its output
             * path in global variable. This results in our tasks can not be executed directly without executing the dependent of the native task,
             * i.e. they are implicitly depending on the native task.
             */
            var cmakeDir: File? = null
            gradle.taskGraph.whenReady {
                val buildCMakeABITask = allTasks
                    .find { it.name.startsWith("buildCMakeDebug[") || it.name.startsWith("buildCMakeRelWithDebInfo[") }
                if (buildCMakeABITask != null) {
                    cmakeDir = buildCMakeABITask.outputs.files.first().parentFile
                }
            }
            val all = tasks.register("installFcitxComponent")
            fun regTask(target: String, component: String) {
                tasks.register("installFcitx${component.capitalized()}") {
                    /**
                     * Important: make sure that this task runs after than the native task
                     * Since we can't declare the dependency relationship, a weaker running order constraint must be enforced
                     * See the note above
                     */
                    mustRunAfter("buildCMakeDebug[$buildABI]")
                    mustRunAfter("buildCMakeRelWithDebInfo[$buildABI]")

                    doLast {
                        val dir = cmakeDir
                            ?: error("Cannot find cmake dir. Did you run this task independently?")
                        exec {
                            workingDir = dir
                            commandLine(
                                "cmake",
                                "--build",
                                ".",
                                "--target",
                                target
                            )
                        }
                        exec {
                            workingDir = dir
                            environment("DESTDIR", file("src/main/assets").absolutePath)
                            commandLine("cmake", "--install", ".", "--component", component)
                        }
                    }
                }.also { all.dependsOn(it) }
            }
            regTask("generate-desktop-file", "config")
            regTask("translation-file", "translation")
            tasks.register<Delete>("cleanFcitxComponents") {
                val locale = file("src/main/assets/usr/share/locale")
                // delete all non symlink dirs
                val fcitx = file("src/main/assets/usr/share/fcitx5").listFiles()?.filter {
                    // https://stackoverflow.com/questions/813710/java-1-6-determine-symbolic-links
                    File(it.parentFile.canonicalFile, it.name).let { s ->
                        s.canonicalFile == s.absoluteFile
                    }
                }
                delete(locale, fcitx)
            }.also { tasks.named("clean").dependsOn(it) }
        }
    }
}