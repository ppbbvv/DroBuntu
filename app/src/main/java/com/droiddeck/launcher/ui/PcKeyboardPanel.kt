package com.droiddeck.launcher.ui

import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.droiddeck.launcher.input.ORTHO_COLUMNS
import com.droiddeck.launcher.input.ORTHO_LEFT
import com.droiddeck.launcher.input.ORTHO_RIGHT
import com.droiddeck.launcher.input.OrthoSlot
import com.droiddeck.launcher.input.PadTouchStream
import com.droiddeck.launcher.input.PcKey

internal const val PC_KEYBOARD_PAD_TAG = "pc-keyboard-pad"

private const val PAD_COLUMNS = 4f

@Composable
fun PcKeyboardPanel(
    sendKey: (Int, Boolean) -> Unit,
    padTouch: (MotionEvent, Int) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val send by rememberUpdatedState(sendKey)
    val board = remember { OrthoKeyboard { code, down -> send(code, down) } }
    DisposableEffect(Unit) { onDispose { board.releaseAll() } }
    Row(
        modifier.background(Color(0xFF101418))
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume(); waitForUpOrCancellation()?.consume() } }
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        KeyHalf(ORTHO_LEFT, board, Modifier.weight(ORTHO_COLUMNS).fillMaxHeight())
        PanelTouchpad(padTouch, Modifier.weight(PAD_COLUMNS).fillMaxHeight())
        KeyHalf(ORTHO_RIGHT, board, Modifier.weight(ORTHO_COLUMNS).fillMaxHeight())
    }
}

@Composable
private fun KeyHalf(rows: List<List<OrthoSlot>>, board: OrthoKeyboard, modifier: Modifier) {
    Column(modifier) {
        for (row in rows) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (slot in row) {
                    key(slot) {
                        val shown = board.shown(slot)
                        val sticky = board.sticky(slot)
                        val layered = shown !== slot.base
                        KeyCap(
                            legend(shown, board.shifted, board.capsOn), slot.base.weight,
                            lit = sticky != STICKY_OFF || layered, locked = sticky == STICKY_LOCKED,
                            modifier = Modifier.padding(2.dp),
                            onDown = { board.press(slot) }, onUp = { board.release(slot) },
                            large = true, hint = slot.fn?.takeUnless { layered }?.label,
                        )
                    }
                }
            }
        }
    }
}

private fun legend(key: PcKey, shifted: Boolean, capsOn: Boolean): String {
    val upper = key.shifted
    return when {
        shifted && upper != null -> upper
        key.label.length == 1 && key.label[0].isLetter() && (shifted xor capsOn) -> key.label.uppercase()
        else -> key.label
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PanelTouchpad(onTouch: (MotionEvent, Int) -> Boolean, modifier: Modifier) {
    val touch by rememberUpdatedState(onTouch)
    val width = remember { intArrayOf(0) }
    val stream = remember { PadTouchStream { event -> touch(event, width[0]) } }
    DisposableEffect(Unit) { onDispose { stream.cancel(SystemClock.uptimeMillis()) } }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier.padding(2.dp).testTag(PC_KEYBOARD_PAD_TAG)
            .background(Color.White.copy(alpha = 0.04f), shape)
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val time = event.changes.maxOfOrNull { it.uptimeMillis } ?: continue
                        width[0] = size.width
                        if (event.changes.any { it.previousPressed && !it.pressed && it.isConsumed }) {
                            stream.cancel(time)
                            continue
                        }
                        stream.frame(time, event.changes.map { change ->
                            PadTouchStream.Touch(
                                change.id.value, change.position.x, change.position.y, change.pressed,
                                change.historical.map { PadTouchStream.Sample(it.uptimeMillis, it.position.x, it.position.y) },
                            )
                        })
                        event.changes.forEach { it.consume() }
                    }
                }
            },
    )
}
