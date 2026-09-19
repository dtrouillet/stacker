package fr.damientrouillet.stacker.game

/**
 * True isometric projection: x runs towards the lower right, z towards the
 * lower left and y straight up. Pure Kotlin so the geometry can be unit tested
 * and reused by tooling outside the Android runtime.
 */
class IsoProjection {

    companion object {
        const val COS30 = 0.8660254f
        const val SIN30 = 0.5f

        /** Fraction of the viewport width taken by a full size block. */
        const val TOWER_WIDTH_RATIO = 0.62f

        /** Where the top of the tower sits vertically, as a fraction of the height. */
        const val ANCHOR = 0.60f
    }

    var scale: Float = 1f
        private set
    var originX: Float = 0f
        private set
    var originY: Float = 0f
        private set

    /** Altitude the viewport is centred on. */
    var cameraY: Float = 0f

    fun configure(width: Float, height: Float) {
        // A block of BASE_SIZE spans 2 * cos(30) * size horizontally once projected.
        scale = width * TOWER_WIDTH_RATIO / (2f * COS30 * GameConfig.BASE_SIZE)
        originX = width / 2f
        originY = height * ANCHOR
    }

    fun x(x: Float, z: Float): Float = originX + (x - z) * COS30 * scale

    fun y(x: Float, z: Float, altitude: Float): Float =
        originY + ((x + z) * SIN30 - (altitude - cameraY)) * scale

    /** Screen height of a box of world height [worldHeight]. */
    fun drop(worldHeight: Float): Float = worldHeight * scale
}
