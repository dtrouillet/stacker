package fr.damientrouillet.stacker.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import fr.damientrouillet.stacker.R
import fr.damientrouillet.stacker.game.Block
import fr.damientrouillet.stacker.game.FallingSlice
import fr.damientrouillet.stacker.game.GameConfig
import fr.damientrouillet.stacker.game.GameState
import fr.damientrouillet.stacker.game.IsoProjection
import fr.damientrouillet.stacker.game.Palette
import fr.damientrouillet.stacker.game.PerfectRing
import fr.damientrouillet.stacker.game.StackGame
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the tower with a true isometric projection: the x axis runs towards the
 * lower right, the z axis towards the lower left and y straight up. Every block
 * is three quads, the lit top face plus the two visible sides.
 */
class GameRenderer(context: Context) {

    private companion object {
        const val CULL_MARGIN = 220f
    }

    /** Top window inset, so the score clears a display cutout. */
    var topInset: Float = 0f

    private var width = 0f
    private var height = 0f
    private var time = 0f
    private val projection = IsoProjection()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val background = Paint()
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val path = Path()
    private val sortedSlices = ArrayList<FallingSlice>(8)
    private val byAltitude = Comparator<FallingSlice> { a, b -> a.y.compareTo(b.y) }

    private var gradientIndex = Int.MIN_VALUE
    private var gradientHeight = -1f

    private val labelTitle = context.getString(R.string.title)
    private val labelStart = context.getString(R.string.tap_to_start)
    private val labelGameOver = context.getString(R.string.game_over)
    private val labelBest = context.getString(R.string.best)
    private val labelRetry = context.getString(R.string.tap_to_retry)

    fun setSize(widthPx: Int, heightPx: Int) {
        width = widthPx.toFloat()
        height = heightPx.toFloat()
        projection.configure(width, height)
        gradientHeight = -1f
    }

    fun draw(canvas: Canvas, game: StackGame, dt: Float) {
        if (width <= 0f || height <= 0f) return
        time += dt
        projection.cameraY = game.cameraY

        drawBackground(canvas, game)
        drawScene(canvas, game)
        drawHud(canvas, game)
    }

    // ---------------------------------------------------------------- scene

    private fun drawScene(canvas: Canvas, game: StackGame) {
        // Painter's algorithm: lower boxes first. Blocks are already ordered, so
        // only the falling slices have to be merged back in by altitude.
        var sliceIndex = 0
        val slices = sortedSlices
        slices.clear()
        slices.addAll(game.slices)
        if (slices.size > 1) slices.sortWith(byAltitude)
        for (block in game.blocks) {
            while (sliceIndex < slices.size && slices[sliceIndex].y < block.y) {
                drawSlice(canvas, slices[sliceIndex])
                sliceIndex++
            }
            drawBlock(canvas, block)
        }
        while (sliceIndex < slices.size) {
            drawSlice(canvas, slices[sliceIndex])
            sliceIndex++
        }
        game.moving?.let { drawBlock(canvas, it) }
        for (ring in game.rings) drawRing(canvas, ring)
    }

    private fun drawBlock(canvas: Canvas, block: Block) {
        val squash = squashOf(block)
        drawBox(
            canvas,
            block.cx, block.cz,
            block.sx, block.sz,
            block.y, block.height * squash,
            Palette.blockColor(block.colorIndex),
            255,
            0f
        )
    }

    /** A landing block briefly squashes down and springs back, like the original. */
    private fun squashOf(block: Block): Float {
        if (block.placeAnim <= 0f) return 1f
        val progress = 1f - block.placeAnim / GameConfig.PLACE_ANIM
        return 1f - 0.26f * sin(progress * PI).toFloat()
    }

    private fun drawSlice(canvas: Canvas, slice: FallingSlice) {
        val alpha = (slice.alpha * 255f).toInt().coerceIn(0, 255)
        if (alpha == 0) return
        drawBox(
            canvas,
            slice.cx, slice.cz,
            slice.sx, slice.sz,
            slice.y, slice.height,
            Palette.blockColor(slice.colorIndex),
            alpha,
            slice.rotation
        )
    }

    private fun drawRing(canvas: Canvas, ring: PerfectRing) {
        val progress = ring.progress
        val grow = 1f + progress * 1.1f
        val alpha = ((1f - progress) * 190f).toInt().coerceIn(0, 255)
        if (alpha == 0) return
        val sx = ring.sx * grow
        val sz = ring.sz * grow
        val x0 = ring.cx - sx / 2f
        val x1 = ring.cx + sx / 2f
        val z0 = ring.cz - sz / 2f
        val z1 = ring.cz + sz / 2f

        path.reset()
        path.moveTo(projectX(x0, z0), projectY(x0, z0, ring.y))
        path.lineTo(projectX(x1, z0), projectY(x1, z0, ring.y))
        path.lineTo(projectX(x1, z1), projectY(x1, z1, ring.y))
        path.lineTo(projectX(x0, z1), projectY(x0, z1, ring.y))
        path.close()

        stroke.color = Color.WHITE
        stroke.alpha = alpha
        stroke.strokeWidth = max(1.5f, width * 0.009f * (1f - progress))
        canvas.drawPath(path, stroke)
    }

