package fr.damientrouillet.stacker.view

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import fr.damientrouillet.stacker.audio.SoundEngine
import fr.damientrouillet.stacker.data.ScoreStore
import fr.damientrouillet.stacker.game.GameSettings
import fr.damientrouillet.stacker.game.GameState
import fr.damientrouillet.stacker.game.Leaderboard
import fr.damientrouillet.stacker.game.StackGame
import fr.damientrouillet.stacker.render.GameRenderer
import fr.damientrouillet.stacker.render.UiRenderer
import fr.damientrouillet.stacker.ui.MenuLayout
import fr.damientrouillet.stacker.ui.Screen
import fr.damientrouillet.stacker.ui.UiAction
import java.util.concurrent.atomic.AtomicReference

/**
 * Hosts the game loop and the menu pages. Simulation and drawing both run on a
 * dedicated thread tied to the lifetime of the surface, so pausing the
 * activity pauses the game for free.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private companion object {
        /** Clamp for the frame delta, so a stall cannot teleport the plate. */
        const val MAX_DELTA = 0.05f
    }

    private val store = ScoreStore(context)
    private val game = StackGame(store.settings)
    private val renderer = GameRenderer(context)
    private val ui = UiRenderer(context)
    private val sound = SoundEngine(context)

    private val pendingTap = AtomicReference<FloatArray?>(null)
    private var loop: GameLoop? = null

    @Volatile
    private var screen: Screen = Screen.TITLE

    @Volatile
    private var settings: GameSettings = store.settings

    @Volatile
    private var leaderboard: Leaderboard = store.leaderboard

    /**
     * Menu geometry only changes with the viewport, so it is computed once per
     * surface change rather than on every frame and every tap.
     */
    @Volatile
    private var layouts: Layouts? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        sound.enabled = settings.soundEnabled
        game.best = leaderboard.best.coerceAtLeast(store.best)
        game.listener = object : StackGame.Listener {
            override fun onStarted() = Unit

            override fun onPlaced(perfect: Boolean, comboStep: Int) {
                sound.playPlace(if (perfect) comboStep else 0)
                if (perfect) vibrate(14, 90)
            }

            override fun onGameOver(score: Int, newBest: Boolean) {
                sound.playGameOver()
                vibrate(45, 140)
                leaderboard = store.record(score, System.currentTimeMillis())
                game.best = leaderboard.best
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            renderer.topInset = bars.top.toFloat()
            insets
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        loop = GameLoop().also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        layouts = Layouts(width.toFloat(), height.toFloat())
        renderer.setSize(width, height)
        ui.setSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        loop?.shutdown()
        loop = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            pendingTap.set(floatArrayOf(event.x, event.y))
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /**
     * Handles the system back gesture. Returns false only on the title page,
     * where the activity should be allowed to close.
     */
    fun goBack(): Boolean {
        if (screen == Screen.TITLE) return false
        returnToTitle()
        return true
    }

    /** Releases the audio resources. Call from the activity's onDestroy. */
    fun release() {
        loop?.shutdown()
        loop = null
        sound.release()
    }

    // -------------------------------------------------------------- input

    private fun handleTap(x: Float, y: Float) {
        val current = layouts ?: return

        when (screen) {
            Screen.TITLE -> when (MenuLayout.hit(current.title.buttons, x, y)) {
                UiAction.PLAY -> startGame()
                UiAction.OPEN_SETTINGS -> screen = Screen.SETTINGS
                UiAction.OPEN_LEADERBOARD -> screen = Screen.LEADERBOARD
                else -> Unit
            }

            Screen.SETTINGS -> when (MenuLayout.hit(current.settings.buttons, x, y)) {
                UiAction.TOGGLE_SOUND -> applySettings(settings.toggleSound())
                UiAction.SHRINK_PLATE -> applySettings(settings.withBaseSizeShifted(-1))
                UiAction.GROW_PLATE -> applySettings(settings.withBaseSizeShifted(1))
                UiAction.BACK -> screen = Screen.TITLE
                else -> Unit
            }

            Screen.LEADERBOARD ->
                if (MenuLayout.hit(current.leaderboard.buttons, x, y) == UiAction.BACK) {
                    screen = Screen.TITLE
                }

            Screen.GAME -> {
                val onMenuButton = game.state == GameState.OVER &&
                    game.canRestart() &&
                    MenuLayout.hit(current.gameOver.buttons, x, y) == UiAction.BACK
                if (onMenuButton) returnToTitle() else game.tap()
            }
        }
    }

    private fun startGame() {
        game.reset()
        game.tap()
        screen = Screen.GAME
    }

    private fun returnToTitle() {
        game.reset()
        screen = Screen.TITLE
    }

    private fun applySettings(updated: GameSettings) {
        settings = updated
        store.settings = updated
        sound.enabled = updated.soundEnabled
        game.settings = updated
        // The title page shows the pedestal, so a new plate size shows at once.
        if (game.state == GameState.READY) game.reset()
    }

    // ------------------------------------------------------------ drawing

    private fun render(canvas: Canvas, dt: Float) {
        renderer.draw(canvas, game, dt)
        val current = layouts ?: return
        when (screen) {
            Screen.TITLE -> ui.drawTitle(canvas, current.title, game.best)
            Screen.SETTINGS -> ui.drawSettings(canvas, current.settings, settings)
            Screen.LEADERBOARD -> ui.drawLeaderboard(canvas, current.leaderboard, leaderboard)
            Screen.GAME -> {
                renderer.drawHud(canvas, game)
                if (game.state == GameState.OVER && game.canRestart()) {
                    ui.drawGameOverMenu(canvas, current.gameOver, renderer.gameOverFade(game))
                }
            }
        }
    }

    /** Every menu page's geometry for one viewport size. */
    private class Layouts(width: Float, height: Float) {
        val title = MenuLayout.title(width, height)
        val settings = MenuLayout.settings(width, height)
        val leaderboard = MenuLayout.leaderboard(width, height)
        val gameOver = MenuLayout.gameOver(width, height)
    }

    private fun vibrate(durationMs: Long, amplitude: Int) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        runCatching {
            device.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        }
    }

    private inner class GameLoop : Thread("stacker-loop") {

        @Volatile
        private var running = true

        fun shutdown() {
            running = false
            while (true) {
                try {
                    join()
                    return
                } catch (interrupted: InterruptedException) {
                    currentThread().interrupt()
                }
            }
        }

        override fun run() {
            var previous = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val dt = ((now - previous) / 1_000_000_000f).coerceIn(0f, MAX_DELTA)
                previous = now

                pendingTap.getAndSet(null)?.let { handleTap(it[0], it[1]) }
                game.update(dt)

                val canvas = try {
                    holder.lockHardwareCanvas()
                } catch (error: IllegalStateException) {
                    null
                }
                if (canvas == null) {
                    // The surface is not ready yet; yield rather than spin.
                    try {
                        sleep(8)
                    } catch (interrupted: InterruptedException) {
                        currentThread().interrupt()
                    }
                    continue
                }
                try {
                    render(canvas, dt)
                } finally {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }
        }
    }
}
