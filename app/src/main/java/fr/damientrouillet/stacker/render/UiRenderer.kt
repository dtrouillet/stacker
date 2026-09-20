package fr.damientrouillet.stacker.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import fr.damientrouillet.stacker.R
import fr.damientrouillet.stacker.game.GameSettings
import fr.damientrouillet.stacker.game.Leaderboard
import fr.damientrouillet.stacker.game.Palette
import fr.damientrouillet.stacker.ui.GameOverLayout
import fr.damientrouillet.stacker.ui.LeaderboardLayout
import fr.damientrouillet.stacker.ui.MenuLayout
import fr.damientrouillet.stacker.ui.SettingsLayout
import fr.damientrouillet.stacker.ui.TitleLayout
import fr.damientrouillet.stacker.ui.UiButton

/**
 * Draws the menu pages over the tower. Geometry comes from [MenuLayout], so
 * what is drawn and what answers a tap can never drift apart.
 */
class UiRenderer(context: Context) {

    private companion object {
        const val SCRIM_ALPHA = 165
        const val DISABLED_ALPHA = 70
    }

    private var width = 0f
    private var height = 0f

    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
    }
    private val rect = RectF()

    private val labelTitle = context.getString(R.string.title)
    private val labelPlay = context.getString(R.string.play)
    private val labelSettings = context.getString(R.string.settings)
    private val labelLeaderboard = context.getString(R.string.leaderboard)
    private val labelBack = context.getString(R.string.back)
    private val labelBest = context.getString(R.string.best)
    private val labelSound = context.getString(R.string.sound)
    private val labelOn = context.getString(R.string.on)
    private val labelOff = context.getString(R.string.off)
    private val labelPlateSize = context.getString(R.string.plate_size)
    private val labelNoScores = context.getString(R.string.no_scores)
    private val labelMenu = context.getString(R.string.menu)

    fun setSize(widthPx: Int, heightPx: Int) {
        width = widthPx.toFloat()
        height = heightPx.toFloat()
    }

    fun drawTitle(canvas: Canvas, layout: TitleLayout, best: Int) {
        scrim(canvas)

        centered(canvas, labelTitle, width * 0.135f, layout.titleY, 245, 0.28f)
        if (best > 0) {
            centered(canvas, "$labelBest $best", width * 0.042f, layout.bestY, 150, 0.16f)
        }

        button(canvas, layout.play, labelPlay, enabled = true)
        button(canvas, layout.settings, labelSettings, enabled = true)
        button(canvas, layout.leaderboard, labelLeaderboard, enabled = true)
    }

    fun drawSettings(canvas: Canvas, layout: SettingsLayout, settings: GameSettings) {
        scrim(canvas)
        centered(canvas, labelSettings, width * 0.085f, layout.headerY, 245, 0.22f)

        val soundValue = if (settings.soundEnabled) labelOn else labelOff
        button(canvas, layout.sound, "$labelSound   $soundValue", enabled = true)

        centered(
            canvas,
            labelPlateSize,
            width * 0.04f,
            layout.plateRowCenterY - height * 0.05f,
            150,
            0.16f
        )
        centered(
            canvas,
            "${settings.baseSizePercent}%",
            width * 0.075f,
            layout.plateRowCenterY + width * 0.026f,
            235,
            0f
        )
        button(canvas, layout.shrink, "−", enabled = settings.canShrinkBaseSize)
        button(canvas, layout.grow, "+", enabled = settings.canGrowBaseSize)

        button(canvas, layout.back, labelBack, enabled = true)
    }

    fun drawLeaderboard(canvas: Canvas, layout: LeaderboardLayout, board: Leaderboard) {
        scrim(canvas)
        centered(canvas, labelLeaderboard, width * 0.075f, layout.headerY, 245, 0.22f)

        if (board.isEmpty) {
            centered(canvas, labelNoScores, width * 0.042f, height * 0.42f, 150, 0.16f)
        } else {
            val rows = minOf(MenuLayout.visibleRows(layout), board.entries.size)
            val size = width * 0.05f
            for (index in 0 until rows) {
                val entry = board.entries[index]
                val y = layout.rowY(index)
                val alpha = if (index == 0) 245 else 185
                text.letterSpacing = 0.05f
                text.textSize = size
                text.alpha = alpha
                text.textAlign = Paint.Align.LEFT
                canvas.drawText("${index + 1}", width * 0.24f, y, text)
                text.textAlign = Paint.Align.RIGHT
                canvas.drawText(entry.score.toString(), width * 0.76f, y, text)
                text.textAlign = Paint.Align.CENTER
            }
        }

        button(canvas, layout.back, labelBack, enabled = true)
    }

    /** The extra button the game over panel shows, on top of what GameRenderer drew. */
    fun drawGameOverMenu(canvas: Canvas, layout: GameOverLayout, fade: Float) {
        val alpha = (255 * fade.coerceIn(0f, 1f)).toInt()
        button(canvas, layout.menu, labelMenu, enabled = true, alphaScale = alpha / 255f)
    }

    private fun scrim(canvas: Canvas) {
        canvas.drawColor(Palette.withAlpha(Color.BLACK, SCRIM_ALPHA))
    }

    private fun button(
        canvas: Canvas,
        target: UiButton,
        label: String,
        enabled: Boolean,
        alphaScale: Float = 1f
    ) {
        val base = if (enabled) 220 else DISABLED_ALPHA
        val alpha = (base * alphaScale).toInt().coerceIn(0, 255)
        rect.set(target.left, target.top, target.right, target.bottom)
        val radius = target.height / 2f
        outline.alpha = (alpha * 0.55f).toInt().coerceIn(0, 255)
        outline.strokeWidth = maxOf(1.5f, width * 0.0035f)
        canvas.drawRoundRect(rect, radius, radius, outline)

        text.letterSpacing = 0.16f
        text.textSize = target.height * 0.36f
        text.alpha = alpha
        // Centre the label on the button rather than on its baseline.
        val metrics = text.fontMetrics
        val baseline = target.centerY - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(label, target.centerX, baseline, text)
    }

    private fun centered(
        canvas: Canvas,
        label: String,
        size: Float,
        y: Float,
        alpha: Int,
        spacing: Float
    ) {
        text.letterSpacing = spacing
        text.textSize = size
        text.alpha = alpha.coerceIn(0, 255)
        canvas.drawText(label, width / 2f, y, text)
    }
}
