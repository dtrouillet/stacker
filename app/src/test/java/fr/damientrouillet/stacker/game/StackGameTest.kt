package fr.damientrouillet.stacker.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StackGameTest {

    private val epsilon = 1e-4f

    private fun startedGame(): StackGame = StackGame().apply { tap() }

    private fun placePerfect(game: StackGame) {
        val previous = game.blocks.last()
        game.moving!!.setCenter(game.movingAxis, previous.center(game.movingAxis))
        game.tap()
    }

    private fun placeOffset(game: StackGame, delta: Float) {
        val previous = game.blocks.last()
        game.moving!!.setCenter(game.movingAxis, previous.center(game.movingAxis) + delta)
        game.tap()
    }

    @Test
    fun `a new game shows only the pedestal`() {
        val game = StackGame()
        assertEquals(GameState.READY, game.state)
        assertEquals(1, game.blocks.size)
        assertEquals(0, game.score)
        assertNull(game.moving)
        assertEquals(GameConfig.BASE_SIZE, game.blocks.first().sx, epsilon)
        assertEquals(0f, game.towerTop(), epsilon)
    }

    @Test
    fun `the first tap starts the game and spawns a block`() {
        val game = startedGame()
        assertEquals(GameState.RUNNING, game.state)
        val moving = game.moving
        assertNotNull(moving)
        assertEquals(GameConfig.BASE_SIZE, moving!!.sx, epsilon)
        assertEquals(0f, moving.y, epsilon)
        assertEquals(Axis.X, game.movingAxis)
    }

    @Test
    fun `consecutive blocks alternate axes`() {
        val game = startedGame()
        assertEquals(Axis.X, game.movingAxis)
        placePerfect(game)
        assertEquals(Axis.Z, game.movingAxis)
        placePerfect(game)
        assertEquals(Axis.X, game.movingAxis)
    }

    @Test
    fun `a perfect placement keeps the size and emits a ring`() {
        val game = startedGame()
        placePerfect(game)

        assertEquals(1, game.score)
        assertEquals(1, game.combo)
        assertTrue(game.slices.isEmpty())
        assertEquals(1, game.rings.size)
        val placed = game.blocks.last()
        assertEquals(GameConfig.BASE_SIZE, placed.sx, epsilon)
        assertEquals(0f, placed.cx, epsilon)
        assertEquals(GameConfig.BLOCK_HEIGHT, game.towerTop(), epsilon)
    }

    @Test
    fun `an offset placement slices the overhang off`() {
        val game = startedGame()
        val delta = 0.3f
        placeOffset(game, delta)

        val placed = game.blocks.last()
        assertEquals(GameConfig.BASE_SIZE - delta, placed.sx, epsilon)
        assertEquals(delta / 2f, placed.cx, epsilon)
        assertEquals(0, game.combo)

        assertEquals(1, game.slices.size)
        val slice = game.slices.first()
        assertEquals(delta, slice.sx, epsilon)
        // The slice sits just outside the surviving block, on the overhang side.
        assertEquals(placed.cx + placed.sx / 2f + delta / 2f, slice.cx, epsilon)
        assertEquals(placed.cz, slice.cz, epsilon)
    }

    @Test
    fun `slicing works the same on the negative side`() {
        val game = startedGame()
        val delta = -0.25f
        placeOffset(game, delta)

        val placed = game.blocks.last()
        assertEquals(GameConfig.BASE_SIZE - abs(delta), placed.sx, epsilon)
        assertEquals(delta / 2f, placed.cx, epsilon)
        val slice = game.slices.first()
        assertEquals(abs(delta), slice.sx, epsilon)
        assertEquals(placed.cx - placed.sx / 2f - abs(delta) / 2f, slice.cx, epsilon)
    }

    @Test
    fun `the block only shrinks along the axis it travels on`() {
        val game = startedGame()
        placeOffset(game, 0.4f)
        val placed = game.blocks.last()
        assertEquals(GameConfig.BASE_SIZE - 0.4f, placed.sx, epsilon)
        assertEquals(GameConfig.BASE_SIZE, placed.sz, epsilon)
    }

    @Test
    fun `missing the tower ends the game`() {
        val game = startedGame()
        placeOffset(game, 1.2f)

        assertEquals(GameState.OVER, game.state)
        assertEquals(0, game.score)
        assertNull(game.moving)
        assertEquals(1, game.slices.size)
        // The tower keeps only the pedestal, the missed block falls away.
        assertEquals(1, game.blocks.size)
    }

    @Test
    fun `an overlap thinner than the minimum counts as a miss`() {
        val game = startedGame()
        placeOffset(game, GameConfig.BASE_SIZE - GameConfig.MIN_SIZE / 2f)
        assertEquals(GameState.OVER, game.state)
    }

    @Test
    fun `losing records a new best score`() {
        val game = startedGame()
        placePerfect(game)
        placePerfect(game)
        placeOffset(game, 5f)

        assertEquals(GameState.OVER, game.state)
        assertEquals(2, game.score)
        assertEquals(2, game.best)
    }

    @Test
    fun `the best score survives a restart`() {
        val game = startedGame()
        placePerfect(game)
        placeOffset(game, 5f)
        game.update(GameConfig.RESTART_DELAY + 0.1f)
        game.tap()

        assertEquals(GameState.RUNNING, game.state)
        assertEquals(0, game.score)
        assertEquals(1, game.best)
    }

    @Test
    fun `a tap right after losing is ignored`() {
        val game = startedGame()
        placeOffset(game, 5f)
        game.update(0.05f)
        game.tap()
        assertEquals(GameState.OVER, game.state)
        assertFalse(game.canRestart())
    }

    @Test
    fun `a long perfect streak grows the block back`() {
        val game = startedGame()
        placeOffset(game, 0.3f)
        val shrunk = game.blocks.last().sx
        assertEquals(GameConfig.BASE_SIZE - 0.3f, shrunk, epsilon)

        // Perfect placements on the Z axis do not touch the X size, so the X
        // size only moves on every other placement.
        repeat(GameConfig.COMBO_GROW_AT * 2) { placePerfect(game) }
        val grown = game.blocks.last().sx
        assertTrue("expected the block to grow back, was $shrunk then $grown", grown > shrunk)
        assertTrue(grown <= GameConfig.BASE_SIZE + epsilon)
    }

    @Test
    fun `growth never exceeds the base size`() {
        val game = startedGame()
        repeat(40) { placePerfect(game) }
        for (block in game.blocks) {
            assertTrue(block.sx <= GameConfig.BASE_SIZE + epsilon)
            assertTrue(block.sz <= GameConfig.BASE_SIZE + epsilon)
        }
    }

    @Test
    fun `the block oscillates around the tower without escaping its travel range`() {
        val game = startedGame()
        val travel = GameConfig.BASE_SIZE * 0.5f + GameConfig.TRAVEL_MARGIN
        var minimum = Float.MAX_VALUE
        var maximum = -Float.MAX_VALUE
        repeat(2000) {
            game.update(1f / 60f)
            val center = game.moving!!.cx
            minimum = minOf(minimum, center)
            maximum = maxOf(maximum, center)
        }
        assertTrue(minimum >= -travel - epsilon)
        assertTrue(maximum <= travel + epsilon)
        // It really does sweep the whole range, in both directions.
        assertTrue(maximum > travel * 0.9f)
        assertTrue(minimum < -travel * 0.9f)
    }

    @Test
    fun `the speed ramps up with the score and then caps`() {
        val game = startedGame()
        val initial = game.currentSpeed()
        assertEquals(GameConfig.START_SPEED, initial, epsilon)
        repeat(30) { placePerfect(game) }
        assertTrue(game.currentSpeed() > initial)
        repeat(200) { placePerfect(game) }
        assertEquals(GameConfig.MAX_SPEED, game.currentSpeed(), epsilon)
    }

    @Test
    fun `the camera rises towards the top of the tower`() {
        val game = startedGame()
        repeat(5) { placePerfect(game) }
        val target = game.towerTop()
        assertTrue(game.cameraY < target)
        repeat(120) { game.update(1f / 60f) }
        assertEquals(target, game.cameraY, 1e-2f)
    }

    @Test
    fun `slices fall and are eventually culled`() {
        val game = startedGame()
        placeOffset(game, 0.3f)
        assertEquals(1, game.slices.size)
        val startY = game.slices.first().y
        game.update(0.2f)
        assertTrue(game.slices.isEmpty() || game.slices.first().y < startY)
        repeat(600) { game.update(1f / 60f) }
        assertTrue(game.slices.isEmpty())
    }

    @Test
    fun `rings fade out on their own`() {
        val game = startedGame()
        placePerfect(game)
        assertEquals(1, game.rings.size)
        game.update(GameConfig.RING_LIFE + 0.05f)
        assertTrue(game.rings.isEmpty())
    }

    @Test
    fun `each block stacks exactly on top of the previous one`() {
        val game = startedGame()
        repeat(10) { placeOffset(game, 0.05f) }
        var expected = 0f
        for (block in game.blocks.drop(1)) {
            assertEquals(expected, block.y, epsilon)
            expected += GameConfig.BLOCK_HEIGHT
        }
        assertEquals(expected, game.moving!!.y, epsilon)
    }

    @Test
    fun `blocks never stick out of the pedestal footprint`() {
        val game = startedGame()
        repeat(60) { placeOffset(game, 0.01f) }
        for (block in game.blocks) {
            assertTrue(abs(block.cx) + block.sx / 2f <= GameConfig.BASE_SIZE / 2f + epsilon)
            assertTrue(abs(block.cz) + block.sz / 2f <= GameConfig.BASE_SIZE / 2f + epsilon)
        }
    }
}
