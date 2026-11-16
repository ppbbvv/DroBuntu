package com.droiddeck.launcher.input

class OrthoSlot(val base: PcKey, val fn: PcKey? = null)

const val ORTHO_FN = -1
const val ORTHO_COLUMNS = 6f

private fun slot(label: String, code: Int, shifted: String? = null, fn: PcKey? = null, weight: Float = 1f) =
    OrthoSlot(PcKey(label, code, weight, shifted), fn)

private fun fn(label: String, code: Int, shifted: String? = null) = PcKey(label, code, 1f, shifted)

val ORTHO_LEFT: List<List<OrthoSlot>> = listOf(
    listOf(
        slot("Esc", 1, fn = fn("`", 41, "~")), slot("1", 2, "!", fn("F1", 59)), slot("2", 3, "@", fn("F2", 60)),
        slot("3", 4, "#", fn("F3", 61)), slot("4", 5, "$", fn("F4", 62)), slot("5", 6, "%", fn("F5", 63)),
    ),
    listOf(slot("Tab", 15, fn = fn("Caps", 58)), slot("q", 16), slot("w", 17), slot("e", 18), slot("r", 19), slot("t", 20)),
    listOf(slot("Ctrl", 29), slot("a", 30), slot("s", 31), slot("d", 32), slot("f", 33), slot("g", 34)),
    listOf(slot("⇧", 42), slot("z", 44), slot("x", 45), slot("c", 46), slot("v", 47), slot("b", 48)),
    listOf(
        slot("Fn", ORTHO_FN), slot("Meta", 125), slot("Alt", 56), slot("=", 13, "+", fn("F12", 88)),
        slot("Space", 57, weight = 2f),
    ),
)

val ORTHO_RIGHT: List<List<OrthoSlot>> = listOf(
    listOf(
        slot("6", 7, "^", fn("F6", 64)), slot("7", 8, "&", fn("F7", 65)), slot("8", 9, "*", fn("F8", 66)),
        slot("9", 10, "(", fn("F9", 67)), slot("0", 11, ")", fn("F10", 68)), slot("-", 12, "_", fn("F11", 87)),
    ),
    listOf(
        slot("y", 21), slot("u", 22), slot("i", 23), slot("o", 24, fn = fn("[", 26, "{")), slot("p", 25, fn = fn("]", 27, "}")),
        slot("⌫", 14, fn = fn("Del", 111)),
    ),
    listOf(slot("h", 35), slot("j", 36), slot("k", 37), slot("l", 38), slot(";", 39, ":"), slot("'", 40, "\"", fn("\\", 43, "|"))),
    listOf(slot("n", 49), slot("m", 50), slot(",", 51, "<"), slot(".", 52, ">"), slot("/", 53, "?"), slot("⏎", 28, fn = fn("Ins", 110))),
    listOf(
        slot("Space", 57, weight = 2f), slot("←", 105, fn = fn("Home", 102)), slot("↓", 108, fn = fn("PgDn", 109)),
        slot("↑", 103, fn = fn("PgUp", 104)), slot("→", 106, fn = fn("End", 107)),
    ),
)
