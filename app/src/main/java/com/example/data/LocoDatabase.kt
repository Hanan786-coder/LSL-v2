package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TournamentEntity::class,
        TeamEntity::class,
        MatchEntity::class,
        PlayerStatsEntity::class,
        PlayerEntity::class,
        TeamMemberEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class LocoDatabase : RoomDatabase() {
    abstract fun locoDao(): LocoDao

    companion object {
        /**
         * v1 -> v2: saved players and team memberships (role, captain, order).
         * Every existing team's comma-separated roster is copied in, so nothing is lost.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `players` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `createdTime` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `team_members` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`teamId` INTEGER NOT NULL, `playerId` INTEGER NOT NULL, `role` TEXT NOT NULL, " +
                        "`isCaptain` INTEGER NOT NULL, `position` INTEGER NOT NULL)"
                )
                val now = System.currentTimeMillis()
                val ids = mutableMapOf<String, Long>()
                db.query("SELECT id, playerNames FROM teams ORDER BY id").use { c ->
                    while (c.moveToNext()) {
                        val teamId = c.getLong(0)
                        val names = (c.getString(1) ?: "").split(",", "\n").map { it.trim() }.filter { it.isNotEmpty() }
                        names.distinctBy { it.lowercase() }.forEachIndexed { pos, name ->
                            val key = name.lowercase()
                            val playerId = ids.getOrPut(key) {
                                db.execSQL("INSERT INTO players (name, createdTime) VALUES (?, ?)", arrayOf<Any>(name, now))
                                db.query("SELECT last_insert_rowid()").use { r -> r.moveToFirst(); r.getLong(0) }
                            }
                            db.execSQL(
                                "INSERT INTO team_members (teamId, playerId, role, isCaptain, position) VALUES (?, ?, 'UNSET', 0, ?)",
                                arrayOf<Any>(teamId, playerId, pos)
                            )
                        }
                    }
                }
            }
        }

        @Volatile
        private var INSTANCE: LocoDatabase? = null

        fun getDatabase(context: Context): LocoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocoDatabase::class.java,
                    "loco_cricket_database"
                )
                .addMigrations(MIGRATION_1_2)
                // No destructive fallback: a schema change must ship a Migration,
                // so an app update can never silently wipe saved matches and stats.
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
