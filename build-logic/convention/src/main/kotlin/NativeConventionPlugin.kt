import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.kotlin.dsl.register

abstract class NativeConventionPlugin : Plugin<Project> {

    protected fun registerCleanCxxTask(project: Project) {
        project.tasks.register<Delete>("cleanCxxIntermediates") {
            delete(project.file(".cxx"))
        }.also {
            project.tasks.named("clean").dependsOn(it)
        }
    }

}
