package com.droiddeck.launcher.input

import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalTopScreenTest {
    @Test fun theAppKeyboardOpensUntilTheUserPicksAnotherPanel() {
        assertEquals(SecondScreenMode.PC_KEYBOARD, ExternalTopScreen.panel(null))
        assertEquals(SecondScreenMode.PC_KEYBOARD, ExternalTopScreen.panel("gone"))
        assertEquals(SecondScreenMode.PC_KEYBOARD, ExternalTopScreen.panel("pc-keyboard"))
        assertEquals(SecondScreenMode.TERMINAL, ExternalTopScreen.panel("terminal"))
        assertEquals(SecondScreenMode.KEYBOARD_TRACKPAD, ExternalTopScreen.panel("keyboard-trackpad"))
        assertEquals(SecondScreenMode.CUSTOM_CONTROLS, ExternalTopScreen.panel("custom-controls"))
    }

    @Test fun panelsThatNeedTheSecondScreenFallBackToTheAppKeyboard() {
        for (id in listOf("none", "deck-controls", "dashboard", "desktop")) {
            assertEquals(SecondScreenMode.PC_KEYBOARD, ExternalTopScreen.panel(id))
        }
    }
}
