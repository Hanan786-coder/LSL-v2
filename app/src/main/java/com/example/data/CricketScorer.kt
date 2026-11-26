package com.example.data

data class BatsmanLiveStats(
    val name: String,
    val runs: Int = 0,
    val ballsFaced: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val outType: String? = null,
    val bowlerName: String? = null,
    val fielderName: String? = null,
    val isStriker: Boolean = false,
    /** Order in which the batter first appeared at the crease; Int.MAX_VALUE if never batted. */
    val battingOrder: Int = Int.MAX_VALUE
) {
    val strikeRate: Double
        get() = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100.0 else 0.0

    val hasBatted: Boolean get() = battingOrder != Int.MAX_VALUE
}

data class BowlerLiveStats(
    val name: String,
    val ballsBowled: Int = 0, // legal balls
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0
) {
    val oversString: String
        get() = "${ballsBowled / 6}.${ballsBowled % 6}"

    val economy: Double
        get() = if (ballsBowled > 0) (runsConceded.toDouble() / (ballsBowled.toDouble() / 6.0)) else 0.0
}

/** One completed (or in-progress) over, used for the over-by-over chart and timeline. */
data class OverSummary(
    val index: Int,          // 0-based over number
    val bowler: String,
    val runs: Int,
    val wickets: Int,
    val labels: List<String>,
    val isComplete: Boolean
)

data class Partnership(val runs: Int, val balls: Int)

data class InningsScorecard(
    val totalRuns: Int = 0,
    val totalWickets: Int = 0,
    val legalBallsBowled: Int = 0,
    val runRate: Double = 0.0,
    val extras: Map<String, Int> = mapOf("WD" to 0, "NB" to 0, "BYE" to 0, "LB" to 0),
    val batsmen: Map<String, BatsmanLiveStats> = emptyMap(),
    val bowlers: Map<String, BowlerLiveStats> = emptyMap(),
    val thisOverBalls: List<String> = emptyList(),
    val overs: List<OverSummary> = emptyList(),
    val partnership: Partnership = Partnership(0, 0)
) {
    val totalOversString: String
        get() = "${legalBallsBowled / 6}.${legalBallsBowled % 6}"

    val totalExtrasSum: Int
        get() = extras.values.sum()

    /** Legal balls already bowled in the over that is currently in progress (0..5). */
    val legalBallsInCurrentOver: Int
        get() = legalBallsBowled % 6
}

/**
 * Ball encoding (from version 2 of the scorer):
 *  - Normal ball:  runs = runs off bat, extraRuns = 0
 *  - Wide:         runs = 0,  extraRuns = 1 + runs completed
 *  - No ball:      runs = runs off bat (credited to batter), extraRuns = 1
 *  - Bye/Leg bye:  runs = 0,  extraRuns = runs completed
 * Total for the ball is always runs + extraRuns.
 */
object CricketScorer {

    /**
     * The one place ball logs are read. Also repairs logs written before the scoring fix,
     * where a no ball stored 1 + runs as extras and a wide with runs also credited the bat:
     *  - NO_BALL with extraRuns > 1  -> extraRuns = 1
     *  - WIDE with runs > 0          -> runs = 0 (extraRuns already holds 1 + runs)
     * Both shapes can never be produced by the current scorer, so the repair is safe to re-run.
     */
    fun parseBalls(json: String): List<BallInfo> =
        JsonHelper.listFromJson(json, BallInfo::class.java).map(::normalize)

    fun normalize(b: BallInfo): BallInfo = when {
        b.extraType == "NO_BALL" && b.extraRuns > 1 -> b.copy(extraRuns = 1)
        b.extraType == "WIDE" && b.runs > 0 -> b.copy(runs = 0, extraRuns = maxOf(b.extraRuns, 1 + b.runs))
        else -> b
    }

    fun isLegal(ball: BallInfo): Boolean = ball.extraType != "WIDE" && ball.extraType != "NO_BALL"

    /** Short, scoreboard-style label for a single delivery. */
    fun ballLabel(ball: BallInfo): String {
        val total = ball.runs + ball.extraRuns
        val base = when (ball.extraType) {
            "WIDE" -> if (total > 1) "${total}wd" else "wd"
            "NO_BALL" -> if (total > 1) "${total}nb" else "nb"
            "BYE" -> "${ball.extraRuns}b"
            "LEG_BYE" -> "${ball.extraRuns}lb"
            else -> "${ball.runs}"
        }
        return if (ball.isWicket) {
            if (total > 0) "W+$total" else "W"
        } else base
    }

