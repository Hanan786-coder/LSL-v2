package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- Data Models (Serialized representation) ---

data class BallInfo(
    val id: String = java.util.UUID.randomUUID().toString(),
    val overIndex: Int,          // 0, 1, 2...
    val ballNumber: Int,         // 1 to 6 (of legal balls in that over)
    val runs: Int,               // runs off bat
    val extraRuns: Int,          // runs from extras (e.g., 1 for wide, 1 for no ball)
    val isExtra: Boolean,
    val extraType: String?,      // "WIDE", "NO_BALL", "BYE", "LEG_BYE"
    val isWicket: Boolean,
    val wicketType: String?,     // "BOWLED", "CAUGHT", "RUN_OUT", "STUMPED", "LBW"
    val striker: String,
    val nonStriker: String,
    val bowler: String,
    val dismissedPlayer: String?,
    val innings: Int,            // 1 or 2
    val timestamp: Long = System.currentTimeMillis(),
    // Who took the catch / effected the run out or stumping. Optional so ball logs
    // saved by earlier versions still load unchanged (no database migration needed).
    val fielder: String? = null
)

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val tournamentId: Int? = null,
    val playerNames: String, // Comma-separated or JSON list of player names
    val createdTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tournamentId: Int? = null,
    val teamAId: Int,
    val teamBId: Int,
    val teamAName: String,
    val teamBName: String,
    val overs: Int,                 // Configured overs (e.g., 2, 5, 10, 20)
    val status: String,             // "ONGOING", "COMPLETED"
    val currentInnings: Int = 1,    // 1 or 2
    val battingFirstTeamId: Int,    // ID of team that bat first
    val currentStriker: String = "",
    val currentNonStriker: String = "",
    val currentBowler: String = "",
    val targetRuns: Int = 0,        // Targets to win (set in 2nd innings)
    val winnerTeamId: Int? = null,
    val resultText: String = "",    // "Team A won by 20 runs"
    val topPerformer: String = "",  // Player name + summary of stats
    val ballsJson: String = "[]",    // Serialized List<BallInfo>
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val playerName: String,
    val teamId: Int,
    val teamName: String,
    val matchesPlayed: Int = 0,
    val runsScored: Int = 0,
    val ballsFaced: Int = 0,
    val wicketsTaken: Int = 0,
    val runsConceded: Int = 0,
    val ballsBowled: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0
)

/** A saved player. One person can be a member of many teams. */
@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdTime: Long = System.currentTimeMillis()
)

/** A player's place in one team: their role there, captaincy and batting position. */
@Entity(tableName = "team_members")
data class TeamMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val playerId: Int,
    val role: String = PlayerRole.UNSET.name,
    val isCaptain: Boolean = false,
    val position: Int = 0
)

enum class PlayerRole(val label: String, val short: String) {
    BATTER("Batter", "Bat"),
    BOWLER("Bowler", "Bowl"),
    ALL_ROUNDER("All-rounder", "AR"),
    KEEPER("Keeper", "WK"),
    UNSET("No role", "");

    companion object {
        fun of(value: String?): PlayerRole = entries.firstOrNull { it.name == value } ?: UNSET
        val choices = listOf(BATTER, BOWLER, ALL_ROUNDER, KEEPER)
    }
}
