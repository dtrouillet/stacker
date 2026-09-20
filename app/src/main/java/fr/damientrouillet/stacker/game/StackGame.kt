package fr.damientrouillet.stacker.game

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.random.Random

/**
 * The whole game simulation. Deliberately free of any Android dependency so it
 * can be unit tested on a plain JVM.
 *
 * Coordinates: x and z span the horizontal plane, y points up. The tower grows
 * along +y and the block currently in play slides along [movingAxis].
 */
class StackGame(
    settings: GameSettings = GameSettings(),
    private val random: Random = Random.Default
) {

    /** Callbacks used by the view layer to trigger sound and haptics. */
    interface Listener {
        fun onStarted()
        fun onPlaced(perfect: Boolean, comboStep: Int)
        fun onGameOver(score: Int, newBest: Boolean)
    }

    /**
     * Options for the next game. Changing them mid-game is harmless: the new
     * values take effect on the next [reset].
     */
    var settings: GameSettings = settings

    val blocks = ArrayList<Block>()
    val slices = ArrayList<FallingSlice>()
    val rings = ArrayList<PerfectRing>()

    var listener: Listener? = null

    var state: GameState = GameState.READY
        private set

    var score: Int = 0
        private set

    var combo: Int = 0
        private set

    var best: Int = 0

    var moving: Block? = null
        private set

    var movingAxis: Axis = Axis.X
        private set

    /** Altitude the camera is centred on, smoothed towards the top of the tower. */
    var cameraY: Float = 0f
        private set

    /** Colour index of the block currently in play, used to tint the background. */
    val colorIndex: Int get() = moving?.colorIndex ?: blocks.size

    /** Plate size this game started with, captured at [reset]. */
    var baseSize: Float = settings.baseSize
        private set

    private var direction = 1
    private var overTimer = 0f

    init {
        reset()
    }

    /** Clears the tower and returns to the title screen. */
    fun reset() {
        blocks.clear()
        slices.clear()
        rings.clear()
        score = 0
        combo = 0
        overTimer = 0f
        direction = 1
        moving = null
        movingAxis = Axis.Z
        state = GameState.READY
        baseSize = settings.baseSize
        blocks.add(
            Block(
                cx = 0f,
                cz = 0f,
                sx = baseSize,
                sz = baseSize,
                y = -GameConfig.PEDESTAL_HEIGHT,
                height = GameConfig.PEDESTAL_HEIGHT,
                colorIndex = 0
            )
        )
        cameraY = towerTop()
    }

    fun towerTop(): Float = blocks.last().top

    /** A tap on the screen: start, place the current block, or restart. */
    fun tap() {
        when (state) {
            GameState.READY -> start()
            GameState.RUNNING -> place()
            GameState.OVER -> if (overTimer >= GameConfig.RESTART_DELAY) {
                reset()
                start()
            }
        }
    }

    private fun start() {
        state = GameState.RUNNING
        spawnNext()
        listener?.onStarted()
    }

    fun update(dt: Float) {
        when (state) {
            GameState.RUNNING -> advanceMovingBlock(dt)
            GameState.OVER -> overTimer += dt
            GameState.READY -> Unit
        }
        updateSlices(dt)
        updateRings(dt)
        updateAnimations(dt)
        updateCamera(dt)
    }

    private fun advanceMovingBlock(dt: Float) {
        val block = moving ?: return
        val anchor = blocks.last().center(movingAxis)
        val travel = block.size(movingAxis) * 0.5f + GameConfig.TRAVEL_MARGIN
        var center = block.center(movingAxis) + direction * currentSpeed() * dt
        if (center >= anchor + travel) {
            center = anchor + travel
            direction = -1
        } else if (center <= anchor - travel) {
            center = anchor - travel
            direction = 1
        }
        block.setCenter(movingAxis, center)
    }

    fun currentSpeed(): Float = GameConfig.speedFor(score)

    private fun place() {
        val current = moving ?: return
        val previous = blocks.last()
        val axis = movingAxis

        val delta = current.center(axis) - previous.center(axis)
        val spread = abs(delta)
        val previousSize = previous.size(axis)
        val overlap = previousSize - spread

        if (overlap <= GameConfig.MIN_SIZE) {
            loseWith(current, if (delta >= 0f) 1f else -1f, axis)
            return
        }

        if (spread <= GameConfig.PERFECT_TOLERANCE) {
            combo++
            val grown = min(baseSize, previousSize + GameConfig.growthFor(combo))
            current.setCenter(axis, previous.center(axis))
            current.setSize(axis, grown)
            rings.add(PerfectRing(current.cx, current.cz, current.sx, current.sz, current.top))
            listener?.onPlaced(perfect = true, comboStep = combo)
        } else {
            combo = 0
            emitSlice(current, previous, axis, delta)
            current.setCenter(axis, previous.center(axis) + delta * 0.5f)
            current.setSize(axis, overlap)
            listener?.onPlaced(perfect = false, comboStep = 0)
        }

        current.placeAnim = GameConfig.PLACE_ANIM
        blocks.add(current)
        moving = null
        score++
        spawnNext()
    }

    /** Cuts the overhanging part off [current] and sends it tumbling. */
    private fun emitSlice(current: Block, previous: Block, axis: Axis, delta: Float) {
        val sign = if (delta > 0f) 1f else -1f
        val sliceSize = abs(delta)
        val sliceCenter = previous.center(axis) + sign * previous.size(axis) * 0.5f + delta * 0.5f
        val onX = axis == Axis.X
        slices.add(
            FallingSlice(
                cx = if (onX) sliceCenter else current.cx,
                cz = if (onX) current.cz else sliceCenter,
                sx = if (onX) sliceSize else current.sx,
                sz = if (onX) current.sz else sliceSize,
                y = current.y,
                height = current.height,
                colorIndex = current.colorIndex,
                driftX = if (onX) sign * 0.45f else 0f,
                driftZ = if (onX) 0f else sign * 0.45f,
                spin = (random.nextFloat() * 80f + 40f) * sign
            )
        )
    }

    private fun loseWith(current: Block, sign: Float, axis: Axis) {
        val onX = axis == Axis.X
        slices.add(
            FallingSlice(
                cx = current.cx,
                cz = current.cz,
                sx = current.sx,
                sz = current.sz,
                y = current.y,
                height = current.height,
                colorIndex = current.colorIndex,
                driftX = if (onX) sign * 0.5f else 0f,
                driftZ = if (onX) 0f else sign * 0.5f,
                spin = (random.nextFloat() * 70f + 50f) * sign
            )
        )
        moving = null
        combo = 0
        state = GameState.OVER
        overTimer = 0f
        val newBest = score > best
        if (newBest) best = score
        listener?.onGameOver(score, newBest)
    }

    private fun spawnNext() {
        val previous = blocks.last()
        movingAxis = movingAxis.other()
        val travel = previous.size(movingAxis) * 0.5f + GameConfig.TRAVEL_MARGIN
        val onX = movingAxis == Axis.X
        direction = 1
        moving = Block(
            cx = if (onX) previous.cx - travel else previous.cx,
            cz = if (onX) previous.cz else previous.cz - travel,
            sx = previous.sx,
            sz = previous.sz,
            y = previous.top,
            height = GameConfig.BLOCK_HEIGHT,
            colorIndex = blocks.size
        )
    }

    private fun updateSlices(dt: Float) {
        var i = slices.size - 1
        while (i >= 0) {
            val slice = slices[i]
            slice.update(dt)
            if (slice.y < cameraY - GameConfig.SLICE_CULL_DEPTH) {
                slices.removeAt(i)
            }
            i--
        }
    }

    private fun updateRings(dt: Float) {
        var i = rings.size - 1
        while (i >= 0) {
            val ring = rings[i]
            ring.update(dt)
            if (ring.done) rings.removeAt(i)
            i--
        }
    }

    private fun updateAnimations(dt: Float) {
        for (block in blocks) {
            if (block.placeAnim > 0f) {
                block.placeAnim = (block.placeAnim - dt).coerceAtLeast(0f)
            }
        }
    }

    private fun updateCamera(dt: Float) {
        val target = towerTop()
        val factor = 1f - exp(-GameConfig.CAMERA_SMOOTH * dt)
        cameraY += (target - cameraY) * factor
    }

    /** True once a lost game accepts a tap to start over. */
    fun canRestart(): Boolean = state == GameState.OVER && overTimer >= GameConfig.RESTART_DELAY

    /** Seconds elapsed since the game was lost. */
    fun timeSinceGameOver(): Float = overTimer
}
