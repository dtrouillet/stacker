package fr.damientrouillet.stacker.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSettingsTest {

    private val epsilon = 1e-4f

    @Test
    fun `the defaults are sound on and a full sized plate`() {
        val settings = GameSettings()
        assertTrue(settings.soundEnabled)
        assertEquals(GameConfig.DEFAULT_BASE_SIZE, settings.baseSize, epsilon)
        assertEquals(100, settings.baseSizePercent)
    }

    @Test
    fun `toggling sound flips only the sound`() {
        val muted = GameSettings().toggleSound()
        assertFalse(muted.soundEnabled)
        assertEquals(GameConfig.DEFAULT_BASE_SIZE, muted.baseSize, epsilon)
        assertTrue(muted.toggleSound().soundEnabled)
    }

    @Test
    fun `the plate size moves one notch at a time`() {
        val start = GameSettings()
        val smaller = start.withBaseSizeShifted(-1)
        assertEquals(start.baseSize - GameSettings.BASE_SIZE_STEP, smaller.baseSize, epsilon)
        assertEquals(start.baseSize, smaller.withBaseSizeShifted(1).baseSize, epsilon)
    }

    @Test
    fun `the plate size stops at both ends`() {
        var settings = GameSettings()
        repeat(20) { settings = settings.withBaseSizeShifted(-1) }
        assertEquals(GameSettings.MIN_BASE_SIZE, settings.baseSize, epsilon)
        assertFalse(settings.canShrinkBaseSize)
        assertTrue(settings.canGrowBaseSize)

        repeat(40) { settings = settings.withBaseSizeShifted(1) }
        assertEquals(GameSettings.MAX_BASE_SIZE, settings.baseSize, epsilon)
        assertFalse(settings.canGrowBaseSize)
        assertTrue(settings.canShrinkBaseSize)
    }

    @Test
    fun `a stored value off the grid snaps onto it`() {
        assertEquals(0.8f, GameSettings.normaliseBaseSize(0.83f), epsilon)
        assertEquals(GameSettings.MIN_BASE_SIZE, GameSettings.normaliseBaseSize(0.01f), epsilon)
        assertEquals(GameSettings.MAX_BASE_SIZE, GameSettings.normaliseBaseSize(9f), epsilon)
    }

    @Test
    fun `every choice is reachable by stepping and is inside the range`() {
        val choices = GameSettings.baseSizeChoices()
        assertTrue(choices.size >= 3)
        assertEquals(GameSettings.MIN_BASE_SIZE, choices.first(), epsilon)
        assertEquals(GameSettings.MAX_BASE_SIZE, choices.last(), epsilon)
        assertTrue(choices.any { kotlin.math.abs(it - GameConfig.DEFAULT_BASE_SIZE) < epsilon })

        var settings = GameSettings(baseSize = GameSettings.MIN_BASE_SIZE)
        val walked = ArrayList<Float>()
        repeat(choices.size) {
            walked.add(settings.baseSize)
            settings = settings.withBaseSizeShifted(1)
        }
        for (i in choices.indices) assertEquals(choices[i], walked[i], epsilon)
    }

    @Test
    fun `the percentage follows the plate size`() {
        assertEquals(60, GameSettings(baseSize = 0.6f).baseSizePercent)
        assertEquals(140, GameSettings(baseSize = 1.4f).baseSizePercent)
    }
}
