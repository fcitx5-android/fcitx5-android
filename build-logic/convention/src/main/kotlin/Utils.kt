import kotlinx.serialization.json.Json
import org.gradle.api.Project
import java.io.ByteArrayOutputStream

fun envOrDefault(env: String, default: () -> String) =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: default()

fun Project.propertyOrDefault(prop: String, default: () -> String) =
    runCatching { property(prop)!!.toString() }.getOrElse { default() }

fun Project.runCmd(cmd: String): String = ByteArrayOutputStream().use {
    project.exec {
        commandLine = cmd.split(" ")
        standardOutput = it
    }
    it.toString().trim()
}

val json = Json { prettyPrint = true }
