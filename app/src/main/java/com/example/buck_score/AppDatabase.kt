package com.example.buck_score

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScorecardEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scorecardDao(): ScorecardDao
}