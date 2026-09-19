package fr.damientrouillet.stacker.game

/**
 * Tunable constants for the game. All distances are expressed in world units
 * where the very first block measures [BASE_SIZE] x [BASE_SIZE].
 */
object GameConfig {

    /** Width and depth of the pedestal, and the largest a block can ever be. */
    const val BASE_SIZE = 1f

    /** Height of every stacked block. */
    const val BLOCK_HEIGHT = 0.17f

    /** The pedestal is a tall column so the tower appears to rise out of the void. */
    const val PEDESTAL_HEIGHT = 10f

    /** Misalignment below this threshold counts as a perfect placement. */
    const val PERFECT_TOLERANCE = 0.042f

    /** Number of consecutive perfect placements before blocks start growing back. */
    const val COMBO_GROW_AT = 6

    /** How much a block grows back per perfect placement once the combo is reached. */
    const val GROW_AMOUNT = 0.055f

    /** Below this size an overlap is considered a miss. */
    const val MIN_SIZE = 0.012f

    /** Sideways speed of the very first block, in world units per second. */
    const val START_SPEED = 1.75f

    /** Speed added for every block placed. */
    const val SPEED_PER_BLOCK = 0.045f

    /** Hard cap on the sideways speed. */
    const val MAX_SPEED = 5.2f

    /** Extra travel beyond the block's own half size, on each side of the tower. */
    const val TRAVEL_MARGIN = 0.9f

    /** Downward acceleration applied to sliced off pieces. */
    const val GRAVITY = 9f

    /** Exponential smoothing factor for the camera rise. */
    const val CAMERA_SMOOTH = 9f

    /** Duration of the squash animation played when a block lands. */
    const val PLACE_ANIM = 0.22f

    /** Lifetime of the white ring emitted by a perfect placement. */
    const val RING_LIFE = 0.45f

    /** How long the game ignores taps after a loss, so the last tap is not reused. */
    const val RESTART_DELAY = 0.7f

    /** Slices are dropped from the simulation once this far below the camera. */
    const val SLICE_CULL_DEPTH = 22f
}
