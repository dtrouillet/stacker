package fr.damientrouillet.stacker.game

enum class GameState {
    /** Title screen, waiting for the first tap. */
    READY,

    /** A block is sliding over the tower. */
    RUNNING,

    /** The last block missed the tower. */
    OVER
}
