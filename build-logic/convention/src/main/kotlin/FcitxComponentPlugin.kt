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
            val all = tasks.register("installFcitxComponent")
            fun regTask(target: String, component: String) {
                tasks.register("installFcitx${component.capitalized()}") {
                    doLast {
                        /**
                         * Tasks registered implicitly depend .cxx dir to install generated files.
                         * Since the native task `buildCMake$Variant$ABI` depend on the current variant and ABI,
                         * we should have registered [installFcitxComponent] tasks for the cartesian product of $Variant and $ABI.
                         * However, this would be way more tedious, as the build variant and ABI do not affect components we are going to install.
                         * The essential cause of this situation is that it's impossible for gradle to handle dynamic dependencies,
                         * where once the build graph was evaluated, no dependencies can be changed. So we delay the process of obtaining the output
                         * of cmake to the evaluation of theses tasks, which means that installFcitxComponent can not be run independently.
                         */
                        val cmakeDir = tasks.find {
                            it.name.startsWith("buildCMakeDebug[") || it.name.startsWith("buildCMakeRelWithDebInfo[")
                        }?.outputs?.files?.firstOrNull()?.parentFile?.takeIf { it.isDirectory }
                            ?: error("Cannot find cmake dir. Did you run this task independently?")
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
                }.also { all.dependsOn(it) }
            }
            regTask("generate-desktop-file", "config")
            regTask("translation-file", "translation")
            tasks.register<Delete>("cleanFcitxComponents") {
                delete(file("src/main/assets/usr/share/locale"))
                // delete all non symlink dirs
                delete(file("src/main/assets/usr/share/fcitx5").listFiles()?.filter {
                    // https://stackoverflow.com/questions/813710/java-1-6-determine-symbolic-links
                    File(it.parentFile.canonicalFile, it.name).let { s ->
                        s.canonicalFile == s.absoluteFile
                    }
                })
                delete(file("src/main/assets/${dataDescriptorName}"))
            }.also { tasks.named("clean").dependsOn(it) }
        }
    }
}