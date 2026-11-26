package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CricketScorerTest {

    private fun ball(
        runs: Int = 0,
        extraRuns: Int = 0,
        extraType: String? = null,
        wicket: Boolean = false,
        wicketType: String? = null,
        striker: String = "A",
        nonStriker: String = "B",
        bowler: String = "X"
    ) = BallInfo(
        overIndex = 0, ballNumber = 1, runs = runs, extraRuns = extraRuns,
        isExtra = extraType != null, extraType = extraType, isWicket = wicket, wicketType = wicketType,
        striker = striker, nonStriker = nonStriker, bowler = bowler,
        dismissedPlayer = if (wicket) striker else null, innings = 1
    )

    @Test
    fun wideWithRunIsCountedOnce() {
        // Wide where the batters ran one: 2 extras in total, nothing to the batter
        val card = CricketScorer.calculateInningsScorecard(listOf(ball(runs = 0, extraRuns = 2, extraType = "WIDE")), 1, listOf("A", "B"))
        assertEquals(2, card.totalRuns)
        assertEquals(0, card.legalBallsBowled)
        assertEquals(0, card.batsmen["A"]!!.runs)
        assertEquals(0, card.batsmen["A"]!!.ballsFaced)
        assertEquals(2, card.bowlers["X"]!!.runsConceded)
        assertEquals("2wd", card.thisOverBalls.single())
    }

    @Test
    fun noBallFourCreditsBatter() {
        val card = CricketScorer.calculateInningsScorecard(listOf(ball(runs = 4, extraRuns = 1, extraType = "NO_BALL")), 1, emptyList())
        assertEquals(5, card.totalRuns)
        assertEquals(4, card.batsmen["A"]!!.runs)
        assertEquals(1, card.batsmen["A"]!!.fours)
        assertEquals(1, card.extras["NB"])
    }

    @Test
    fun byesGoToExtrasNotBatter() {
        val card = CricketScorer.calculateInningsScorecard(listOf(ball(extraRuns = 2, extraType = "BYE")), 1, emptyList())
        assertEquals(2, card.totalRuns)
        assertEquals(0, card.batsmen["A"]!!.runs)
        assertEquals(1, card.batsmen["A"]!!.ballsFaced)
        assertEquals(0, card.bowlers["X"]!!.runsConceded)
    }

    @Test
    fun oversGroupAndPartnershipResetsOnWicket() {
        val balls = List(6) { ball(runs = 1) } + ball(wicket = true, wicketType = "BOWLED") + ball(runs = 4, striker = "C")
        val card = CricketScorer.calculateInningsScorecard(balls, 1, listOf("A", "B", "C"))
        assertEquals(2, card.overs.size)
        assertTrue(card.overs[0].isComplete)
        assertEquals(6, card.overs[0].runs)
        assertEquals(1, card.overs[1].wickets)
        assertEquals(4, card.partnership.runs)
        assertEquals(1, card.partnership.balls)
        assertEquals(1, card.bowlers["X"]!!.wickets)
        assertEquals(listOf("W", "4"), card.thisOverBalls)
    }

    @Test
    fun runOutDoesNotCreditBowler() {
        val card = CricketScorer.calculateInningsScorecard(listOf(ball(runs = 1, wicket = true, wicketType = "RUN_OUT")), 1, emptyList())
        assertEquals(1, card.totalWickets)
        assertEquals(0, card.bowlers["X"]!!.wickets)
        assertEquals("W+1", card.thisOverBalls.single())
    }

    @Test
    fun legacyNoBallAndWideAreRepaired() {
        // Old encoding: no ball stored 1 + runs as extras, wide with runs also kept runs on the bat
        val oldNb = ball(runs = 4, extraRuns = 5, extraType = "NO_BALL")
        val oldWd = ball(runs = 1, extraRuns = 2, extraType = "WIDE")
        val card = CricketScorer.calculateInningsScorecard(listOf(oldNb, oldWd), 1, emptyList())
        assertEquals(5 + 2, card.totalRuns)
        assertEquals(4, card.batsmen["A"]!!.runs)
        // Repair is idempotent
        assertEquals(CricketScorer.normalize(oldNb), CricketScorer.normalize(CricketScorer.normalize(oldNb)))
    }

    @Test
    fun legacyJsonWithoutFielderStillParses() {
        val json = """[{"id":"x","overIndex":0,"ballNumber":1,"runs":2,"extraRuns":0,"isExtra":false,"extraType":null,"isWicket":false,"wicketType":null,"striker":"A","nonStriker":"B","bowler":"X","dismissedPlayer":null,"innings":1,"timestamp":1}]"""
        val balls = CricketScorer.parseBalls(json)
        assertEquals(1, balls.size)
        assertEquals(null, balls[0].fielder)
    }

    @Test
    fun fielderIsRecordedOnDismissal() {
        val b = ball(wicket = true, wicketType = "CAUGHT").copy(fielder = "Fahad")
        val card = CricketScorer.calculateInningsScorecard(listOf(b), 1, listOf("A", "B"))
        assertEquals("Fahad", card.batsmen["A"]!!.fielderName)
        assertEquals(1, card.bowlers["X"]!!.wickets)
    }
}
