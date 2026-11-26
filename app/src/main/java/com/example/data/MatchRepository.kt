package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MatchRepository(private val locoDao: LocoDao) {

    // --- Tournaments ---
    val allTournaments: Flow<List<TournamentEntity>> = locoDao.getAllTournaments()
    
    suspend fun getTournamentById(id: Int): TournamentEntity? = locoDao.getTournamentById(id)
    
    suspend fun insertTournament(tournament: TournamentEntity): Long = locoDao.insertTournament(tournament)
    
    suspend fun deleteTournament(tournament: TournamentEntity) = locoDao.deleteTournament(tournament)

    // --- Teams ---
    val allTeams: Flow<List<TeamEntity>> = locoDao.getAllTeams()
    
    fun getTeamsByTournament(tournamentId: Int): Flow<List<TeamEntity>> = locoDao.getTeamsByTournament(tournamentId)
    
    suspend fun getTeamById(id: Int): TeamEntity? = locoDao.getTeamById(id)
    
    suspend fun insertTeam(team: TeamEntity): Long = locoDao.insertTeam(team)
    
    suspend fun deleteTeam(team: TeamEntity) = locoDao.deleteTeam(team)

    // --- Matches ---
    val allMatches: Flow<List<MatchEntity>> = locoDao.getAllMatches()
    
    fun getMatchesByTournament(tournamentId: Int): Flow<List<MatchEntity>> = locoDao.getMatchesByTournament(tournamentId)
    
    suspend fun getMatchById(id: Int): MatchEntity? = locoDao.getMatchById(id)
    
    suspend fun insertMatch(match: MatchEntity): Long = locoDao.insertMatch(match)
    
    suspend fun deleteMatchById(id: Int) = locoDao.deleteMatchById(id)
    
    suspend fun deleteMatch(match: MatchEntity) = locoDao.deleteMatch(match)

    // --- Player Stats ---
    val allPlayerStats: Flow<List<PlayerStatsEntity>> = locoDao.getAllPlayerStats()
    
    fun getPlayerStatsByTeam(teamId: Int): Flow<List<PlayerStatsEntity>> = locoDao.getPlayerStatsByTeam(teamId)
    
    suspend fun insertPlayerStats(stats: PlayerStatsEntity) = locoDao.insertPlayerStats(stats)
    
    suspend fun updatePlayerStats(stats: PlayerStatsEntity) = locoDao.updatePlayerStats(stats)

    // --- Save/Update accumulated stats when a match completes ---
    suspend fun updatePlayerStatsFromCompletedMatch(match: MatchEntity) {
        val balls = JsonHelper.listFromJson(match.ballsJson, BallInfo::class.java)
        if (balls.isEmpty()) return

        // Extract and aggregate stats from the ball list
        val batsmanStats = mutableMapOf<String, Pair<Int, Int>>() // Name -> Pair(Runs, BallsFaced)
        val bowlerStats = mutableMapOf<String, Pair<Int, Int>>() // Name -> Pair(Wickets, RunsConceded) // ballsBowled calculated from legal balls
        val bowlerBalls = mutableMapOf<String, Int>() // Name -> Count of legal balls
        val boundaries = mutableMapOf<String, Pair<Int, Int>>() // Name -> Pair(Fours, Sixes)

        for (ball in balls) {
            // Batsman scoring
            val batName = ball.striker
            val currentBat = batsmanStats.getOrDefault(batName, Pair(0, 0))
            var runsContributed = ball.runs
            val facedIncrement = if (ball.extraType == "WIDE") 0 else 1
            
            // In cricket, Wide deliveries are not balls faced by the batsman
            batsmanStats[batName] = Pair(currentBat.first + runsContributed, currentBat.second + facedIncrement)

            // Boundaries
            val bnd = boundaries.getOrDefault(batName, Pair(0, 0))
            if (ball.runs == 4) {
                boundaries[batName] = Pair(bnd.first + 1, bnd.second)
            } else if (ball.runs == 6) {
                boundaries[batName] = Pair(bnd.first, bnd.second + 1)
            }

            // Bowler bowling
            val bowlName = ball.bowler
            val currentBowl = bowlerStats.getOrDefault(bowlName, Pair(0, 0))
            val isLegal = ball.extraType != "WIDE" && ball.extraType != "NO_BALL"
            val bowlerRunsConceded = ball.runs + (if (ball.extraType == "WIDE" || ball.extraType == "NO_BALL") ball.extraRuns else 0)
            
            val bowlWkts = if (ball.isWicket && ball.wicketType != "RUN_OUT" && ball.wicketType != "RETIRED") 1 else 0
            bowlerStats[bowlName] = Pair(currentBowl.first + bowlWkts, currentBowl.second + bowlerRunsConceded)
            if (isLegal) {
                bowlerBalls[bowlName] = bowlerBalls.getOrDefault(bowlName, 0) + 1
            }
        }

        // Now save to database
        val teamA = locoDao.getTeamById(match.teamAId)
        val teamB = locoDao.getTeamById(match.teamBId)

        // For Team A players
        teamA?.let { team ->
            val listPlayers = team.playerNames.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
            for (pName in listPlayers) {
                val stats = batsmanStats[pName]
                val bwl = bowlerStats[pName]
                val bBalls = bowlerBalls[pName] ?: 0
                val bnd = boundaries[pName] ?: Pair(0, 0)
                
                if (stats != null || bwl != null) {
                    val runs = stats?.first ?: 0
                    val ballsFaced = stats?.second ?: 0
                    val wkts = bwl?.first ?: 0
                    val runsCon = bwl?.second ?: 0

                    updateSinglePlayerStats(pName, team.id, team.name, runs, ballsFaced, wkts, runsCon, bBalls, bnd.first, bnd.second)
                }
            }
        }

        // For Team B players
        teamB?.let { team ->
            val listPlayers = team.playerNames.split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
            for (pName in listPlayers) {
                val stats = batsmanStats[pName]
                val bwl = bowlerStats[pName]
                val bBalls = bowlerBalls[pName] ?: 0
                val bnd = boundaries[pName] ?: Pair(0, 0)
                
                if (stats != null || bwl != null) {
                    val runs = stats?.first ?: 0
                    val ballsFaced = stats?.second ?: 0
                    val wkts = bwl?.first ?: 0
                    val runsCon = bwl?.second ?: 0

                    updateSinglePlayerStats(pName, team.id, team.name, runs, ballsFaced, wkts, runsCon, bBalls, bnd.first, bnd.second)
                }
            }
        }
    }

    private suspend fun updateSinglePlayerStats(
        pName: String,
        teamId: Int,
        teamName: String,
        runs: Int,
        ballsFaced: Int,
        wickets: Int,
        runsConceded: Int,
        ballsBowled: Int,
        fours: Int,
        sixes: Int
    ) {
        val existing = locoDao.getPlayerStatsByNameAndTeam(pName, teamId)
        if (existing == null) {
            locoDao.insertPlayerStats(
                PlayerStatsEntity(
                    playerName = pName,
                    teamId = teamId,
                    teamName = teamName,
                    matchesPlayed = 1,
                    runsScored = runs,
                    ballsFaced = ballsFaced,
                    wicketsTaken = wickets,
                    runsConceded = runsConceded,
                    ballsBowled = ballsBowled,
                    fours = fours,
                    sixes = sixes
                )
            )
        } else {
            locoDao.updatePlayerStats(
                existing.copy(
                    matchesPlayed = existing.matchesPlayed + 1,
                    runsScored = existing.runsScored + runs,
                    ballsFaced = existing.ballsFaced + ballsFaced,
                    wicketsTaken = existing.wicketsTaken + wickets,
                    runsConceded = existing.runsConceded + runsConceded,
                    ballsBowled = existing.ballsBowled + ballsBowled,
                    fours = existing.fours + fours,
                    sixes = existing.sixes + sixes
                )
            )
        }
    }

    // --- Prepopulate realistic sample data ---
    suspend fun prepopulateIfEmpty() {
        // App starts clean with no dummy data as requested!
    }
}
