package com.droiddeck.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.droiddeck.launcher.input.ORTHO_FN
import com.droiddeck.launcher.input.OrthoSlot
import com.droiddeck.launcher.input.PC_MODIFIERS
import com.droiddeck.launcher.input.PcKey

internal const val STICKY_OFF = 0
internal const val STICKY_ONCE = 1
internal const val STICKY_LOCKED = 2

private const val KEY_CAPS = 58
private const val KEY_LEFTSHIFT = 42
private const val KEY_RIGHTSHIFT = 54

internal class OrthoKeyboard(private val sendKey: (Int, Boolean) -> Unit) {
    private val mods = mutableStateMapOf<Int, Int>()
    private val held = HashMap<OrthoSlot, Int>()

    var fn by mutableStateOf(STICKY_OFF)
        private set
    var capsOn by mutableStateOf(false)
        private set

    val shifted: Boolean get() = sticky(KEY_LEFTSHIFT) != STICKY_OFF || sticky(KEY_RIGHTSHIFT) != STICKY_OFF

    fun shown(slot: OrthoSlot): PcKey = if (fn != STICKY_OFF) slot.fn ?: slot.base else slot.base

    fun sticky(slot: OrthoSlot): Int = if (slot.base.code == ORTHO_FN) fn else sticky(shown(slot).code)

    private fun sticky(code: Int): Int = mods[code] ?: STICKY_OFF

    fun press(slot: OrthoSlot) {
        if (slot.base.code == ORTHO_FN) {
            fn = (fn + 1) % 3
            return
        }
        val key = shown(slot)
        if (key.code in PC_MODIFIERS) {
            when (sticky(key.code)) {
                STICKY_OFF -> { sendKey(key.code, true); mods[key.code] = STICKY_ONCE }
                STICKY_ONCE -> mods[key.code] = STICKY_LOCKED
                else -> { sendKey(key.code, false); mods[key.code] = STICKY_OFF }
            }
            return
        }
        held.remove(slot)?.let { sendKey(it, false) }
        sendKey(key.code, true)
        held[slot] = key.code
    }

    fun release(slot: OrthoSlot) {
        val code = held.remove(slot) ?: return
        sendKey(code, false)
        if (code == KEY_CAPS) capsOn = !capsOn
        for ((modifier, state) in mods.toMap()) if (state == STICKY_ONCE) { sendKey(modifier, false); mods[modifier] = STICKY_OFF }
        if (fn == STICKY_ONCE) fn = STICKY_OFF
    }

    fun releaseAll() {
        for (code in held.values) sendKey(code, false)
        held.clear()
        for ((modifier, state) in mods.toMap()) if (state != STICKY_OFF) sendKey(modifier, false)
        mods.clear()
        fn = STICKY_OFF
    }
}
