package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LocoViewModel(private val repository: MatchRepository) : ViewModel() {

    init {
        // Prepopulate with rich sample data on first start
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }
    }

    // --- State flows from DB ---
    val tournaments: StateFlow<List<TournamentEntity>> = repository.allTournaments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teams: StateFlow<List<TeamEntity>> = repository.allTeams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matches: StateFlow<List<MatchEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerStats: StateFlow<List<PlayerStatsEntity>> = repository.allPlayerStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Screen Navigation & UI flows ---
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // --- Match Scoring Active State ---
    private val _activeMatchId = MutableStateFlow<Int?>(null)
    val activeMatchId: StateFlow<Int?> = _activeMatchId.asStateFlow()

    val activeMatch: StateFlow<MatchEntity?> = _activeMatchId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allMatches.map { list -> list.find { it.id == id } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dialogue Controls
    var showDismissalDialog = MutableStateFlow(false)
    var showBowlerSelectDialog = MutableStateFlow(false)
    var showEditScoreDialog = MutableStateFlow(false)

    // --- Team & Tournament Management Actions ---
    fun createTeam(name: String, players: List<String>, tournamentId: Int? = null) {
        viewModelScope.launch {
            val commaPlayers = players.filter { it.isNotBlank() }.joinToString(", ")
            repository.insertTeam(TeamEntity(name = name, playerNames = commaPlayers, tournamentId = tournamentId))
        }
    }

    fun deleteTeam(team: TeamEntity) {
        viewModelScope.launch {
            repository.deleteTeam(team)
        }
    }

    fun createTournament(name: String) {
        viewModelScope.launch {
            repository.insertTournament(TournamentEntity(name = name))
        }
    }

    fun deleteTournament(tournament: TournamentEntity) {
        viewModelScope.launch {
            repository.deleteTournament(tournament)
        }
    }

    // --- Match Operations ---
    fun startMatch(
        teamAId: Int,
        teamBId: Int,
        teamAName: String,
        teamBName: String,
        overs: Int,
        battingFirstId: Int,
        teamAPlayers: List<String>,
        teamBPlayers: List<String>,
        tournamentId: Int? = null
    ) {
        viewModelScope.launch {
            val battingTeamPlayers = if (battingFirstId == teamAId) teamAPlayers else teamBPlayers
            val bowlingTeamPlayers = if (battingFirstId == teamAId) teamBPlayers else teamAPlayers
            
            val striker = battingTeamPlayers.getOrNull(0) ?: "Batsman 1"
            val nonStriker = battingTeamPlayers.getOrNull(1) ?: "Batsman 2"
            val bowler = bowlingTeamPlayers.getOrNull(0) ?: "Bowler 1"

            val matchEntity = MatchEntity(
                tournamentId = tournamentId,
                teamAId = teamAId,
                teamBId = teamBId,
                teamAName = teamAName,
                teamBName = teamBName,
                overs = overs,
                status = "ONGOING",
                currentInnings = 1,
                battingFirstTeamId = battingFirstId,
                currentStriker = striker,
                currentNonStriker = nonStriker,
                currentBowler = bowler,
                ballsJson = "[]"
            )
            val matchId = repository.insertMatch(matchEntity).toInt()
            _activeMatchId.value = matchId
            navigateTo("scoring")
        }
    }

    fun resumeScoring(matchId: Int) {
        _activeMatchId.value = matchId
        navigateTo("scoring")
    }

    // Get current lists of batsman and bowler rosters based on who is playing
    fun getActiveMatchTeams(): Pair<TeamEntity?, TeamEntity?> {
        val match = activeMatch.value ?: return Pair(null, null)
        val allTeamsList = teams.value
        val teamA = allTeamsList.find { it.id == match.teamAId }
        val teamB = allTeamsList.find { it.id == match.teamBId }
        return Pair(teamA, teamB)
    }

    // Active Batting List based on current innings
    fun getBattingTeamPlayers(): List<String> {
        val match = activeMatch.value ?: return emptyList()
        val (teamA, teamB) = getActiveMatchTeams()
        val battingFirst = match.battingFirstTeamId
        val battingTeamId = if (match.currentInnings == 1) {
            battingFirst
        } else {
            if (battingFirst == match.teamAId) match.teamBId else match.teamAId
        }
        val team = if (teamA?.id == battingTeamId) teamA else teamB
        return team?.playerNames?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    // Active Bowling List based on current innings
    fun getBowlingTeamPlayers(): List<String> {
        val match = activeMatch.value ?: return emptyList()
        val (teamA, teamB) = getActiveMatchTeams()
        val battingFirst = match.battingFirstTeamId
        val bowlingTeamId = if (match.currentInnings == 1) {
            if (battingFirst == match.teamAId) match.teamBId else match.teamAId
        } else {
            battingFirst
        }
        val team = if (teamA?.id == bowlingTeamId) teamA else teamB
        return team?.playerNames?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
    }

    // --- Primary Ball-By-Ball Scoring Action ---
    fun recordBall(
        runs: Int,
        extraType: String? = null, // "WIDE", "NO_BALL", "BYE", "LEG_BYE"
        isWicket: Boolean = false,
        wicketType: String? = null,
        dismissedPlayer: String? = null
    ) {
        val match = activeMatch.value ?: return
        val balls = JsonHelper.listFromJson(match.ballsJson, BallInfo::class.java).toMutableList()

        val striker = match.currentStriker
        val nonStriker = match.currentNonStriker
        val bowler = match.currentBowler

        // Find how many legal balls are bowled in this innings so far, to calculate over and ballNumber
        val inningsId = match.currentInnings
        val currentInningsBalls = balls.filter { it.innings == inningsId }
        val legalCount = currentInningsBalls.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }

        val overIndex = legalCount / 6
        val ballNumber = (legalCount % 6) + 1

        val isExtra = extraType != null
        val extraRuns = when (extraType) {
            "WIDE", "NO_BALL" -> 1
            "BYE", "LEG_BYE" -> 0 // runs are physical runs off the bat, but treated as extra. We input runs as total runs, but we store them appropriately
            else -> 0
        }

        // Inside gully cricket scoring, if wide is bowled and batters run 1 (making 2 total wide runs),
        // we can input runs as some value.
        // We calculate total runs off bat vs extras. Let's make it extremely clean and logical.
        val ballInfo = BallInfo(
            overIndex = overIndex,
            ballNumber = if (isExtra && (extraType == "WIDE" || extraType == "NO_BALL")) {
                if (ballNumber - 1 < 1) 1 else ballNumber - 1
            } else ballNumber,
            runs = if (extraType == "BYE" || extraType == "LEG_BYE") 0 else runs,
            extraRuns = if (extraType == "WIDE" || extraType == "NO_BALL") 1 + runs else if (extraType == "BYE" || extraType == "LEG_BYE") runs else 0,
            isExtra = isExtra,
            extraType = extraType,
            isWicket = isWicket,
            wicketType = wicketType,
            striker = striker,
            nonStriker = nonStriker,
            bowler = bowler,
            dismissedPlayer = if (isWicket) (dismissedPlayer ?: striker) else null,
            innings = inningsId
        )

        balls.add(ballInfo)

        // Rotation & State calculations
        var nextStriker = striker
        var nextNonStriker = nonStriker

        // 1. Swap batsmen on Odd Runs (if not wide, or if wide/no ball with odd runs completed)
        val swapRuns = if (extraType == "BYE" || extraType == "LEG_BYE") runs else runs
        if (swapRuns % 2 == 1) {
            nextStriker = nonStriker
            nextNonStriker = striker
        }

        // 2. Wicket management
        var isDismissedStriker = isWicket && (dismissedPlayer == null || dismissedPlayer == striker)
        var isDismissedNonStriker = isWicket && (dismissedPlayer == nonStriker)

        // Calculate if we need new batsman dialog trigger
        var triggerDismissedSelect = false
        if (isWicket) {
            triggerDismissedSelect = true
        }

        // Calculate total legal balls count after of this ball
        val newLegalCount = if (isExtra && (extraType == "WIDE" || extraType == "NO_BALL")) legalCount else legalCount + 1
        var triggerBowlerSelect = false
        if (newLegalCount > 0 && newLegalCount % 6 == 0 && (extraType != "WIDE" && extraType != "NO_BALL")) {
            // Over finished! Swap batsmen ends
            val temp = nextStriker
            nextStriker = nextNonStriker
            nextNonStriker = temp
            triggerBowlerSelect = true
        }

        // Calculate current scorecard to check for Innings Complete or Match Complete
        val currentRoster = getBattingTeamPlayers()
        val tempInningsCard = CricketScorer.calculateInningsScorecard(balls, inningsId, currentRoster)

        // Gully cricket rosters can be n players, batting ends when n-1 wickets occur (all out)
        val totalPlayers = currentRoster.size
        val maxWickets = if (totalPlayers > 1) totalPlayers - 1 else 10 // default 10 if roster not defined

        var isInningsEnd = false
        var isMatchEnd = false
        
        val totalOvers = match.overs

        if (tempInningsCard.totalWickets >= maxWickets || tempInningsCard.legalBallsBowled >= (totalOvers * 6)) {
            isInningsEnd = true
        }

        if (inningsId == 2) {
            val firstInningsCard = CricketScorer.calculateInningsScorecard(balls, 1, getBowlingTeamPlayers())
            val target = firstInningsCard.totalRuns + 1
            if (tempInningsCard.totalRuns >= target) {
                isMatchEnd = true
                isInningsEnd = true
            } else if (isInningsEnd) {
                isMatchEnd = true
            }
        }

        viewModelScope.launch {
            var updatedMatch = match.copy(
                ballsJson = JsonHelper.listToJson(balls, BallInfo::class.java),
                currentStriker = nextStriker,
                currentNonStriker = nextNonStriker
            )

            if (isMatchEnd) {
                // Complete match and calculate winner
                val firstInningsCard = CricketScorer.calculateInningsScorecard(balls, 1, getBowlingTeamPlayers())
                val secondInningsCard = CricketScorer.calculateInningsScorecard(balls, 2, getBattingTeamPlayers())
                
                val teamAName = match.teamAName
                val teamBName = match.teamBName
                val isABattingFirst = match.battingFirstTeamId == match.teamAId

                val batFirstRuns = firstInningsCard.totalRuns
                val batSecondRuns = secondInningsCard.totalRuns

                val winnerId: Int
                val result: String

                if (batFirstRuns > batSecondRuns) {
                    winnerId = match.battingFirstTeamId
                    val winnerName = if (winnerId == match.teamAId) teamAName else teamBName
                    val diff = batFirstRuns - batSecondRuns
                    result = "$winnerName won by $diff runs"
                } else if (batSecondRuns > batFirstRuns) {
                    winnerId = if (match.battingFirstTeamId == match.teamAId) match.teamBId else match.teamAId
                    val winnerName = if (winnerId == match.teamAId) teamAName else teamBName
                    val wktsLeft = maxWickets - secondInningsCard.totalWickets
                    result = "$winnerName won by $wktsLeft wickets"
                } else {
                    winnerId = -1 // Draw
                    result = "Match Tied!"
                }

                // Pick top performer based on most runs / wickets in balls
                var bestPerformerName = ""
                var maxScoreValue = -1
                for ( batsmanEntry in secondInningsCard.batsmen.values + firstInningsCard.batsmen.values ) {
                    val score = batsmanEntry.runs * 2 + batsmanEntry.ballsFaced // arbitrary weights
                    if (score > maxScoreValue) {
                        maxScoreValue = score
                        bestPerformerName = "${batsmanEntry.name} ${batsmanEntry.runs}(${batsmanEntry.ballsFaced})"
                    }
                }
                
                updatedMatch = updatedMatch.copy(
                    status = "COMPLETED",
                    winnerTeamId = if (winnerId != -1) winnerId else null,
                    resultText = result,
                    topPerformer = bestPerformerName
                )
                repository.insertMatch(updatedMatch)
                repository.updatePlayerStatsFromCompletedMatch(updatedMatch)
                _activeMatchId.value = updatedMatch.id
                navigateTo("scorecard")
            } else if (isInningsEnd && inningsId == 1) {
                // First innings ended! Swift to innings 2
                val firstInningsCard = CricketScorer.calculateInningsScorecard(balls, 1, currentRoster)
                val target = firstInningsCard.totalRuns + 1
                
                val originalBattingFirst = match.battingFirstTeamId
                val newBattingTeamId = if (originalBattingFirst == match.teamAId) match.teamBId else match.teamAId
                val newBattingTeam = if (newBattingTeamId == match.teamAId) originalBattingFirst else newBattingTeamId
                
                // Fetch opponent roster (now the batting roster)
                val newRoster = getBowlingTeamPlayers()
                val nextBowlRoster = getBattingTeamPlayers()

                updatedMatch = updatedMatch.copy(
                    currentInnings = 2,
                    targetRuns = target,
                    currentStriker = newRoster.getOrNull(0) ?: "Batsman A",
                    currentNonStriker = newRoster.getOrNull(1) ?: "Batsman B",
                    currentBowler = nextBowlRoster.getOrNull(0) ?: "Bowler A"
                )
                repository.insertMatch(updatedMatch)
            } else {
                // Match is ongoing
                repository.insertMatch(updatedMatch)

                if (triggerDismissedSelect) {
                    showDismissalDialog.value = true
                } else if (triggerBowlerSelect) {
                    showBowlerSelectDialog.value = true
                }
            }
        }
    }

    // Replace the dismissed batsman
    fun replaceDismissedBatsman(newName: String, isStriker: Boolean = true) {
        val match = activeMatch.value ?: return
        viewModelScope.launch {
            val updated = if (isStriker) {
                match.copy(currentStriker = newName)
            } else {
                match.copy(currentNonStriker = newName)
            }
            repository.insertMatch(updated)
            showDismissalDialog.value = false
            
            // Check if bowler needs select immediately after replacement (e.g. wicket fell on last ball of over)
            val balls = JsonHelper.listFromJson(updated.ballsJson, BallInfo::class.java)
            val legalCount = balls.filter { it.innings == updated.currentInnings }.count { it.extraType != "WIDE" && it.extraType != "NO_BALL" }
            if (legalCount > 0 && legalCount % 6 == 0 && balls.isNotEmpty() && balls.last().extraType != "WIDE" && balls.last().extraType != "NO_BALL") {
                showBowlerSelectDialog.value = true
            }
        }
    }

    // Change current bowler
    fun selectNewBowler(name: String) {
        val match = activeMatch.value ?: return
        viewModelScope.launch {
            repository.insertMatch(match.copy(currentBowler = name))
            showBowlerSelectDialog.value = false
        }
    }

    // Undo last delivery
    fun undoLastBall() {
        val match = activeMatch.value ?: return
        val balls = JsonHelper.listFromJson(match.ballsJson, BallInfo::class.java).toMutableList()
        if (balls.isEmpty()) return

        val undoneBall = balls.removeAt(balls.size - 1)

        viewModelScope.launch {
            val updatedMatch = match.copy(
                ballsJson = JsonHelper.listToJson(balls, BallInfo::class.java),
                // Revert striker/nonstriker/bowlers is best handled by re-evaluating the history minus 1,
                // or returning to last ball striker. For simplicity, we can let user manually correct, 
                // but let's reset to what it was in undone ball!
                currentStriker = undoneBall.striker,
                currentNonStriker = undoneBall.nonStriker,
                currentBowler = undoneBall.bowler,
                currentInnings = undoneBall.innings
            )
            repository.insertMatch(updatedMatch)
        }
    }

    // Update index score (Edit scores in history JSON)
    fun editBallAt(index: Int, editedBall: BallInfo) {
        val match = activeMatch.value ?: return
        val balls = JsonHelper.listFromJson(match.ballsJson, BallInfo::class.java).toMutableList()
        if (index < 0 || index >= balls.size) return

        balls[index] = editedBall

        viewModelScope.launch {
            repository.insertMatch(match.copy(ballsJson = JsonHelper.listToJson(balls, BallInfo::class.java)))
        }
    }

    fun deleteMatch(id: Int) {
        viewModelScope.launch {
            repository.deleteMatchById(id)
        }
    }

    companion object {
        fun provideFactory(repository: MatchRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LocoViewModel(repository) as T
                }
            }
    }
}
