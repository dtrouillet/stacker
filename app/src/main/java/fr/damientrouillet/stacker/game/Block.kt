package fr.damientrouillet.stacker.game

/**
 * An axis aligned box. [cx] / [cz] are the centre on the horizontal plane,
 * [sx] / [sz] its footprint, and [y] the altitude of its underside.
 */
class Block(
    var cx: Float,
    var cz: Float,
    var sx: Float,
    var sz: Float,
    var y: Float,
    val height: Float = GameConfig.BLOCK_HEIGHT,
    val colorIndex: Int = 0
) {
    /** Remaining time of the landing squash animation, in seconds. */
    var placeAnim: Float = 0f

    val top: Float get() = y + height

    fun center(axis: Axis): Float = if (axis == Axis.X) cx else cz

    fun size(axis: Axis): Float = if (axis == Axis.X) sx else sz

    fun setCenter(axis: Axis, value: Float) {
        if (axis == Axis.X) cx = value else cz = value
    }

    fun setSize(axis: Axis, value: Float) {
        if (axis == Axis.X) sx = value else sz = value
    }
}
