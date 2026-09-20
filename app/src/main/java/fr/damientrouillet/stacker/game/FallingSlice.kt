package fr.damientrouillet.stacker.game

/** The overhanging chunk that is cut off a block and tumbles away. */
class FallingSlice(
    var cx: Float,
    var cz: Float,
    val sx: Float,
    val sz: Float,
    var y: Float,
    val height: Float,
    val colorIndex: Int,
    private val driftX: Float,
    private val driftZ: Float,
    val spin: Float
) {
    var velocityY: Float = 0f
    var rotation: Float = 0f

    fun update(dt: Float) {
        velocityY -= GameConfig.GRAVITY * dt
        y += velocityY * dt
        cx += driftX * dt
        cz += driftZ * dt
        rotation += spin * dt
    }
}
