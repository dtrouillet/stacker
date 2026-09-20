package fr.damientrouillet.stacker.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardTest {

    private fun boardOf(vararg scores: Int): Leaderboard {
        var board = Leaderboard.EMPTY
        scores.forEachIndexed { index, score -> board = board.submit(score, 1000L + index) }
        return board
    }

    @Test
    fun `a fresh table is empty`() {
        assertTrue(Leaderboard.EMPTY.isEmpty)
        assertEquals(0, Leaderboard.EMPTY.best)
    }

    @Test
    fun `scores are ranked highest first`() {
        val board = boardOf(4, 19, 7)
        assertEquals(listOf(19, 7, 4), board.entries.map { it.score })
        assertEquals(19, board.best)
    }

    @Test
    fun `a tie keeps the older run ahead`() {
        val board = Leaderboard.EMPTY.submit(12, 200L).submit(12, 100L)
        assertEquals(listOf(100L, 200L), board.entries.map { it.recordedAt })
    }

    @Test
    fun `the table never grows past its limit`() {
        var board = Leaderboard.EMPTY
        repeat(40) { board = board.submit(it + 1, it.toLong()) }
        assertEquals(Leaderboard.MAX_ENTRIES, board.entries.size)
        assertEquals(40, board.best)
        assertEquals(40 - Leaderboard.MAX_ENTRIES + 1, board.entries.last().score)
    }

    @Test
    fun `a score too low for a full table is refused`() {
        var board = Leaderboard.EMPTY
        repeat(Leaderboard.MAX_ENTRIES) { board = board.submit(100 + it, it.toLong()) }
        assertFalse(board.wouldRank(5))
        assertEquals(board.entries, board.submit(5, 9_999L).entries)
        assertTrue(board.wouldRank(500))
    }

    @Test
    fun `a blank game is not recorded`() {
        assertFalse(Leaderboard.EMPTY.wouldRank(0))
        assertTrue(Leaderboard.EMPTY.submit(0, 1L).isEmpty)
    }

    @Test
    fun `a submitted score can be located by position`() {
        val board = boardOf(4, 19, 7)
        assertEquals(1, board.positionOf(19, 1001L))
        assertEquals(3, board.positionOf(4, 1000L))
        assertNull(board.positionOf(19, 5L))
    }

    @Test
    fun `encoding round trips`() {
        val board = boardOf(4, 19, 7)
        val restored = Leaderboard.decode(board.encode())
        assertEquals(board.entries, restored.entries)
    }

    @Test
    fun `decoding survives anything`() {
        assertTrue(Leaderboard.decode(null).isEmpty)
        assertTrue(Leaderboard.decode("").isEmpty)
        assertTrue(Leaderboard.decode("   ").isEmpty)
        assertTrue(Leaderboard.decode("nonsense").isEmpty)
        assertTrue(Leaderboard.decode("12").isEmpty)
        assertTrue(Leaderboard.decode("x:y").isEmpty)
        assertTrue(Leaderboard.decode("-3:10").isEmpty)
        // A single good entry among the rubbish still comes through.
        assertEquals(listOf(8), Leaderboard.decode("oops;8:42;:;9").entries.map { it.score })
    }
}
