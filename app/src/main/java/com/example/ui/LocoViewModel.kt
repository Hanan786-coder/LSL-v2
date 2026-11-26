package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** One-off moments the scoring screen celebrates or announces. */
sealed interface ScoreEvent {
    data object Four : ScoreEvent
    data object Six : ScoreEvent
    data class Wicket(val batter: String) : ScoreEvent
    data class OverComplete(val overNumber: Int, val runs: Int) : ScoreEvent
    data class InningsBreak(val target: Int, val chasingTeam: String) : ScoreEvent
}

fun String.toPlayerList(): List<String> =
    split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }

@OptIn(ExperimentalCoroutinesApi::class)
class LocoViewModel(private val repository: MatchRepository) : ViewModel() {

    // --- State flows from DB ---
    val tournaments: StateFlow<List<TournamentEntity>> = repository.allTournaments
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val teams: StateFlow<List<TeamEntity>> = repository.allTeams
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val matches: StateFlow<List<MatchEntity>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playerStats: StateFlow<List<PlayerStatsEntity>> = repository.allPlayerStats
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Navigation (simple back stack) ---
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()
    private val backStack = ArrayDeque<String>()

    /** Direction of the last navigation, so screen transitions can slide the right way. */
    var lastNavWasBack = false
        private set

    fun navigateTo(screen: String) {
        val current = _currentScreen.value
        if (current == screen) return
        lastNavWasBack = false
        if (screen == "dashboard") {
            backStack.clear()
        } else if (current != "splash") {
            backStack.addLast(current)
        }
        _currentScreen.value = screen
    }

    /** Returns false when there is nowhere to go back to (let the system close the app). */
    fun goBack(): Boolean {
        val current = _currentScreen.value
        if (current == "dashboard" || current == "splash") return false
        lastNavWasBack = true
        var target = backStack.removeLastOrNull() ?: "dashboard"
        // Never return to the scoring pad of a finished match.
        if (target == "scoring" && activeMatch.value?.status != "ONGOING") {
            target = backStack.removeLastOrNull() ?: "dashboard"
        }
        _currentScreen.value = target
        return true
    }

    // --- Match Scoring Active State ---
    private val _activeMatchId = MutableStateFlow<Int?>(null)

    val activeMatch: StateFlow<MatchEntity?> = _activeMatchId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allMatches.map { list -> list.find { it.id == id } }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _events = MutableSharedFlow<ScoreEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ScoreEvent> = _events.asSharedFlow()

    /** Pre-selected teams for the match setup screen (e.g. from a tournament fixture). */
    data class SetupPrefill(val teamAId: Int, val teamBId: Int, val tournamentId: Int?)
    val setupPrefill = MutableStateFlow<SetupPrefill?>(null)

    fun setupFixture(teamAId: Int, teamBId: Int, tournamentId: Int?) {
        setupPrefill.value = SetupPrefill(teamAId, teamBId, tournamentId)
        navigateTo("match_setup")
    }

    // Dialog controls
    val showDismissalDialog = MutableStateFlow(false)
    val showBowlerSelectDialog = MutableStateFlow(false)
    val showEditScoreDialog = MutableStateFlow(false)

