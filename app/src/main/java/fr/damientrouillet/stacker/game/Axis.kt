package fr.damientrouillet.stacker.game

/** The horizontal axis a block travels along. Consecutive blocks alternate. */
enum class Axis {
    X,
    Z;

    fun other(): Axis = if (this == X) Z else X
}
