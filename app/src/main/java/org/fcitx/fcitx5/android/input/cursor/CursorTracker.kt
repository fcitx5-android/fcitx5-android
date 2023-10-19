package org.fcitx.fcitx5.android.input.cursor

import timber.log.Timber
import java.util.ArrayDeque

class CursorTracker {

    val current = CursorRange()

    private val predictions = ArrayDeque<CursorRange>(16)

    val latest: CursorRange
        get() = predictions.peekLast() ?: current

    fun resetTo(start: Int, end: Int = start) {
        predictions.clear()
        current.update(start, end)
    }

    fun predict(new: CursorRange) {
        if (!latest.rangeEquals(new)) {
            predictions.add(new)
        }
        Timber.d("current: $current; predicted: ${predictions.joinToString()}")
    }

    fun predict(start: Int, end: Int = start) {
        predict(CursorRange(start, end))
    }

    fun predictOffset(offsetStart: Int, offsetEnd: Int = offsetStart) {
        predict(CursorRange(latest.start + offsetStart, latest.end + offsetEnd))
    }

    fun consume(start: Int, end: Int = start): Boolean {
        if (current.rangeEquals(start, end)) {
            return true
        }
        var matched = false
        while (predictions.isNotEmpty()) {
            if (predictions.removeFirst().rangeEquals(start, end)) {
                matched = true
                break
            }
        }
        current.update(start, end)
        if (!matched) {
            Timber.d("unable to consume [$start,$end]")
        }
        return matched
    }
}
