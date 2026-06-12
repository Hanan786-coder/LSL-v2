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
    val isStriker: Boolean = false
) {
    val strikeRate: Double
        get() = if (ballsFaced > 0) (runs.toDouble() / ballsFaced) * 100.0 else 0.0
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

data class InningsScorecard(
    val totalRuns: Int = 0,
    val totalWickets: Int = 0,
    val legalBallsBowled: Int = 0,
    val runRate: Double = 0.0,
    val extras: Map<String, Int> = mapOf("WD" to 0, "NB" to 0, "BYE" to 0, "LB" to 0),
    val batsmen: Map<String, BatsmanLiveStats> = emptyMap(),
    val bowlers: Map<String, BowlerLiveStats> = emptyMap(),
    val thisOverBalls: List<String> = emptyList() // list of labels for the current over
) {
    val totalOversString: String
        get() = "${legalBallsBowled / 6}.${legalBallsBowled % 6}"
        
    val totalExtrasSum: Int
        get() = extras.values.sum()
}

object CricketScorer {

    fun calculateInningsScorecard(balls: List<BallInfo>, inningsId: Int, playerRoster: List<String>): InningsScorecard {
        var totalRuns = 0
        var totalWickets = 0
        var legalBallsBowled = 0
        var wides = 0
        var noBalls = 0
        var byes = 0
        var legByes = 0

        val batsmenStats = mutableMapOf<String, BatsmanLiveStats>()
        val bowlersStats = mutableMapOf<String, BowlerLiveStats>()
        val thisOverBalls = mutableListOf<String>()

        // Initialize players in batsmen stats so we can show them on scorecard even with 0(0)
        for (player in playerRoster) {
            batsmenStats[player] = BatsmanLiveStats(name = player)
        }

        val inningsBalls = balls.filter { it.innings == inningsId }

        for (ball in inningsBalls) {
            val batName = ball.striker
            val bowlName = ball.bowler

            val bStat = batsmenStats[batName] ?: BatsmanLiveStats(name = batName)
            val bowlStat = bowlersStats[bowlName] ?: BowlerLiveStats(name = bowlName)

            // Calculate runs and legal ball status
            val isWide = ball.extraType == "WIDE"
            val isNoBall = ball.extraType == "NO_BALL"
            val isLegal = !isWide && !isNoBall

            if (isLegal) {
                legalBallsBowled++
            }

            // Run accrual
            var ballTotalRuns = ball.runs + ball.extraRuns
            totalRuns += ballTotalRuns

            // Extra breakdown
            if (ball.isExtra) {
                when (ball.extraType) {
                    "WIDE" -> wides += ball.extraRuns
                    "NO_BALL" -> noBalls += ball.extraRuns
                    "BYE" -> byes += ball.extraRuns
                    "LEG_BYE" -> legByes += ball.extraRuns
                }
            }

            // Batsman scoring update
            val batsmanRunsIncrement = if (ball.extraType == "BYE" || ball.extraType == "LEG_BYE") 0 else ball.runs
            val ballsFacedIncrement = if (isWide) 0 else 1 // wide is not ball faced

            val newFour = if (batsmanRunsIncrement == 4) 1 else 0
            val newSix = if (batsmanRunsIncrement == 6) 1 else 0

            var batsmanWickets = bStat.isOut
            var batsmanOutType = bStat.outType
            var batsmanBowlerName = bStat.bowlerName

            if (ball.isWicket) {
                // If the player is the striker or the dismissedPlayer matches striker
                val outPlayer = ball.dismissedPlayer ?: batName
                if (outPlayer == batName) {
                    batsmanWickets = true
                    batsmanOutType = ball.wicketType ?: "OUT"
                    batsmanBowlerName = bowlName
                } else {
                    // Could be runout for non-striker
                    val nonStrStat = batsmenStats[outPlayer] ?: BatsmanLiveStats(name = outPlayer)
                    batsmenStats[outPlayer] = nonStrStat.copy(
                        isOut = true,
                        outType = "RUN OUT",
                        bowlerName = bowlName
                    )
                }
                totalWickets++
            }

            batsmenStats[batName] = bStat.copy(
                runs = bStat.runs + batsmanRunsIncrement,
                ballsFaced = bStat.ballsFaced + ballsFacedIncrement,
                fours = bStat.fours + newFour,
                sixes = bStat.sixes + newSix,
                isOut = batsmanWickets,
                outType = batsmanOutType,
                bowlerName = batsmanBowlerName
            )

            // Bowler scoring update
            // bowler runs conceded: batsman runs + wides/noballs extras
            val bowlerRunsIncrement = ball.runs + (if (isWide || isNoBall) ball.extraRuns else 0)
            val bowlerWicketIncrement = if (ball.isWicket && ball.wicketType != "RUN_OUT") 1 else 0

            bowlersStats[bowlName] = bowlStat.copy(
                ballsBowled = bowlStat.ballsBowled + (if (isLegal) 1 else 0),
                runsConceded = bowlStat.runsConceded + bowlerRunsIncrement,
                wickets = bowlStat.wickets + bowlerWicketIncrement,
                wides = bowlStat.wides + (if (isWide) 1 else 0),
                noBalls = bowlStat.noBalls + (if (isNoBall) 1 else 0)
            )

            // Format ball log for over visual feed
            val ballLabel = when {
                ball.isWicket -> "W"
                isWide -> "${ball.runs + ball.extraRuns}wd"
                isNoBall -> "${ball.runs + ball.extraRuns}nb"
                ball.runs == 0 -> "0"
                else -> "${ball.runs}"
            }
            thisOverBalls.add(ballLabel)
        }

        // Keep only last over's deliveries for standard view (or last 6 balls)
        val overLength = if (legalBallsBowled > 0) legalBallsBowled % 6 else 0
        // We can group balls in the current active over:
        // A legal ball index starts fresh on every (legalBalls / 6) over boundary
        val lastOverBalls = mutableListOf<String>()
        var legalInCurrent = 0
        for (i in billingOverSequences(inningsBalls)) {
            lastOverBalls.add(i)
        }

        val runRate = if (legalBallsBowled > 0) (totalRuns.toDouble() / (legalBallsBowled.toDouble() / 6.0)) else 0.0

        val extraMap = mapOf(
            "WD" to wides,
            "NB" to noBalls,
            "BYE" to byes,
            "LB" to legByes
        )

        return InningsScorecard(
            totalRuns = totalRuns,
            totalWickets = totalWickets,
            legalBallsBowled = legalBallsBowled,
            runRate = runRate,
            extras = extraMap,
            batsmen = batsmenStats,
            bowlers = bowlersStats,
            thisOverBalls = lastOverBalls.takeLast(12) // show last 12 events for clean over flow
        )
    }

    private fun billingOverSequences(balls: List<BallInfo>): List<String> {
        val list = mutableListOf<String>()
        var legalCount = 0
        for (ball in balls) {
            val isWide = ball.extraType == "WIDE"
            val isNoBall = ball.extraType == "NO_BALL"
            val label = when {
                ball.isWicket -> "W"
                isWide -> "wd"
                isNoBall -> "nb"
                else -> "${ball.runs}"
            }
            
            val suffix = if (isWide || isNoBall) "" else ""
            list.add(label + suffix)

            if (!isWide && !isNoBall) {
                legalCount++
            }
        }
        
        // Return only elements of the current over (elements since the last completed over)
        val completedOvers = legalCount / 6
        val ballsBeforeCurrentOver = completedOvers * 6
        
        // Find indices of legal balls to filter
        var tempLegalCount = 0
        val overBalls = mutableListOf<String>()
        for (ball in balls) {
            val isWide = ball.extraType == "WIDE"
            val isNoBall = ball.extraType == "NO_BALL"
            
            val label = when {
                ball.isWicket -> if (ball.wicketType == "RUN_OUT") "W-RO" else "W"
                isWide -> if (ball.runs > 0) "${ball.runs + 1}wd" else "wd"
                isNoBall -> if (ball.runs > 0) "${ball.runs + 1}nb" else "nb"
                ball.extraType == "BYE" -> "${ball.extraRuns}B"
                ball.extraType == "LEG_BYE" -> "${ball.extraRuns}LB"
                else -> "${ball.runs}"
            }
            
            if (tempLegalCount >= ballsBeforeCurrentOver) {
                overBalls.add(label)
            }
            
            if (!isWide && !isNoBall) {
                tempLegalCount++
            }
        }
        return overBalls
    }
}
