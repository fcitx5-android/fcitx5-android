package org.fcitx.fcitx5.android.utils

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import org.fcitx.fcitx5.android.FcitxApplication
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.UniqueComponentWrapper

fun tracer(name: String) = lazy { FcitxApplication.getInstance().openTelemetry.getTracer(name) }

fun <T : Any> T.tStr() = when (this) {
    is UniqueComponentWrapper<*> -> type.qualifiedName ?: type.toString()
    is UniqueComponent<*> -> type.qualifiedName ?: type.toString()
    else -> toString()
}

inline fun <T> Tracer.withSpan(s: String, block: Span.() -> T): T {
    val span = spanBuilder(s).startSpan()
    val scope = span.makeCurrent()
    return try {
        span.block()
    } catch (e: Exception) {
        span.setStatus(StatusCode.ERROR, e.message ?: e.toString())
        throw e
    } finally {
        scope.close()
        span.end()
    }
}
