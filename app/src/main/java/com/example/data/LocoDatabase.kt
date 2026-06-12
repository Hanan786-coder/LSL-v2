package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TournamentEntity::class,
        TeamEntity::class,
        MatchEntity::class,
        PlayerStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LocoDatabase : RoomDatabase() {
    abstract fun locoDao(): LocoDao

    companion object {
        @Volatile
        private var INSTANCE: LocoDatabase? = null

        fun getDatabase(context: Context): LocoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocoDatabase::class.java,
                    "loco_cricket_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
