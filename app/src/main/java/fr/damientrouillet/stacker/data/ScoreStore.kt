package fr.damientrouillet.stacker.data

import android.content.Context
import fr.damientrouillet.stacker.game.GameSettings
import fr.damientrouillet.stacker.game.Leaderboard

/** Persists the high score table and the player's options. */
class ScoreStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("stacker", Context.MODE_PRIVATE)

    var leaderboard: Leaderboard
        get() = Leaderboard.decode(prefs.getString(KEY_LEADERBOARD, null))
        set(value) = prefs.edit().putString(KEY_LEADERBOARD, value.encode()).apply()

    var settings: GameSettings
        get() = GameSettings(
            soundEnabled = prefs.getBoolean(KEY_SOUND, true),
            baseSize = GameSettings.normaliseBaseSize(
                prefs.getFloat(KEY_BASE_SIZE, GameSettings().baseSize)
            )
        )
        set(value) = prefs.edit()
            .putBoolean(KEY_SOUND, value.soundEnabled)
            .putFloat(KEY_BASE_SIZE, value.baseSize)
            .apply()

    /**
     * The best score. Older versions stored it on its own, so it is read back
     * once and folded into the table the first time this runs.
     */
    val best: Int
        get() {
            val table = leaderboard
            return maxOf(table.best, prefs.getInt(KEY_LEGACY_BEST, 0))
        }

    /** Records a finished game and returns the table it produced. */
    fun record(score: Int, recordedAt: Long): Leaderboard {
        val updated = leaderboard.submit(score, recordedAt)
        leaderboard = updated
        return updated
    }

    private companion object {
        const val KEY_LEADERBOARD = "leaderboard"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_BASE_SIZE = "base_size"
        const val KEY_LEGACY_BEST = "best_score"
    }
}
