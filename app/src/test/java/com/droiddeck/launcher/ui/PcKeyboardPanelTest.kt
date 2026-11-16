package com.droiddeck.launcher.ui

import android.graphics.RectF
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import com.droiddeck.launcher.input.PointerGestures
import com.droiddeck.launcher.input.TouchpadGestures
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w640dp-h360dp-land", application = android.app.Application::class)
class PcKeyboardPanelTest {
    @get:Rule val compose = createRobolectricComposeRule()
    private val keys = mutableListOf<String>()
    private val pointer = mutableListOf<String>()
    private lateinit var touchpad: TouchpadGestures

    private val listener = object : PointerGestures.Listener {
        override fun onMove(x: Float, y: Float) { pointer += "move" }
        override fun onButton(button: Int, pressed: Boolean, x: Float, y: Float) {
            val name = when (button) { PointerGestures.BTN_RIGHT -> "right"; PointerGestures.BTN_MIDDLE -> "middle"; else -> "left" }
            pointer += "$name ${if (pressed) "down" else "up"}"
        }
        override fun onWheel(steps: Int) { pointer += "wheel" }
        override fun onLongPress() { pointer += "long press" }
        override fun onScroll(dx: Float, dy: Float) { pointer += "scroll" }
        override fun onScrollEnd() { pointer += "scroll end" }
        override fun onZoom(steps: Int) { pointer += "zoom" }
    }

    @Before fun setUp() {
        touchpad = TouchpadGestures(8f, listener)
        touchpad.desktop = true
        touchpad.bounds = RectF(0f, 0f, 1920f, 1080f)
        touchpad.place(960f, 540f)
        compose.setContent {
            DroidDeckTheme {
                PcKeyboardPanel(
                    { code, down -> keys += "$code ${if (down) "down" else "up"}" },
                    { event, _ -> touchpad.onTouch(event) },
                    Modifier.fillMaxSize(),
                )
            }
        }
        // A touch in the first moments after boot reads as the second tap of a double tap.
        compose.mainClock.advanceTimeBy(BOOTED_MS)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(BOOTED_MS))
        compose.waitForIdle()
    }

    @Test fun aKeySendsItsCode() {
        tap("q")
        assertEquals(listOf("16 down", "16 up"), keys)
    }

    @Test fun shiftCapitalisesOneLetter() {
        tap("⇧")
        compose.onNodeWithText("Q").assertExists()
        tap("Q")
        compose.onNodeWithText("q").assertExists()
        assertEquals(listOf("42 down", "16 down", "16 up", "42 up"), keys)
    }

    @Test fun fnShowsTheSecondLayerForOneKey() {
        compose.onNodeWithText("1").assertExists()
        tap("Fn")
        compose.onNodeWithText("1").assertDoesNotExist()
        tap("F1")
        compose.onNodeWithText("1").assertExists()
        assertEquals(listOf("59 down", "59 up"), keys)
    }

    @Test fun aTapOnThePadClicksTheLeftButton() {
        compose.onNodeWithTag(PC_KEYBOARD_PAD_TAG).performTouchInput {
            down(center)
            up()
        }
        settle()
        assertEquals(listOf("left down", "left up"), pointer)
        assertEquals(emptyList<String>(), keys)
    }

    @Test fun aTwoFingerTapOnThePadClicksTheRightButton() {
        compose.onNodeWithTag(PC_KEYBOARD_PAD_TAG).performTouchInput {
            down(0, center - Offset(20f, 0f))
            down(1, center + Offset(20f, 0f))
            up(1)
            up(0)
        }
        settle()
        assertEquals(listOf("right down", "right up"), pointer)
    }

    @Test fun twoFingersSlidingOnThePadScroll() {
        compose.onNodeWithTag(PC_KEYBOARD_PAD_TAG).performTouchInput {
            down(0, center - Offset(20f, 60f))
            down(1, center + Offset(20f, -60f))
            repeat(4) {
                updatePointerBy(0, Offset(0f, 25f))
                updatePointerBy(1, Offset(0f, 25f))
                move()
            }
            up(1)
            up(0)
        }
        settle()
        assertTrue(pointer.contains("scroll"))
        assertEquals("scroll end", pointer.last())
        assertFalse(pointer.any { it.startsWith("left") || it.startsWith("right") || it == "zoom" })
    }

    @Test fun oneFingerSlidingOnThePadMovesThePointer() {
        compose.onNodeWithTag(PC_KEYBOARD_PAD_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, 40f))
            moveBy(Offset(0f, 40f))
            up()
        }
        settle()
        assertTrue(pointer.isNotEmpty() && pointer.all { it == "move" })
    }

    @Test fun aFingerHeldOnAKeyIsNotASecondFingerOnThePad() {
        val key = compose.onNodeWithText("q").fetchSemanticsNode().boundsInRoot.center
        val pad = compose.onNodeWithTag(PC_KEYBOARD_PAD_TAG).fetchSemanticsNode().boundsInRoot.center
        compose.onRoot().performTouchInput {
            down(0, key)
            down(1, pad)
            up(1)
            up(0)
        }
        settle()
        assertEquals(listOf("left down", "left up"), pointer)
        assertEquals(listOf("16 down", "16 up"), keys)
    }

    private fun tap(label: String) {
        compose.onNodeWithText(label).performTouchInput {
            down(center)
            up()
        }
        compose.waitForIdle()
    }

    private fun settle() {
        compose.waitForIdle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
    }

    private companion object {
        const val BOOTED_MS = 2_000L
    }
}
