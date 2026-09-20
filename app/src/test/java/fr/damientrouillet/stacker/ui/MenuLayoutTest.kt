package fr.damientrouillet.stacker.ui

import fr.damientrouillet.stacker.game.Leaderboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuLayoutTest {

    private val width = 1080f
    private val height = 2160f

    private fun overlaps(a: UiButton, b: UiButton): Boolean =
        a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

    private fun insideViewport(button: UiButton): Boolean =
        button.left >= 0f && button.top >= 0f && button.right <= width && button.bottom <= height

    @Test
    fun `a tap on a button returns its action`() {
        val layout = MenuLayout.title(width, height)
        assertEquals(
            UiAction.PLAY,
            MenuLayout.hit(layout.buttons, layout.play.centerX, layout.play.centerY)
        )
        assertEquals(
            UiAction.OPEN_SETTINGS,
            MenuLayout.hit(layout.buttons, layout.settings.centerX, layout.settings.centerY)
        )
        assertEquals(
            UiAction.OPEN_LEADERBOARD,
            MenuLayout.hit(layout.buttons, layout.leaderboard.centerX, layout.leaderboard.centerY)
        )
    }

    @Test
    fun `a tap on empty space returns nothing`() {
        val layout = MenuLayout.title(width, height)
        assertNull(MenuLayout.hit(layout.buttons, width / 2f, 0f))
        assertNull(MenuLayout.hit(layout.buttons, 0f, layout.play.centerY))
        assertNull(MenuLayout.hit(layout.buttons, width / 2f, height))
    }

    @Test
    fun `the edges of a button count as hits and just outside does not`() {
        val play = MenuLayout.title(width, height).play
        assertTrue(play.contains(play.left, play.top))
        assertTrue(play.contains(play.right, play.bottom))
        assertFalse(play.contains(play.left - 1f, play.centerY))
        assertFalse(play.contains(play.centerX, play.bottom + 1f))
    }

    @Test
    fun `title buttons are stacked, separated and on screen`() {
        val layout = MenuLayout.title(width, height)
        assertTrue(layout.play.bottom < layout.settings.top)
        assertTrue(layout.settings.bottom < layout.leaderboard.top)
        assertTrue(layout.titleY < layout.bestY)
        assertTrue(layout.bestY < layout.play.top)
        for (button in layout.buttons) assertTrue(insideViewport(button))
    }

    @Test
    fun `settings controls never overlap`() {
        val layout = MenuLayout.settings(width, height)
        val buttons = layout.buttons
        for (i in buttons.indices) {
            for (j in i + 1 until buttons.size) {
                assertFalse(
                    "${buttons[i].action} overlaps ${buttons[j].action}",
                    overlaps(buttons[i], buttons[j])
                )
            }
        }
        for (button in buttons) assertTrue(insideViewport(button))
    }

    @Test
    fun `the plate steppers sit either side of the row centre`() {
        val layout = MenuLayout.settings(width, height)
        assertTrue(layout.shrink.right < width / 2f)
        assertTrue(layout.grow.left > width / 2f)
        assertEquals(layout.plateRowCenterY, layout.shrink.centerY, 0.01f)
        assertEquals(layout.plateRowCenterY, layout.grow.centerY, 0.01f)
        assertEquals(width / 2f - layout.shrink.centerX, layout.grow.centerX - width / 2f, 0.01f)
    }

    @Test
    fun `each settings control answers its own action`() {
        val layout = MenuLayout.settings(width, height)
        for (button in layout.buttons) {
            assertEquals(
                button.action,
                MenuLayout.hit(layout.buttons, button.centerX, button.centerY)
            )
        }
    }

    @Test
    fun `the leaderboard shows rows without running into the back button`() {
        val layout = MenuLayout.leaderboard(width, height)
        val rows = MenuLayout.visibleRows(layout)
        assertTrue("expected room for the whole table, got $rows", rows >= Leaderboard.MAX_ENTRIES)
        assertTrue(layout.rowY(rows - 1) < layout.back.top)
        assertTrue(layout.headerY < layout.firstRowY)
    }

    @Test
    fun `a short screen shows fewer rows rather than drawing over the button`() {
        val layout = MenuLayout.leaderboard(720f, 640f)
        val rows = MenuLayout.visibleRows(layout)
        assertTrue(rows >= 0)
        assertTrue(rows <= Leaderboard.MAX_ENTRIES)
        if (rows > 0) assertTrue(layout.rowY(rows - 1) < layout.back.top)
    }

    @Test
    fun `the game over panel offers a way back below the replay hint`() {
        val layout = MenuLayout.gameOver(width, height)
        assertEquals(UiAction.BACK, layout.menu.action)
        assertTrue(insideViewport(layout.menu))
        // Below the replay hint the renderer draws at 0.82 of the height.
        assertTrue(layout.menu.top > height * 0.82f)
        assertEquals(
            UiAction.BACK,
            MenuLayout.hit(layout.buttons, layout.menu.centerX, layout.menu.centerY)
        )
        assertNull(MenuLayout.hit(layout.buttons, width / 2f, height * 0.5f))
    }

    @Test
    fun `the layout follows the viewport size`() {
        val narrow = MenuLayout.title(720f, 1280f)
        val wide = MenuLayout.title(1440f, 1280f)
        assertEquals(2f, wide.play.width / narrow.play.width, 0.01f)
        assertEquals(360f, narrow.play.centerX, 0.01f)
        assertEquals(720f, wide.play.centerX, 0.01f)
    }
}
