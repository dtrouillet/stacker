package fr.damientrouillet.stacker.view

import android.content.Context
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
import fr.damientrouillet.stacker.game.StackGame
import fr.damientrouillet.stacker.render.GameRenderer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hosts the game loop. Simulation and drawing both run on a dedicated thread
 * that is tied to the lifetime of the surface, so pausing the activity pauses
 * the game for free.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    private companion object {
        /** Clamp for the frame delta, so a stall cannot teleport the block. */
        const val MAX_DELTA = 0.05f
    }

    private val game = StackGame()
    private val renderer = GameRenderer(context)
    private val sound = SoundEngine(context)
    private val store = ScoreStore(context)
    private val tapPending = AtomicBoolean(false)
    private var loop: GameLoop? = null

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
        game.best = store.best
        game.listener = object : StackGame.Listener {
            override fun onStarted() = Unit

            override fun onPlaced(perfect: Boolean, comboStep: Int) {
                sound.playPlace(if (perfect) comboStep else 0)
                if (perfect) vibrate(14, 90)
            }

            override fun onGameOver(score: Int, newBest: Boolean) {
                sound.playGameOver()
                vibrate(45, 140)
                store.best = score
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
        renderer.setSize(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        loop?.shutdown()
        loop = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            tapPending.set(true)
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** Releases the audio resources. Call from the activity's onDestroy. */
    fun release() {
        loop?.shutdown()
        loop = null
        sound.release()
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

                if (tapPending.getAndSet(false)) game.tap()
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
                    renderer.draw(canvas, game, dt)
                } finally {
                    runCatching { holder.unlockCanvasAndPost(canvas) }
                }
            }
        }
    }
}
