package com.droiddeck.launcher.input

import android.view.InputDevice
import android.view.MotionEvent

/**
 * Turns the pointers one panel area owns into a touch stream of its own. The window's stream also
 * carries fingers resting elsewhere on the panel, and a recogniser fed with it would count a held
 * key as a second finger on the pad.
 */
class PadTouchStream(private val sink: (MotionEvent) -> Unit) {
    class Sample(val time: Long, val x: Float, val y: Float)

    class Touch(val id: Long, val x: Float, val y: Float, val down: Boolean, val history: List<Sample> = emptyList())

    private class Finger(val key: Long, val id: Int, var x: Float, var y: Float)

    private val fingers = ArrayList<Finger>()
    private var downTime = 0L

    fun frame(time: Long, touches: List<Touch>) {
        val moved = touches.filter { touch -> touch.down && fingers.any { it.key == touch.id && (it.x != touch.x || it.y != touch.y) } }
        if (moved.isNotEmpty()) move(time, moved)
        for (touch in touches) {
            if (!touch.down || fingers.size >= MAX_FINGERS || fingers.any { it.key == touch.id }) continue
            fingers += Finger(touch.id, freeId(), touch.x, touch.y)
            if (fingers.size == 1) downTime = time
            send(time, if (fingers.size == 1) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_POINTER_DOWN, fingers.lastIndex)
        }
        for (touch in touches) {
            if (touch.down) continue
            val index = fingers.indexOfFirst { it.key == touch.id }
            if (index < 0) continue
            fingers[index].x = touch.x
            fingers[index].y = touch.y
            send(time, if (fingers.size == 1) MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_UP, index)
            fingers.removeAt(index)
        }
    }

    fun cancel(time: Long) {
        if (fingers.isEmpty()) return
        send(time, MotionEvent.ACTION_CANCEL, 0)
        fingers.clear()
    }

    private fun move(time: Long, moved: List<Touch>) {
        val steps = moved.minOf { it.history.size }
        var event: MotionEvent? = null
        for (step in 0..steps) {
            var at = time
            for (touch in moved) {
                val finger = fingers.first { it.key == touch.id }
                val sample = touch.history.getOrNull(touch.history.size - steps + step)
                finger.x = sample?.x ?: touch.x
                finger.y = sample?.y ?: touch.y
                if (sample != null) at = sample.time
            }
            val batch = event
            if (batch == null) event = obtain(at, MotionEvent.ACTION_MOVE, 0) else batch.addBatch(at, coords(), 0)
        }
        event?.let {
            sink(it)
            it.recycle()
        }
    }

    private fun send(time: Long, action: Int, index: Int) {
        val event = obtain(time, action, index)
        sink(event)
        event.recycle()
    }

    private fun obtain(time: Long, action: Int, index: Int): MotionEvent {
        val properties = Array(fingers.size) { i ->
            MotionEvent.PointerProperties().apply {
                id = fingers[i].id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        return MotionEvent.obtain(
            downTime, time, action or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT), fingers.size, properties, coords(),
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
        )
    }

    private fun coords() = Array(fingers.size) { i ->
        MotionEvent.PointerCoords().apply {
            x = fingers[i].x
            y = fingers[i].y
            pressure = 1f
            size = 1f
        }
    }

    private fun freeId(): Int {
        var id = 0
        while (fingers.any { it.id == id }) id++
        return id
    }

    companion object {
        private const val MAX_FINGERS = 5
        private const val REACH = 0.4f
        private const val SLOWEST = 1.15f
        private const val FASTEST = 3f

        /** A slow swipe across the pad carries the pointer [REACH] of the way across the output. */
        fun sensitivity(outputWidth: Float, padWidth: Int): Float =
            if (padWidth <= 0) SLOWEST else (REACH * outputWidth / padWidth).coerceIn(SLOWEST, FASTEST)
    }
}
