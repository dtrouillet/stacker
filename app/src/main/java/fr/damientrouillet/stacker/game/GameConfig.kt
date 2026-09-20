package fr.damientrouillet.stacker.game

import kotlin.math.PI
import kotlin.math.sin

/**
 * Tunable constants and the rules derived from them. All distances are in
 * world units, where a default sized plate measures [DEFAULT_BASE_SIZE].
 */
object GameConfig {

    /**
     * Width and depth of a default plate. It is also the reference the
     * isometric projection scales by, so a game configured with a smaller
     * plate really does look smaller on screen rather than being zoomed in.
     */
    const val DEFAULT_BASE_SIZE = 1f

    /** Height of every stacked block. */
    const val BLOCK_HEIGHT = 0.17f

    /** The pedestal is a tall column so the tower appears to rise out of the void. */
    const val PEDESTAL_HEIGHT = 10f

    /** Misalignment below this threshold counts as a perfect placement. */
    const val PERFECT_TOLERANCE = 0.042f

    /** Consecutive perfect placements before plates start growing back. */
    const val COMBO_GROW_AT = 6

    /** Consecutive perfect placements before plates grow back much faster. */
    const val COMBO_BIG_GROW_AT = 8

    /** Growth per perfect placement once [COMBO_GROW_AT] is reached. */
    const val GROW_AMOUNT = 0.055f

    /** Growth per perfect placement once [COMBO_BIG_GROW_AT] is reached. */
    const val BIG_GROW_AMOUNT = 0.14f

    /** Below this size an overlap is considered a miss. */
    const val MIN_SIZE = 0.012f

    /** Sideways speed of the very first plate, in world units per second. */
    const val START_SPEED = 1.75f

    /** Speed the long term trend adds for every plate placed. */
    const val SPEED_PER_BLOCK = 0.02f

    /** How far the wave pushes the speed either side of the trend. */
    const val SPEED_WAVE_AMPLITUDE = 0.45f

    /** Plates per full wave, so the speed eases off roughly every 11 plates. */
    const val SPEED_WAVE_PERIOD = 11f

    /** The speed never drops below this, however low the wave goes. */
    const val MIN_SPEED = 1.25f

    /** Hard cap on the sideways speed. */
    const val MAX_SPEED = 4.2f

    /** Extra travel beyond the plate's own half size, on each side of the tower. */
    const val TRAVEL_MARGIN = 0.9f

    /** Downward acceleration applied to sliced off pieces. */
    const val GRAVITY = 9f

    /** Exponential smoothing factor for the camera rise. */
    const val CAMERA_SMOOTH = 9f

    /** Duration of the squash animation played when a plate lands. */
    const val PLACE_ANIM = 0.22f

    /** Lifetime of the white ring emitted by a perfect placement. */
    const val RING_LIFE = 0.45f

    /** How long the game ignores taps after a loss, so the last tap is not reused. */
    const val RESTART_DELAY = 0.7f

    /** Slices are dropped from the simulation once this far below the camera. */
    const val SLICE_CULL_DEPTH = 22f

    /**
     * Sideways speed at a given score. A gentle upward trend carries a wave on
     * top, so the game keeps getting harder overall while still easing off for
     * a few plates at a time instead of climbing relentlessly.
     */
    fun speedFor(score: Int): Float {
        val trend = START_SPEED + score * SPEED_PER_BLOCK
        val wave = SPEED_WAVE_AMPLITUDE * sin(2.0 * PI * score / SPEED_WAVE_PERIOD).toFloat()
        return (trend + wave).coerceIn(MIN_SPEED, MAX_SPEED)
    }

    /** How much a plate grows back for a perfect placement at this streak length. */
    fun growthFor(combo: Int): Float = when {
        combo >= COMBO_BIG_GROW_AT -> BIG_GROW_AMOUNT
        combo >= COMBO_GROW_AT -> GROW_AMOUNT
        else -> 0f
    }
}
