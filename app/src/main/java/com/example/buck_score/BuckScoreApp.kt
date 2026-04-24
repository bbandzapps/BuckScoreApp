package com.example.buck_score

import android.app.Application
import androidx.room.Room

class BuckScoreApp : Application() {

    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "buck_score_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    val dao by lazy {
        database.scorecardDao()
    }
}
