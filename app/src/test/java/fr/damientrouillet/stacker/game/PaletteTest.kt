package fr.damientrouillet.stacker.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {

    private fun red(color: Int) = (color ushr 16) and 0xFF
    private fun green(color: Int) = (color ushr 8) and 0xFF
    private fun blue(color: Int) = color and 0xFF
    private fun alpha(color: Int) = (color ushr 24) and 0xFF

    @Test
    fun `hsv produces the expected primaries`() {
        assertEquals(0xFFFF0000.toInt(), Palette.hsv(0f, 1f, 1f))
        assertEquals(0xFF00FF00.toInt(), Palette.hsv(120f, 1f, 1f))
        assertEquals(0xFF0000FF.toInt(), Palette.hsv(240f, 1f, 1f))
        assertEquals(0xFFFFFFFF.toInt(), Palette.hsv(0f, 0f, 1f))
        assertEquals(0xFF000000.toInt(), Palette.hsv(0f, 0f, 0f))
    }

    @Test
    fun `hue wraps around in both directions`() {
        assertEquals(Palette.hsv(10f, 1f, 1f), Palette.hsv(370f, 1f, 1f))
        assertEquals(Palette.hsv(350f, 1f, 1f), Palette.hsv(-10f, 1f, 1f))
        assertTrue(Palette.hueFor(-5) in 0f..360f)
        assertTrue(Palette.hueFor(10_000) in 0f..360f)
    }

    @Test
    fun `colours stay opaque and drift from block to block`() {
        assertEquals(255, alpha(Palette.blockColor(0)))
        assertNotEquals(Palette.blockColor(0), Palette.blockColor(1))
        assertNotEquals(Palette.blockColor(0), Palette.blockColor(7))
    }

    @Test
    fun `shading darkens every channel without touching alpha`() {
        val base = Palette.blockColor(3)
        val shaded = Palette.shade(base, Palette.SHADE_RIGHT)
        assertEquals(255, alpha(shaded))
        assertTrue(red(shaded) <= red(base))
        assertTrue(green(shaded) <= green(base))
        assertTrue(blue(shaded) <= blue(base))
    }

    @Test
    fun `the two visible sides are darker than the lit top`() {
        val base = Palette.blockColor(5)
        val right = Palette.shade(base, Palette.SHADE_RIGHT)
        val left = Palette.shade(base, Palette.SHADE_LEFT)
        assertTrue(luminance(left) < luminance(right))
        assertTrue(luminance(right) < luminance(base))
    }

    @Test
    fun `the background stays darker than the tower`() {
        for (index in 0 until 120) {
            val block = luminance(Palette.blockColor(index))
            assertTrue(luminance(Palette.backgroundTop(index)) < block)
            assertTrue(luminance(Palette.backgroundBottom(index)) < luminance(Palette.backgroundTop(index)))
        }
    }

    @Test
    fun `withAlpha replaces only the alpha channel`() {
        val color = Palette.withAlpha(0xFF123456.toInt(), 128)
        assertEquals(128, alpha(color))
        assertEquals(0x12, red(color))
        assertEquals(0x34, green(color))
        assertEquals(0x56, blue(color))
    }

    private fun luminance(color: Int) =
        0.299 * red(color) + 0.587 * green(color) + 0.114 * blue(color)
}
