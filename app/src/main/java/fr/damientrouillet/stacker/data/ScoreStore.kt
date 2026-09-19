package fr.damientrouillet.stacker.data

import android.content.Context

/** Persists the best score across sessions. */
class ScoreStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("stacker", Context.MODE_PRIVATE)

    var best: Int
        get() = prefs.getInt(KEY_BEST, 0)
        set(value) {
            if (value > best) prefs.edit().putInt(KEY_BEST, value).apply()
        }

    private companion object {
        const val KEY_BEST = "best_score"
    }
}
