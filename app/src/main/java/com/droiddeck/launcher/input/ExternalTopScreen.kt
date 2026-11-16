package com.droiddeck.launcher.input

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import com.droiddeck.launcher.glasses.Glasses
import com.droiddeck.launcher.session.SessionPrefs

object ExternalTopScreen {
    fun display(context: Context, displayManager: DisplayManager, primaryDisplayId: Int): Display? {
        if (!SessionPrefs.externalTopScreen(context)) return null
        return pick(displayManager, primaryDisplayId)
    }

    fun matches(context: Context, displayManager: DisplayManager, primaryDisplayId: Int, target: Display): Boolean =
        display(context, displayManager, primaryDisplayId)?.displayId == target.displayId

    private fun pick(displayManager: DisplayManager, primaryDisplayId: Int): Display? {
        val external = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).filter {
            it.isValid && it.displayId != primaryDisplayId && it.state != Display.STATE_OFF &&
                (it.flags and Display.FLAG_PRESENTATION) != 0 && !DualScreen.builtIn(it) && !DualScreen.taken(it.displayId)
        }
        if (external.isEmpty()) return null
        val glasses = Glasses.state.value
        val glassesId = glasses.displayId.takeIf { it != Display.INVALID_DISPLAY }
            ?: Glasses.selectGlassesDisplay(external, glasses.present)?.displayId
        return external.firstOrNull { it.displayId != glassesId && Glasses.selectGlassesDisplay(listOf(it), false) == null }
    }

    fun panel(remembered: String?): SecondScreenMode =
        SecondScreenMode.entries.firstOrNull { it.id == remembered && it in PANELS } ?: SecondScreenMode.PC_KEYBOARD

    private val PANELS = setOf(
        SecondScreenMode.PC_KEYBOARD, SecondScreenMode.KEYBOARD_TRACKPAD, SecondScreenMode.TERMINAL, SecondScreenMode.CUSTOM_CONTROLS,
    )
}