    // --- Team & Tournament Management ---
    fun createTeam(name: String, players: List<String>, tournamentId: Int? = null) {
        viewModelScope.launch {
            repository.insertTeam(
                TeamEntity(
                    name = name.trim(),
                    playerNames = players.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", "),
                    tournamentId = tournamentId
                )
            )
        }
    }

    fun updateTeam(team: TeamEntity, name: String, players: List<String>, tournamentId: Int?) {
        viewModelScope.launch {
            repository.insertTeam(
                team.copy(
                    name = name.trim(),
                    playerNames = players.map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", "),
                    tournamentId = tournamentId
                )
            )
        }
    }

    fun deleteTeam(team: TeamEntity) {
        viewModelScope.launch { repository.deleteTeam(team) }
    }

    fun createTournament(name: String) {
        viewModelScope.launch { repository.insertTournament(TournamentEntity(name = name.trim())) }
    }

    fun deleteTournament(tournament: TournamentEntity) {
        viewModelScope.launch { repository.deleteTournament(tournament) }
    }

    // --- Roster helpers (pure: work for any match, not just the active one) ---
    fun rosterFor(teamId: Int): List<String> =
        teams.value.find { it.id == teamId }?.playerNames?.toPlayerList() ?: emptyList()

    fun battingTeamId(match: MatchEntity, innings: Int = match.currentInnings): Int {
        val second = if (match.battingFirstTeamId == match.teamAId) match.teamBId else match.teamAId
        return if (innings == 1) match.battingFirstTeamId else second
    }

    fun bowlingTeamId(match: MatchEntity, innings: Int = match.currentInnings): Int =
        battingTeamId(match, if (innings == 1) 2 else 1)

    fun teamName(match: MatchEntity, teamId: Int): String =
        if (teamId == match.teamAId) match.teamAName else match.teamBName

    fun getBattingTeamPlayers(): List<String> {
        val match = activeMatch.value ?: return emptyList()
        return rosterFor(battingTeamId(match))
    }

    fun getBowlingTeamPlayers(): List<String> {
        val match = activeMatch.value ?: return emptyList()
        return rosterFor(bowlingTeamId(match))
    }

    private fun maxWicketsFor(roster: List<String>): Int =
        if (roster.size > 1) roster.size - 1 else 10

    // --- Match lifecycle ---
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
                currentStriker = battingTeamPlayers.getOrNull(0) ?: "Batter 1",
                currentNonStriker = battingTeamPlayers.getOrNull(1) ?: "Batter 2",
                currentBowler = bowlingTeamPlayers.getOrNull(0) ?: "Bowler 1",
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

    /** Open a match's scorecard: live matches go to the scoring pad, finished ones to the card. */
    fun openMatch(match: MatchEntity) {
        _activeMatchId.value = match.id
        navigateTo(if (match.status == "ONGOING") "scoring" else "scorecard")
    }

    private fun balls(match: MatchEntity) =
        JsonHelper.listFromJson(match.ballsJson, BallInfo::class.java)

    // --- Ball-by-ball scoring ---
    /**
     * Records one delivery atomically. When [isWicket] is set, [incomingBatter] (if any) walks in
     * at the dismissed batter's end in the same write, so nothing can overwrite the ball.
     */
    fun recordBall(
        runs: Int,
        extraType: String? = null, // "WIDE", "NO_BALL", "BYE", "LEG_BYE"
        isWicket: Boolean = false,
        wicketType: String? = null,
        dismissedPlayer: String? = null,
        incomingBatter: String? = null
    ) {
        val match = activeMatch.value ?: return
        if (match.status != "ONGOING") return
        val balls = balls(match).toMutableList()

        val striker = match.currentStriker
        val nonStriker = match.currentNonStriker
        val inningsId = match.currentInnings
        val legalCount = balls.count { it.innings == inningsId && CricketScorer.isLegal(it) }

        val isWideOrNb = extraType == "WIDE" || extraType == "NO_BALL"
        val dismissed = if (isWicket) (dismissedPlayer ?: striker) else null

        val ball = BallInfo(
            overIndex = legalCount / 6,
            ballNumber = if (isWideOrNb) legalCount % 6 else (legalCount % 6) + 1,
            runs = when (extraType) {
                "WIDE", "BYE", "LEG_BYE" -> 0
                else -> runs
            },
            extraRuns = when (extraType) {
                "WIDE" -> 1 + runs
                "NO_BALL" -> 1
                "BYE", "LEG_BYE" -> runs
                else -> 0
            },
            isExtra = extraType != null,
            extraType = extraType,
            isWicket = isWicket,
            wicketType = wicketType,
            striker = striker,
            nonStriker = nonStriker,
            bowler = match.currentBowler,
            dismissedPlayer = dismissed,
            innings = inningsId
        )
        balls.add(ball)

        // Strike rotation on runs actually run
        var nextStriker = striker
        var nextNonStriker = nonStriker
        if (runs % 2 == 1) {
            nextStriker = nonStriker
            nextNonStriker = striker
        }

        // New batter takes the dismissed batter's place (at whichever end they ended up)
        if (dismissed != null) {
            val replacement = incomingBatter ?: ""
            if (nextStriker == dismissed) nextStriker = replacement
            else if (nextNonStriker == dismissed) nextNonStriker = replacement
        }

        val newLegalCount = if (isWideOrNb) legalCount else legalCount + 1
        val overComplete = !isWideOrNb && newLegalCount % 6 == 0
        if (overComplete) {
            val t = nextStriker; nextStriker = nextNonStriker; nextNonStriker = t
        }

        viewModelScope.launch {
            val updated = match.copy(
                ballsJson = JsonHelper.listToJson(balls, BallInfo::class.java),
                currentStriker = nextStriker,
                currentNonStriker = nextNonStriker
            )

            // Celebrate first, then settle state
            when {
                isWicket -> _events.tryEmit(ScoreEvent.Wicket(dismissed ?: striker))
                extraType == null && runs == 6 -> _events.tryEmit(ScoreEvent.Six)
                extraType == null && runs == 4 -> _events.tryEmit(ScoreEvent.Four)
                extraType == "NO_BALL" && runs == 6 -> _events.tryEmit(ScoreEvent.Six)
                extraType == "NO_BALL" && runs == 4 -> _events.tryEmit(ScoreEvent.Four)
            }

            val ended = settleInnings(updated, balls, forceEnd = false)
            if (!ended && overComplete) {
                val card = CricketScorer.calculateInningsScorecard(balls, inningsId, emptyList())
                _events.tryEmit(ScoreEvent.OverComplete(newLegalCount / 6, card.overs.lastOrNull()?.runs ?: 0))
                showBowlerSelectDialog.value = true
            }
        }
    }

    /**
     * Saves [updated] and moves the match to the next innings or to completion if needed.
     * Returns true when the innings (or match) ended.
     */
    private suspend fun settleInnings(updated: MatchEntity, balls: List<BallInfo>, forceEnd: Boolean): Boolean {
        val inningsId = updated.currentInnings
        val battingRoster = rosterFor(battingTeamId(updated))
        val card = CricketScorer.calculateInningsScorecard(balls, inningsId, battingRoster)
        val maxWickets = maxWicketsFor(battingRoster)

        var inningsEnd = forceEnd ||
            card.totalWickets >= maxWickets ||
            card.legalBallsBowled >= updated.overs * 6
        var matchEnd = false

        if (inningsId == 2) {
            val first = CricketScorer.calculateInningsScorecard(balls, 1, emptyList())
            if (card.totalRuns > first.totalRuns) {
                inningsEnd = true
                matchEnd = true
            } else if (inningsEnd) {
                matchEnd = true
            }
        }

        when {
            matchEnd -> {
                val completed = completeMatch(updated, balls)
                repository.insertMatch(completed)
                repository.updatePlayerStatsFromCompletedMatch(completed)
                navigateTo("scorecard")
            }
            inningsEnd && inningsId == 1 -> {
                val target = card.totalRuns + 1
                val newBattingId = battingTeamId(updated, 2)
                val newBatting = rosterFor(newBattingId)
                val newBowling = rosterFor(bowlingTeamId(updated, 2))
                repository.insertMatch(
                    updated.copy(
                        currentInnings = 2,
                        targetRuns = target,
                        currentStriker = newBatting.getOrNull(0) ?: "Batter 1",
                        currentNonStriker = newBatting.getOrNull(1) ?: "Batter 2",
                        currentBowler = newBowling.getOrNull(0) ?: "Bowler 1"
                    )
                )
                showBowlerSelectDialog.value = false
                showDismissalDialog.value = false
                _events.tryEmit(ScoreEvent.InningsBreak(target, teamName(updated, newBattingId)))
            }
            else -> repository.insertMatch(updated)
        }
        return inningsEnd
    }

    private fun completeMatch(match: MatchEntity, balls: List<BallInfo>): MatchEntity {
        val firstRoster = rosterFor(battingTeamId(match, 1))
        val secondRoster = rosterFor(battingTeamId(match, 2))
        val first = CricketScorer.calculateInningsScorecard(balls, 1, firstRoster)
        val second = CricketScorer.calculateInningsScorecard(balls, 2, secondRoster)

        val firstId = battingTeamId(match, 1)
        val secondId = battingTeamId(match, 2)
        val (winnerId, result) = when {
            first.totalRuns > second.totalRuns ->
                firstId to "${teamName(match, firstId)} won by ${first.totalRuns - second.totalRuns} runs"
            second.totalRuns > first.totalRuns -> {
                val left = maxWicketsFor(secondRoster) - second.totalWickets
                secondId to "${teamName(match, secondId)} won by $left wicket${if (left == 1) "" else "s"}"
            }
            else -> null to "Match tied"
        }

        return match.copy(
            status = "COMPLETED",
            winnerTeamId = winnerId,
            resultText = result,
            topPerformer = pickPlayerOfMatch(first, second)
        )
    }

    /** Runs plus 20 per wicket, across both innings. */
    private fun pickPlayerOfMatch(first: InningsScorecard, second: InningsScorecard): String {
        data class Impact(var runs: Int = 0, var balls: Int = 0, var wkts: Int = 0, var conceded: Int = 0)
        val impact = mutableMapOf<String, Impact>()
        for (card in listOf(first, second)) {
            card.batsmen.values.filter { it.hasBatted }.forEach {
                impact.getOrPut(it.name) { Impact() }.apply { runs += it.runs; balls += it.ballsFaced }
            }
            card.bowlers.values.forEach {
                impact.getOrPut(it.name) { Impact() }.apply { wkts += it.wickets; conceded += it.runsConceded }
            }
        }
        val best = impact.maxByOrNull { (_, v) -> v.runs + v.wkts * 20 } ?: return ""
        val v = best.value
        val parts = buildList {
            if (v.balls > 0 || v.runs > 0) add("${v.runs} (${v.balls})")
            if (v.wkts > 0) add("${v.wkts}/${v.conceded}")
        }
        return "${best.key} ${parts.joinToString(" & ")}".trim()
    }

    /** Ends the current innings right now (bad light, time up, ball lost…). */
    fun endInningsNow() {
        val match = activeMatch.value ?: return
        if (match.status != "ONGOING") return
        viewModelScope.launch { settleInnings(match, balls(match), forceEnd = true) }
    }

    fun swapStrike() {
        val match = activeMatch.value ?: return
        viewModelScope.launch {
            repository.insertMatch(
                match.copy(currentStriker = match.currentNonStriker, currentNonStriker = match.currentStriker)
            )
        }
    }

    /** Put a different batter at one end (e.g. picking openers or correcting a mistake). */
    fun setBatter(name: String, atStrikerEnd: Boolean) {
        val match = activeMatch.value ?: return
        viewModelScope.launch {
            val updated = when {
                atStrikerEnd && name == match.currentNonStriker ->
                    match.copy(currentStriker = name, currentNonStriker = match.currentStriker)
                !atStrikerEnd && name == match.currentStriker ->
                    match.copy(currentNonStriker = name, currentStriker = match.currentNonStriker)
                atStrikerEnd -> match.copy(currentStriker = name)
                else -> match.copy(currentNonStriker = name)
            }
            repository.insertMatch(updated)
        }
    }

    fun selectNewBowler(name: String) {
        val match = activeMatch.value ?: return
        viewModelScope.launch {
            repository.insertMatch(match.copy(currentBowler = name))
            showBowlerSelectDialog.value = false
        }
    }

    /** Removes the last delivery and puts the players back exactly where they were. */
    fun undoLastBall() {
        val match = activeMatch.value ?: return
        val balls = balls(match).toMutableList()
        if (balls.isEmpty()) return
        val undone = balls.removeAt(balls.size - 1)

        viewModelScope.launch {
            repository.insertMatch(
                match.copy(
                    ballsJson = JsonHelper.listToJson(balls, BallInfo::class.java),
                    currentStriker = undone.striker,
                    currentNonStriker = undone.nonStriker,
                    currentBowler = undone.bowler,
                    currentInnings = undone.innings,
                    targetRuns = if (undone.innings == 1) 0 else match.targetRuns
                )
            )
            showBowlerSelectDialog.value = false
        }
    }

    /** [index] is the position in the full ball list of the match. */
    fun editBallAt(index: Int, editedBall: BallInfo) {
        val match = activeMatch.value ?: return
        val balls = balls(match).toMutableList()
        if (index !in balls.indices) return
        balls[index] = editedBall
        viewModelScope.launch {
            repository.insertMatch(match.copy(ballsJson = JsonHelper.listToJson(balls, BallInfo::class.java)))
        }
    }

    fun deleteMatch(id: Int) {
        viewModelScope.launch {
            if (_activeMatchId.value == id) _activeMatchId.value = null
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
