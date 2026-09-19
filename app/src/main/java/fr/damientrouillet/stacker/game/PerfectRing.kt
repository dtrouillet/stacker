package fr.damientrouillet.stacker.game

/** The expanding white outline emitted by a perfect placement. */
class PerfectRing(
    val cx: Float,
    val cz: Float,
    val sx: Float,
    val sz: Float,
    val y: Float
) {
    /** Progress from 0 (just emitted) to 1 (faded out). */
    var progress: Float = 0f

    fun update(dt: Float) {
        progress = (progress + dt / GameConfig.RING_LIFE).coerceAtMost(1f)
    }

    val done: Boolean get() = progress >= 1f
}
