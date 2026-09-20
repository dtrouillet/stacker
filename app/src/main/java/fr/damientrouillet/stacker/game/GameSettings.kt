package fr.damientrouillet.stacker.game

import kotlin.math.roundToInt

/**
 * Player controlled options. Immutable so a running game keeps the settings it
 * started with, and the next game picks up the new ones.
 */
data class GameSettings(
    val soundEnabled: Boolean = true,
    val baseSize: Float = GameConfig.DEFAULT_BASE_SIZE
) {

    companion object {
        const val MIN_BASE_SIZE = 0.6f
        const val MAX_BASE_SIZE = 1.4f
        const val BASE_SIZE_STEP = 0.2f

        /** Snaps [value] onto the nearest step inside the allowed range. */
        fun normaliseBaseSize(value: Float): Float {
            val clamped = value.coerceIn(MIN_BASE_SIZE, MAX_BASE_SIZE)
            val steps = ((clamped - MIN_BASE_SIZE) / BASE_SIZE_STEP).roundToInt()
            return (MIN_BASE_SIZE + steps * BASE_SIZE_STEP)
                .coerceIn(MIN_BASE_SIZE, MAX_BASE_SIZE)
        }

        /** Every selectable plate size, smallest first. */
        fun baseSizeChoices(): List<Float> {
            val choices = ArrayList<Float>()
            var steps = 0
            while (true) {
                val value = MIN_BASE_SIZE + steps * BASE_SIZE_STEP
                if (value > MAX_BASE_SIZE + 1e-4f) break
                choices.add(normaliseBaseSize(value))
                steps++
            }
            return choices
        }
    }

    init {
        require(baseSize > 0f) { "baseSize must be positive" }
    }

    /** The plate size as a percentage of the default, for display. */
    val baseSizePercent: Int get() = (baseSize / GameConfig.DEFAULT_BASE_SIZE * 100f).roundToInt()

    fun withSound(enabled: Boolean): GameSettings = copy(soundEnabled = enabled)

    fun toggleSound(): GameSettings = copy(soundEnabled = !soundEnabled)

    /** Moves the plate size by [steps] notches, stopping at the ends. */
    fun withBaseSizeShifted(steps: Int): GameSettings =
        copy(baseSize = normaliseBaseSize(baseSize + steps * BASE_SIZE_STEP))

    val canGrowBaseSize: Boolean get() = baseSize < MAX_BASE_SIZE - 1e-4f

    val canShrinkBaseSize: Boolean get() = baseSize > MIN_BASE_SIZE + 1e-4f
}
