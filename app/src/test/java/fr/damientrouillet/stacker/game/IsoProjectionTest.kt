package fr.damientrouillet.stacker.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IsoProjectionTest {

    private val epsilon = 1e-3f

    private fun projection(width: Float = 1080f, height: Float = 2400f) =
        IsoProjection().apply { configure(width, height) }

    @Test
    fun `the origin sits at the horizontal centre and at the anchor line`() {
        val p = projection()
        assertEquals(540f, p.originX, epsilon)
        assertEquals(2400f * IsoProjection.ANCHOR, p.originY, epsilon)
        assertEquals(540f, p.x(0f, 0f), epsilon)
        assertEquals(p.originY, p.y(0f, 0f, 0f), epsilon)
    }

    @Test
    fun `a base sized block spans the configured fraction of the width`() {
        val p = projection()
        val half = GameConfig.BASE_SIZE / 2f
        val left = p.x(-half, half)
        val right = p.x(half, -half)
        assertEquals(1080f * IsoProjection.TOWER_WIDTH_RATIO, right - left, epsilon)
    }

    @Test
    fun `the top face is a diamond twice as wide as it is tall`() {
        val p = projection()
        val half = GameConfig.BASE_SIZE / 2f
        val width = p.x(half, -half) - p.x(-half, half)
        val height = p.y(half, half, 0f) - p.y(-half, -half, 0f)
        // cos(30) / sin(30) = sqrt(3), the signature ratio of an isometric view.
        assertEquals(1.7320508f, width / height, 1e-3f)
    }

    @Test
    fun `the x axis goes right and down, the z axis left and down`() {
        val p = projection()
        assertTrue(p.x(1f, 0f) > p.x(0f, 0f))
        assertTrue(p.y(1f, 0f, 0f) > p.y(0f, 0f, 0f))
        assertTrue(p.x(0f, 1f) < p.x(0f, 0f))
        assertTrue(p.y(0f, 1f, 0f) > p.y(0f, 0f, 0f))
    }

    @Test
    fun `altitude only moves a point up the screen`() {
        val p = projection()
        assertEquals(p.x(0.3f, 0.2f), p.x(0.3f, 0.2f), epsilon)
        val ground = p.y(0.3f, 0.2f, 0f)
        val raised = p.y(0.3f, 0.2f, 1f)
        assertEquals(p.drop(1f), ground - raised, epsilon)
        assertTrue(raised < ground)
    }

    @Test
    fun `raising the camera pulls the tower down the screen`() {
        val p = projection()
        val before = p.y(0f, 0f, 0f)
        p.cameraY = 1f
        assertEquals(before + p.drop(1f), p.y(0f, 0f, 0f), epsilon)
        // The point the camera is centred on always lands on the anchor line.
        assertEquals(p.originY, p.y(0f, 0f, 1f), epsilon)
    }

    @Test
    fun `the projection scales with the viewport width only`() {
        val narrow = projection(width = 720f, height = 1600f)
        val wide = projection(width = 1440f, height = 1600f)
        assertEquals(2f, wide.scale / narrow.scale, epsilon)
    }
}
