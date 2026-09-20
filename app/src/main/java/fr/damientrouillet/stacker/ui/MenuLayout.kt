package fr.damientrouillet.stacker.ui

import fr.damientrouillet.stacker.game.Leaderboard

/** Buttons and anchor lines of the title page. */
data class TitleLayout(
    val play: UiButton,
    val settings: UiButton,
    val leaderboard: UiButton,
    val titleY: Float,
    val bestY: Float
) {
    val buttons: List<UiButton> get() = listOf(play, settings, leaderboard)
}

/** Buttons and anchor lines of the settings page. */
data class SettingsLayout(
    val sound: UiButton,
    val shrink: UiButton,
    val grow: UiButton,
    val back: UiButton,
    val headerY: Float,
    val plateRowCenterY: Float
) {
    val buttons: List<UiButton> get() = listOf(sound, shrink, grow, back)
}

/** Buttons and anchor lines of the leaderboard page. */
data class LeaderboardLayout(
    val back: UiButton,
    val headerY: Float,
    val firstRowY: Float,
    val rowSpacing: Float
) {
    val buttons: List<UiButton> get() = listOf(back)

    fun rowY(index: Int): Float = firstRowY + index * rowSpacing
}

/** The one button the game over panel offers besides tapping to replay. */
data class GameOverLayout(val menu: UiButton) {
    val buttons: List<UiButton> get() = listOf(menu)
}

/**
 * Turns a viewport size into menu geometry. Kept free of Android types so the
 * drawing code and the hit testing read from the same numbers, and so those
 * numbers can be unit tested.
 */
object MenuLayout {

    private const val BUTTON_WIDTH_RATIO = 0.66f
    private const val BUTTON_HEIGHT_RATIO = 0.078f
    private const val STEPPER_SIZE_RATIO = 0.13f

    fun title(width: Float, height: Float): TitleLayout {
        val first = height * 0.46f
        val gap = height * 0.105f
        return TitleLayout(
            play = wideButton(UiAction.PLAY, width, height, first),
            settings = wideButton(UiAction.OPEN_SETTINGS, width, height, first + gap),
            leaderboard = wideButton(UiAction.OPEN_LEADERBOARD, width, height, first + 2 * gap),
            titleY = height * 0.26f,
            bestY = height * 0.32f
        )
    }

    fun settings(width: Float, height: Float): SettingsLayout {
        val soundRowCenter = height * 0.36f
        val plateRowCenter = height * 0.5f
        val stepper = width * STEPPER_SIZE_RATIO
        val stepperHalf = stepper / 2f
        val stepperOffset = width * 0.3f
        return SettingsLayout(
            sound = wideButton(UiAction.TOGGLE_SOUND, width, height, soundRowCenter),
            shrink = UiButton(
                UiAction.SHRINK_PLATE,
                left = width / 2f - stepperOffset - stepperHalf,
                top = plateRowCenter - stepperHalf,
                right = width / 2f - stepperOffset + stepperHalf,
                bottom = plateRowCenter + stepperHalf
            ),
            grow = UiButton(
                UiAction.GROW_PLATE,
                left = width / 2f + stepperOffset - stepperHalf,
                top = plateRowCenter - stepperHalf,
                right = width / 2f + stepperOffset + stepperHalf,
                bottom = plateRowCenter + stepperHalf
            ),
            back = wideButton(UiAction.BACK, width, height, height * 0.8f),
            headerY = height * 0.2f,
            plateRowCenterY = plateRowCenter
        )
    }

    fun leaderboard(width: Float, height: Float): LeaderboardLayout {
        val firstRow = height * 0.29f
        val spacing = height * 0.048f
        return LeaderboardLayout(
            back = wideButton(UiAction.BACK, width, height, height * 0.86f),
            headerY = height * 0.2f,
            firstRowY = firstRow,
            rowSpacing = spacing
        )
    }

    fun gameOver(width: Float, height: Float): GameOverLayout =
        GameOverLayout(menu = wideButton(UiAction.BACK, width, height, height * 0.9f))

    /** The action under the given point, or null when the tap missed everything. */
    fun hit(buttons: List<UiButton>, x: Float, y: Float): UiAction? =
        buttons.firstOrNull { it.contains(x, y) }?.action

    /** Rows the leaderboard page can show without running into the back button. */
    fun visibleRows(layout: LeaderboardLayout): Int {
        val available = layout.back.top - layout.firstRowY
        if (available <= 0f || layout.rowSpacing <= 0f) return 0
        return minOf(Leaderboard.MAX_ENTRIES, (available / layout.rowSpacing).toInt())
    }

    private fun wideButton(
        action: UiAction,
        width: Float,
        height: Float,
        centerY: Float
    ): UiButton {
        val halfWidth = width * BUTTON_WIDTH_RATIO / 2f
        val halfHeight = height * BUTTON_HEIGHT_RATIO / 2f
        return UiButton(
            action,
            left = width / 2f - halfWidth,
            top = centerY - halfHeight,
            right = width / 2f + halfWidth,
            bottom = centerY + halfHeight
        )
    }
}
