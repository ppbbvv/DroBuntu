package com.droiddeck.launcher.input

import android.graphics.RectF
import android.os.Looper
import android.view.MotionEvent
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class PadTouchStreamTest {
    private val gestures = mutableListOf<String>()
    private val actions = mutableListOf<String>()
    private val samples = mutableListOf<String>()
    private lateinit var touchpad: TouchpadGestures
    private lateinit var stream: PadTouchStream
    private var time = 1000L

    private val listener = object : PointerGestures.Listener {
        override fun onMove(x: Float, y: Float) { gestures += "move" }
        override fun onButton(button: Int, pressed: Boolean, x: Float, y: Float) {
            val name = when (button) { PointerGestures.BTN_RIGHT -> "right"; PointerGestures.BTN_MIDDLE -> "middle"; else -> "left" }
            gestures += "$name ${if (pressed) "down" else "up"}"
        }
        override fun onWheel(steps: Int) { gestures += "wheel" }
        override fun onLongPress() { gestures += "long press" }
        override fun onScroll(dx: Float, dy: Float) { gestures += "scroll" }
        override fun onScrollEnd() { gestures += "scroll end" }
        override fun onZoom(steps: Int) { gestures += "zoom" }
    }

    @Before fun setUp() {
        touchpad = TouchpadGestures(SLOP, listener)
        touchpad.desktop = true
        touchpad.bounds = RectF(0f, 0f, 1920f, 1080f)
        touchpad.place(960f, 540f)
        stream = PadTouchStream { event ->
            actions += "${event.actionMasked}/${event.actionIndex}/${event.pointerCount}"
            samples += (0 until event.historySize).map { event.getHistoricalX(0, it) }.plus(event.x).joinToString(" ")
            touchpad.onTouch(event)
        }
    }

    @Test fun aTapClicksTheLeftButton() {
        frame(down(7, 100f, 100f))
        advance(80)
        frame(up(7, 100f, 100f))
        advance(100)
        assertEquals(listOf("left down", "left up"), gestures)
        assertEquals(listOf(action(MotionEvent.ACTION_DOWN, 0, 1), action(MotionEvent.ACTION_UP, 0, 1)), actions)
    }

    @Test fun aTwoFingerTapClicksTheRightButton() {
        frame(down(7, 100f, 100f))
        advance(10)
        frame(down(7, 100f, 100f), down(9, 160f, 110f))
        advance(60)
        frame(down(7, 100f, 100f), up(9, 160f, 110f))
        advance(10)
        frame(up(7, 100f, 100f))
        advance(100)
        assertEquals(listOf("right down", "right up"), gestures)
        assertEquals(
            listOf(
                action(MotionEvent.ACTION_DOWN, 0, 1), action(MotionEvent.ACTION_POINTER_DOWN, 1, 2),
                action(MotionEvent.ACTION_POINTER_UP, 1, 2), action(MotionEvent.ACTION_UP, 0, 1),
            ),
            actions,
        )
    }

    @Test fun twoFingersSlidingScroll() {
        frame(down(7, 100f, 100f))
        advance(10)
        frame(down(7, 100f, 100f), down(9, 160f, 100f))
        for (step in 1..5) {
            advance(16)
            frame(down(7, 100f, 100f + step * 20f), down(9, 160f, 100f + step * 20f))
        }
        frame(down(7, 100f, 200f), up(9, 160f, 200f))
        frame(up(7, 100f, 200f))
        advance(100)
        assertTrue(gestures.contains("scroll"))
        assertEquals("scroll end", gestures.last())
        assertFalse(gestures.any { it.startsWith("right") || it.startsWith("left") || it == "zoom" || it == "wheel" })
    }

    @Test fun oneFingerSlidingMovesThePointer() {
        frame(down(7, 100f, 100f))
        advance(16)
        frame(down(7, 150f, 100f))
        advance(16)
        frame(up(7, 150f, 100f))
        advance(100)
        assertEquals(listOf("move"), gestures)
    }

    @Test fun samplesBetweenFramesArriveAsHistory() {
        frame(down(7, 100f, 100f))
        advance(16)
        val history = listOf(PadTouchStream.Sample(time - 12, 110f, 100f), PadTouchStream.Sample(time - 6, 120f, 100f))
        frame(PadTouchStream.Touch(7, 130f, 100f, true, history))
        assertEquals(listOf("100.0", "110.0 120.0 130.0"), samples)
    }

    @Test fun aFingerThatDidNotMoveSendsNothing() {
        frame(down(7, 100f, 100f))
        frame(down(7, 100f, 100f))
        frame(down(7, 100f, 100f))
        assertEquals(1, actions.size)
    }

    @Test fun theSecondFingerCarriesOnWhenTheFirstLifts() {
        frame(down(7, 100f, 100f))
        frame(down(7, 100f, 100f), down(9, 160f, 100f))
        advance(400)
        frame(up(7, 100f, 100f), down(9, 160f, 100f))
        advance(16)
        frame(down(9, 190f, 100f))
        frame(up(9, 190f, 100f))
        assertEquals(
            listOf(
                action(MotionEvent.ACTION_DOWN, 0, 1), action(MotionEvent.ACTION_POINTER_DOWN, 1, 2),
                action(MotionEvent.ACTION_POINTER_UP, 0, 2), action(MotionEvent.ACTION_MOVE, 0, 1), action(MotionEvent.ACTION_UP, 0, 1),
            ),
            actions,
        )
    }

    @Test fun aCancelledTouchClicksNothing() {
        frame(down(7, 100f, 100f))
        advance(40)
        stream.cancel(time)
        advance(700)
        assertEquals(emptyList<String>(), gestures)
        assertEquals(action(MotionEvent.ACTION_CANCEL, 0, 1), actions.last())
        frame(down(7, 100f, 100f))
        assertEquals(action(MotionEvent.ACTION_DOWN, 0, 1), actions.last())
    }

    @Test fun aFingerThatWasNeverDownIsIgnored() {
        frame(up(7, 100f, 100f))
        stream.cancel(time)
        assertEquals(emptyList<String>(), actions)
    }

    @Test fun aSmallPadIsMoreSensitive() {
        assertEquals(1.15f, PadTouchStream.sensitivity(1920f, 0), 0f)
        assertEquals(1.15f, PadTouchStream.sensitivity(800f, 1000), 0f)
        assertEquals(1.6f, PadTouchStream.sensitivity(1920f, 480), 0.001f)
        assertEquals(3f, PadTouchStream.sensitivity(3840f, 300), 0f)
    }

    private fun down(id: Long, x: Float, y: Float) = PadTouchStream.Touch(id, x, y, true)

    private fun up(id: Long, x: Float, y: Float) = PadTouchStream.Touch(id, x, y, false)

    private fun frame(vararg touches: PadTouchStream.Touch) = stream.frame(time, touches.toList())

    private fun action(action: Int, index: Int, pointers: Int) = "$action/$index/$pointers"

    private fun advance(ms: Long) {
        time += ms
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms))
    }

    private companion object {
        const val SLOP = 8f
    }
}
