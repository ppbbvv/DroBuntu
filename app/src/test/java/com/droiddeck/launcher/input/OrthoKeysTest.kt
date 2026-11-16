package com.droiddeck.launcher.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrthoKeysTest {
    private val halves = listOf(ORTHO_LEFT, ORTHO_RIGHT)
    private val slots = halves.flatten().flatten()
    private val base = slots.map { it.base }.filter { it.code != ORTHO_FN }
    private val layered = slots.mapNotNull { it.fn }

    @Test fun everyRowFillsTheGrid() {
        for (half in halves) {
            assertEquals(5, half.size)
            for (row in half) assertEquals(ORTHO_COLUMNS, row.sumOf { it.base.weight.toDouble() }.toFloat(), 0f)
        }
    }

    @Test fun theBaseLayerHasLettersDigitsAndTheKeysAroundThem() {
        val codes = base.map { it.code }.toSet()
        val letters = (16..25) + (30..38) + (44..50)
        val digits = (2..11).toList()
        val editing = listOf(1, 14, 15, 28, 57, 29, 42, 56, 125, 103, 105, 106, 108)
        for (code in letters + digits + editing) assertTrue("no key sends $code", code in codes)
    }

    @Test fun theFnLayerAddsOnlyWhatTheBaseLayerLacks() {
        val codes = layered.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(codes.none { code -> base.any { it.code == code } })
        val wanted = (59..68) + listOf(87, 88, 41, 26, 27, 43, 58, 111, 110, 102, 107, 104, 109)
        for (code in wanted) assertTrue("Fn reaches no key sending $code", code in codes)
    }

    @Test fun onlySpaceIsOnBothHalves() {
        val repeated = base.groupBy { it.code }.filterValues { it.size > 1 }.keys
        assertEquals(setOf(57), repeated)
    }

    @Test fun oneFnKeyAndNoModifierBehindIt() {
        val fn = slots.filter { it.base.code == ORTHO_FN }
        assertEquals(1, fn.size)
        assertNull(fn.single().fn)
        assertTrue(layered.none { it.code in PC_MODIFIERS })
        assertTrue(slots.filter { it.base.code in PC_MODIFIERS }.all { it.fn == null })
    }

    @Test fun codesAndShiftedLegendsMatchThePcKeyTable() {
        for (key in base + layered) {
            val known = PC_KEYS[key.code]
            assertTrue("${key.label} sends ${key.code}, which the PC keyboard does not know", known != null)
            assertEquals(known?.shifted, key.shifted)
        }
    }
}
