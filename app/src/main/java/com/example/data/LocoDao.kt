package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocoDao {

    // --- Tournaments ---
    @Query("SELECT * FROM tournaments ORDER BY createdTime DESC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: Int): TournamentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity): Long

    @Delete
    suspend fun deleteTournament(tournament: TournamentEntity)

    // --- Teams ---
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE tournamentId = :tournamentId ORDER BY name ASC")
    fun getTeamsByTournament(tournamentId: Int): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamById(id: Int): TeamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity): Long

    @Delete
    suspend fun deleteTeam(team: TeamEntity)

    // --- Matches ---
    @Query("SELECT * FROM matches ORDER BY date DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE tournamentId = :tournamentId ORDER BY date DESC")
    fun getMatchesByTournament(tournamentId: Int): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatchById(id: Int)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    // --- Player Stats ---
    @Query("SELECT * FROM player_stats ORDER BY runsScored DESC, wicketsTaken DESC")
    fun getAllPlayerStats(): Flow<List<PlayerStatsEntity>>

    @Query("SELECT * FROM player_stats WHERE teamId = :teamId ORDER BY runsScored DESC")
    fun getPlayerStatsByTeam(teamId: Int): Flow<List<PlayerStatsEntity>>

    @Query("SELECT * FROM player_stats WHERE playerName = :name AND teamId = :teamId")
    suspend fun getPlayerStatsByNameAndTeam(name: String, teamId: Int): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayerStats(stats: PlayerStatsEntity): Long

    @Update
    suspend fun updatePlayerStats(stats: PlayerStatsEntity)
}
