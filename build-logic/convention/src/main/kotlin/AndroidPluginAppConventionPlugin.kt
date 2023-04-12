import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.kotlin.dsl.configure

/**
 * Register `assemble${Variant}Plugins` task for root project,
 * and make all plugins' `assemble${Variant}` depends on it
 */
class AndroidPluginAppConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.extensions.configure<BaseAppModuleExtension> {
            applicationVariants.all {
                val pluginsTaskName = "assemble${name.capitalized()}Plugins"
                val pluginsTask = target.rootProject.tasks.findByName(pluginsTaskName)
                    ?: target.rootProject.task(pluginsTaskName)
                pluginsTask.dependsOn(assembleProvider)
            }
        }
    }

}
