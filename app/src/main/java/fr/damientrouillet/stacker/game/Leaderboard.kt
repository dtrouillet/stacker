package fr.damientrouillet.stacker.game

/** One finished game worth remembering. */
data class ScoreEntry(val score: Int, val recordedAt: Long)

/**
 * The local high score table. Immutable: submitting a score returns the new
 * table, which makes the ranking rules straightforward to test.
 */
class Leaderboard private constructor(val entries: List<ScoreEntry>) {

    companion object {
        const val MAX_ENTRIES = 10

        private const val ENTRY_SEPARATOR = ";"
        private const val FIELD_SEPARATOR = ":"

        val EMPTY = Leaderboard(emptyList())

        fun of(entries: List<ScoreEntry>): Leaderboard = Leaderboard(rank(entries))

        /** Reads back [encode]. Anything unparseable is dropped rather than thrown. */
        fun decode(text: String?): Leaderboard {
            if (text.isNullOrBlank()) return EMPTY
            val parsed = text.split(ENTRY_SEPARATOR).mapNotNull { chunk ->
                val fields = chunk.split(FIELD_SEPARATOR)
                if (fields.size != 2) return@mapNotNull null
                val score = fields[0].trim().toIntOrNull() ?: return@mapNotNull null
                val at = fields[1].trim().toLongOrNull() ?: return@mapNotNull null
                if (score < 0 || at < 0) null else ScoreEntry(score, at)
            }
            return of(parsed)
        }

        /** Highest score first; a tie is settled in favour of the older run. */
        private fun rank(entries: List<ScoreEntry>): List<ScoreEntry> = entries
            .sortedWith(compareByDescending<ScoreEntry> { it.score }.thenBy { it.recordedAt })
            .take(MAX_ENTRIES)
    }

    val best: Int get() = entries.firstOrNull()?.score ?: 0

    val isEmpty: Boolean get() = entries.isEmpty()

    /** True when a score of [score] would earn a place on the table. */
    fun wouldRank(score: Int): Boolean {
        if (score <= 0) return false
        if (entries.size < MAX_ENTRIES) return true
        return score > entries.last().score
    }

    /** Returns the table with [score] added, or this table when it does not rank. */
    fun submit(score: Int, recordedAt: Long): Leaderboard {
        if (!wouldRank(score)) return this
        return of(entries + ScoreEntry(score, recordedAt))
    }

    /** The 1 based position of the entry recorded at [recordedAt], or null. */
    fun positionOf(score: Int, recordedAt: Long): Int? {
        val index = entries.indexOfFirst { it.score == score && it.recordedAt == recordedAt }
        return if (index < 0) null else index + 1
    }

    fun encode(): String = entries.joinToString(ENTRY_SEPARATOR) {
        "${it.score}$FIELD_SEPARATOR${it.recordedAt}"
    }
}
