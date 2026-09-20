package fr.damientrouillet.stacker.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameConfigTest {

    private val epsilon = 1e-4f

    private fun speeds(upTo: Int) = (0..upTo).map { GameConfig.speedFor(it) }

    @Test
    fun `the first plate moves at the starting speed`() {
        assertEquals(GameConfig.START_SPEED, GameConfig.speedFor(0), epsilon)
    }

    @Test
    fun `the speed stays inside its bounds at every score`() {
        for (speed in speeds(2_000)) {
            assertTrue(speed >= GameConfig.MIN_SPEED - epsilon)
            assertTrue(speed <= GameConfig.MAX_SPEED + epsilon)
        }
    }

    @Test
    fun `the speed sometimes drops from one plate to the next`() {
        val curve = speeds(120)
        val drops = curve.zipWithNext().count { (before, after) -> after < before - epsilon }
        assertTrue("the curve should ease off regularly, found $drops drops", drops > 20)
    }

    @Test
    fun `the speed still trends upwards over a long game`() {
        val curve = speeds(200)
        // Compare whole waves, so the comparison is not taken at a wave's peak
        // against another wave's trough.
        val period = GameConfig.SPEED_WAVE_PERIOD.toInt()
        val early = curve.take(period).average()
        val late = curve.drop(100).take(period).average()
        assertTrue("late average $late should beat early average $early", late > early)
    }

    @Test
    fun `the speed climbs more gently than one unit every twenty plates`() {
        // The trend alone, ignoring the wave, over twenty plates.
        val over20 = 20 * GameConfig.SPEED_PER_BLOCK
        assertTrue("trend of $over20 per twenty plates is too steep", over20 < 0.5f)
    }

    @Test
    fun `a short streak grows nothing`() {
        for (combo in 0 until GameConfig.COMBO_GROW_AT) {
            assertEquals(0f, GameConfig.growthFor(combo), epsilon)
        }
    }

    @Test
    fun `growth steps up at six and again at eight`() {
        assertEquals(GameConfig.GROW_AMOUNT, GameConfig.growthFor(GameConfig.COMBO_GROW_AT), epsilon)
        assertEquals(
            GameConfig.GROW_AMOUNT,
            GameConfig.growthFor(GameConfig.COMBO_BIG_GROW_AT - 1),
            epsilon
        )
        assertEquals(
            GameConfig.BIG_GROW_AMOUNT,
            GameConfig.growthFor(GameConfig.COMBO_BIG_GROW_AT),
            epsilon
        )
        assertEquals(GameConfig.BIG_GROW_AMOUNT, GameConfig.growthFor(50), epsilon)
        assertTrue(GameConfig.BIG_GROW_AMOUNT > GameConfig.GROW_AMOUNT)
    }
}
