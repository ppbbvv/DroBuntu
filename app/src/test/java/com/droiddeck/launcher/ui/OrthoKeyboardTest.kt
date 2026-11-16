package com.droiddeck.launcher.ui

import com.droiddeck.launcher.input.ORTHO_FN
import com.droiddeck.launcher.input.ORTHO_LEFT
import com.droiddeck.launcher.input.ORTHO_RIGHT
import com.droiddeck.launcher.input.OrthoSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrthoKeyboardTest {
    private val sent = mutableListOf<String>()
    private val board = OrthoKeyboard { code, down -> sent += "$code ${if (down) "down" else "up"}" }
    private val slots = (ORTHO_LEFT + ORTHO_RIGHT).flatten()

    private fun slot(code: Int): OrthoSlot = slots.first { it.base.code == code }

    private fun tap(code: Int) {
        val slot = slot(code)
        board.press(slot)
        board.release(slot)
    }

    @Test fun aKeyGoesDownAndUp() {
        tap(Q)
        assertEquals(listOf("$Q down", "$Q up"), sent)
    }

    @Test fun shiftHoldsForOneKey() {
        tap(SHIFT)
        assertTrue(board.shifted)
        tap(A)
        assertFalse(board.shifted)
        tap(A)
        assertEquals(listOf("$SHIFT down", "$A down", "$A up", "$SHIFT up", "$A down", "$A up"), sent)
    }

    @Test fun aSecondTapLocksAModifierAndAThirdLetsGo() {
        tap(CTRL)
        tap(CTRL)
        assertEquals(STICKY_LOCKED, board.sticky(slot(CTRL)))
        tap(C)
        tap(V)
        tap(CTRL)
        assertEquals(STICKY_OFF, board.sticky(slot(CTRL)))
        assertEquals(listOf("$CTRL down", "$C down", "$C up", "$V down", "$V up", "$CTRL up"), sent)
    }

    @Test fun fnReachesTheSecondLayerForOneKey() {
        tap(ORTHO_FN)
        assertEquals(STICKY_ONCE, board.fn)
        tap(ONE)
        assertEquals(STICKY_OFF, board.fn)
        tap(ONE)
        assertEquals(listOf("$F1 down", "$F1 up", "$ONE down", "$ONE up"), sent)
    }

    @Test fun lockedFnStaysUntilTappedAgain() {
        tap(ORTHO_FN)
        tap(ORTHO_FN)
        assertEquals(STICKY_LOCKED, board.fn)
        tap(ONE)
        tap(TWO)
        tap(ORTHO_FN)
        tap(ONE)
        assertEquals(listOf("$F1 down", "$F1 up", "$F2 down", "$F2 up", "$ONE down", "$ONE up"), sent)
    }

    @Test fun keysWithoutASecondLayerStillTypeUnderFn() {
        tap(ORTHO_FN)
        tap(Q)
        assertEquals(listOf("$Q down", "$Q up"), sent)
        assertEquals(STICKY_OFF, board.fn)
    }

    @Test fun aKeyComesUpWithTheCodeItWentDownWith() {
        val one = slot(ONE)
        val q = slot(Q)
        tap(ORTHO_FN)
        board.press(one)
        board.press(q)
        board.release(q)
        board.release(one)
        assertEquals(listOf("$F1 down", "$Q down", "$Q up", "$F1 up"), sent)
    }

    @Test fun shiftAndFnCombine() {
        tap(SHIFT)
        tap(ORTHO_FN)
        tap(ESC)
        assertEquals(listOf("$SHIFT down", "$GRAVE down", "$GRAVE up", "$SHIFT up"), sent)
    }

    @Test fun capsLockIsRememberedForTheLegends() {
        tap(ORTHO_FN)
        tap(TAB)
        assertEquals(listOf("$CAPS down", "$CAPS up"), sent)
        assertTrue(board.capsOn)
    }

    @Test fun leavingLetsGoOfEverythingHeld() {
        tap(CTRL)
        board.press(slot(A))
        board.releaseAll()
        assertEquals(listOf("$CTRL down", "$A down", "$A up", "$CTRL up"), sent)
        assertEquals(STICKY_OFF, board.sticky(slot(CTRL)))
    }

    private companion object {
        const val ESC = 1
        const val ONE = 2
        const val TWO = 3
        const val TAB = 15
        const val Q = 16
        const val CTRL = 29
        const val A = 30
        const val GRAVE = 41
        const val SHIFT = 42
        const val C = 46
        const val V = 47
        const val CAPS = 58
        const val F1 = 59
        const val F2 = 60
    }
}
