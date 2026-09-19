package fr.damientrouillet.stacker.game

import kotlin.math.abs

/**
 * Colour cycling used by the tower and the background. Pure Kotlin so it stays
 * unit testable; colours are returned as packed ARGB integers.
 */
object Palette {

    private const val START_HUE = 196f
    private const val HUE_STEP = 5.5f

    private const val BLOCK_SATURATION = 0.52f
    private const val BLOCK_VALUE = 0.88f

    /** Brightness multiplier of the face pointing towards +x. */
    const val SHADE_RIGHT = 0.76f

    /** Brightness multiplier of the face pointing towards +z. */
    const val SHADE_LEFT = 0.56f

    fun hueFor(index: Int): Float {
        var hue = (START_HUE + index * HUE_STEP) % 360f
        if (hue < 0f) hue += 360f
        return hue
    }

    fun blockColor(index: Int): Int = hsv(hueFor(index), BLOCK_SATURATION, BLOCK_VALUE)

    fun backgroundTop(index: Int): Int = hsv(hueFor(index) + 10f, 0.50f, 0.27f)

    fun backgroundBottom(index: Int): Int = hsv(hueFor(index) + 10f, 0.66f, 0.08f)

    /** Multiplies the rgb channels of [color] by [factor], keeping the alpha. */
    fun shade(color: Int, factor: Float): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF) * factor
        val g = ((color ushr 8) and 0xFF) * factor
        val b = (color and 0xFF) * factor
        return pack(a, r.toInt(), g.toInt(), b.toInt())
    }

    fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    fun hsv(hueDegrees: Float, saturation: Float, value: Float): Int {
        var hue = hueDegrees % 360f
        if (hue < 0f) hue += 360f
        val s = saturation.coerceIn(0f, 1f)
        val v = value.coerceIn(0f, 1f)
        val c = v * s
        val sector = hue / 60f
        val x = c * (1f - abs(sector % 2f - 1f))
        val m = v - c
        val r: Float
        val g: Float
        val b: Float
        when (sector.toInt()) {
            0 -> { r = c; g = x; b = 0f }
            1 -> { r = x; g = c; b = 0f }
            2 -> { r = 0f; g = c; b = x }
            3 -> { r = 0f; g = x; b = c }
            4 -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }
        return pack(
            255,
            ((r + m) * 255f + 0.5f).toInt(),
            ((g + m) * 255f + 0.5f).toInt(),
            ((b + m) * 255f + 0.5f).toInt()
        )
    }

    private fun pack(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or
            (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or
            b.coerceIn(0, 255)
}
