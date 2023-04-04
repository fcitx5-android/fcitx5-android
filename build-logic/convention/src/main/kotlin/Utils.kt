import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.getByType
import java.io.ByteArrayOutputStream

inline fun envOrDefault(env: String, default: () -> String) =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: default()

inline fun Project.propertyOrDefault(prop: String, default: () -> String) =
    runCatching { property(prop)!!.toString() }.getOrElse {
        default()
    }

inline fun Project.extOrDefault(name: String, default: () -> String) =
    runCatching { extra.get(name) as String }.getOrElse { default().also { extra.set(name, it) } }

fun Project.runCmd(cmd: String): String = ByteArrayOutputStream().use {
    project.exec {
        commandLine = cmd.split(" ")
        standardOutput = it
    }
    it.toString().trim()
}

val json = Json { prettyPrint = true }

internal inline fun Project.eep(name: String, envName: String, block: () -> String) =
    extOrDefault(name) {
        envOrDefault(envName) {
            propertyOrDefault(envName) {
                block()
            }
        }
    }

val Project.versionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")