    fun calculateInningsScorecard(balls: List<BallInfo>, inningsId: Int, playerRoster: List<String>): InningsScorecard {
        var totalRuns = 0
        var totalWickets = 0
        var legalBallsBowled = 0
        var wides = 0
        var noBalls = 0
        var byes = 0
        var legByes = 0
        var nextBattingOrder = 0

        val batsmenStats = linkedMapOf<String, BatsmanLiveStats>()
        val bowlersStats = linkedMapOf<String, BowlerLiveStats>()

        // Seed roster so the scorecard can show "yet to bat" players.
        for (player in playerRoster) {
            batsmenStats[player] = BatsmanLiveStats(name = player)
        }

        fun markAtCrease(name: String) {
            if (name.isBlank()) return
            val s = batsmenStats[name] ?: BatsmanLiveStats(name = name)
            if (!s.hasBatted) batsmenStats[name] = s.copy(battingOrder = nextBattingOrder++)
        }

        val inningsBalls = balls.filter { it.innings == inningsId }.map(::normalize)

        // Over tracking
        val overs = mutableListOf<OverSummary>()
        var overRuns = 0
        var overWkts = 0
        var overLabels = mutableListOf<String>()
        var overBowler = ""
        var overLegal = 0

        // Partnership tracking
        var pRuns = 0
        var pBalls = 0

        for (ball in inningsBalls) {
            val batName = ball.striker
            val bowlName = ball.bowler
            markAtCrease(batName)
            markAtCrease(ball.nonStriker)

            val bStat = batsmenStats[batName]!!
            val bowlStat = bowlersStats[bowlName] ?: BowlerLiveStats(name = bowlName)

            val isWide = ball.extraType == "WIDE"
            val isNoBall = ball.extraType == "NO_BALL"
            val legal = !isWide && !isNoBall
            if (legal) legalBallsBowled++

            val ballTotal = ball.runs + ball.extraRuns
            totalRuns += ballTotal

            when (ball.extraType) {
                "WIDE" -> wides += ball.extraRuns
                "NO_BALL" -> noBalls += ball.extraRuns
                "BYE" -> byes += ball.extraRuns
                "LEG_BYE" -> legByes += ball.extraRuns
            }

            val batRuns = if (ball.extraType == "BYE" || ball.extraType == "LEG_BYE" || isWide) 0 else ball.runs
            val facedIncrement = if (isWide) 0 else 1

            var striker = bStat.copy(
                runs = bStat.runs + batRuns,
                ballsFaced = bStat.ballsFaced + facedIncrement,
                fours = bStat.fours + if (batRuns == 4) 1 else 0,
                sixes = bStat.sixes + if (batRuns == 6) 1 else 0
            )

            if (ball.isWicket) {
                val outPlayer = ball.dismissedPlayer ?: batName
                val outType = ball.wicketType ?: "OUT"
                if (outPlayer == batName) {
                    striker = striker.copy(isOut = true, outType = outType, bowlerName = bowlName, fielderName = ball.fielder)
                } else {
                    val other = batsmenStats[outPlayer] ?: BatsmanLiveStats(name = outPlayer)
                    batsmenStats[outPlayer] = other.copy(isOut = true, outType = outType, bowlerName = bowlName, fielderName = ball.fielder)
                }
                totalWickets++
            }
            batsmenStats[batName] = striker

            val bowlerRuns = ball.runs + (if (isWide || isNoBall) ball.extraRuns else 0)
            val creditsBowler = ball.isWicket && ball.wicketType != "RUN_OUT" && ball.wicketType != "RETIRED"
            bowlersStats[bowlName] = bowlStat.copy(
                ballsBowled = bowlStat.ballsBowled + if (legal) 1 else 0,
                runsConceded = bowlStat.runsConceded + bowlerRuns,
                wickets = bowlStat.wickets + if (creditsBowler) 1 else 0,
                wides = bowlStat.wides + if (isWide) 1 else 0,
                noBalls = bowlStat.noBalls + if (isNoBall) 1 else 0
            )

            // Partnership
            pRuns += ballTotal
            if (legal) pBalls++
            if (ball.isWicket) { pRuns = 0; pBalls = 0 }

            // Over grouping
            if (overLabels.isEmpty()) overBowler = bowlName
            overLabels.add(ballLabel(ball))
            overRuns += ballTotal
            if (ball.isWicket) overWkts++
            if (legal) overLegal++
            if (overLegal == 6) {
                overs.add(OverSummary(overs.size, overBowler, overRuns, overWkts, overLabels, true))
                overRuns = 0; overWkts = 0; overLegal = 0; overLabels = mutableListOf()
            }
        }

        if (overLabels.isNotEmpty()) {
            overs.add(OverSummary(overs.size, overBowler, overRuns, overWkts, overLabels, false))
        }

        val runRate = if (legalBallsBowled > 0) totalRuns / (legalBallsBowled / 6.0) else 0.0

        return InningsScorecard(
            totalRuns = totalRuns,
            totalWickets = totalWickets,
            legalBallsBowled = legalBallsBowled,
            runRate = runRate,
            extras = mapOf("WD" to wides, "NB" to noBalls, "BYE" to byes, "LB" to legByes),
            batsmen = batsmenStats,
            bowlers = bowlersStats,
            // Balls of the over in progress (empty right after an over completes)
            thisOverBalls = overs.lastOrNull()?.takeIf { !it.isComplete }?.labels ?: emptyList(),
            overs = overs,
            partnership = Partnership(pRuns, pBalls)
        )
    }
}