    /**
     * Draws one cuboid. [rotation] spins the projected shape around its own
     * centre, which is enough to sell a tumbling slice.
     */
    private fun drawBox(
        canvas: Canvas,
        cx: Float,
        cz: Float,
        sx: Float,
        sz: Float,
        y: Float,
        boxHeight: Float,
        color: Int,
        alpha: Int,
        rotation: Float
    ) {
        val x0 = cx - sx / 2f
        val x1 = cx + sx / 2f
        val z0 = cz - sz / 2f
        val z1 = cz + sz / 2f
        val yTop = y + boxHeight

        // Top face corners. Projected y only depends on altitude through a
        // constant offset, so the bottom corners are simply drop pixels lower.
        val ax = projectX(x0, z0); val ay = projectY(x0, z0, yTop)
        val bx = projectX(x1, z0); val by = projectY(x1, z0, yTop)
        val cxp = projectX(x1, z1); val cyp = projectY(x1, z1, yTop)
        val dx = projectX(x0, z1); val dy = projectY(x0, z1, yTop)
        val drop = projection.drop(boxHeight)

        if (cyp + drop < -CULL_MARGIN || ay > height + CULL_MARGIN) return

        val rotating = rotation != 0f
        if (rotating) {
            canvas.save()
            canvas.rotate(rotation, (ax + cxp) / 2f, (ay + cyp + drop) / 2f)
        }

        // Face pointing towards +z, on the left of the screen and least lit.
        quad(canvas, dx, dy, cxp, cyp, cxp, cyp + drop, dx, dy + drop,
            Palette.shade(color, Palette.SHADE_LEFT), alpha)
        // Face pointing towards +x, on the right of the screen.
        quad(canvas, bx, by, cxp, cyp, cxp, cyp + drop, bx, by + drop,
            Palette.shade(color, Palette.SHADE_RIGHT), alpha)
        // Lit top face.
        quad(canvas, ax, ay, bx, by, cxp, cyp, dx, dy, color, alpha)

        if (rotating) canvas.restore()
    }

    private fun quad(
        canvas: Canvas,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        x3: Float, y3: Float,
        x4: Float, y4: Float,
        color: Int,
        alpha: Int
    ) {
        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.lineTo(x4, y4)
        path.close()
        fill.color = color
        fill.alpha = alpha
        canvas.drawPath(path, fill)
    }

    private fun projectX(x: Float, z: Float): Float = projection.x(x, z)

    private fun projectY(x: Float, z: Float, y: Float): Float = projection.y(x, z, y)

    // ------------------------------------------------------------------ hud

    private fun drawBackground(canvas: Canvas, game: StackGame) {
        val index = game.colorIndex
        if (index != gradientIndex || gradientHeight != height) {
            gradientIndex = index
            gradientHeight = height
            background.shader = LinearGradient(
                0f, 0f, 0f, height,
                Palette.backgroundTop(index),
                Palette.backgroundBottom(index),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width, height, background)
    }

    private fun drawHud(canvas: Canvas, game: StackGame) {
        when (game.state) {
            GameState.READY -> drawTitle(canvas, game)
            GameState.RUNNING -> drawScore(canvas, game)
            GameState.OVER -> drawGameOver(canvas, game)
        }
    }

    private fun drawScore(canvas: Canvas, game: StackGame) {
        text.letterSpacing = 0f
        text.textSize = width * 0.155f
        text.alpha = 235
        canvas.drawText(
            game.score.toString(),
            width / 2f,
            max(topInset, height * 0.04f) + text.textSize,
            text
        )
    }

    private fun drawTitle(canvas: Canvas, game: StackGame) {
        text.letterSpacing = 0.28f
        text.textSize = width * 0.135f
        text.alpha = 245
        canvas.drawText(labelTitle, width / 2f, height * 0.29f, text)

        if (game.best > 0) {
            text.letterSpacing = 0.16f
            text.textSize = width * 0.042f
            text.alpha = 150
            canvas.drawText(
                "$labelBest ${game.best}",
                width / 2f,
                height * 0.35f,
                text
            )
        }

        drawPulsingHint(canvas, labelStart, height * 0.43f)
    }

    private fun drawGameOver(canvas: Canvas, game: StackGame) {
        val fade = min(1f, game.timeSinceGameOver() / 0.45f)
        canvas.drawColor(Palette.withAlpha(Color.BLACK, (170f * fade).toInt()))

        text.letterSpacing = 0.22f
        text.textSize = width * 0.085f
        text.alpha = (245 * fade).toInt()
        canvas.drawText(labelGameOver, width / 2f, height * 0.38f, text)

        text.letterSpacing = 0f
        text.textSize = width * 0.24f
        canvas.drawText(game.score.toString(), width / 2f, height * 0.54f, text)

        text.letterSpacing = 0.16f
        text.textSize = width * 0.045f
        text.alpha = (165 * fade).toInt()
        canvas.drawText("$labelBest ${game.best}", width / 2f, height * 0.60f, text)

        if (game.canRestart()) drawPulsingHint(canvas, labelRetry, height * 0.82f)
    }

    private fun drawPulsingHint(canvas: Canvas, label: String, y: Float) {
        val pulse = 0.55f + 0.45f * sin(time * 2.6f)
        text.letterSpacing = 0.2f
        text.textSize = width * 0.045f
        text.alpha = (120 + 110 * pulse).toInt().coerceIn(0, 255)
        canvas.drawText(label, width / 2f, y, text)
    }
}
