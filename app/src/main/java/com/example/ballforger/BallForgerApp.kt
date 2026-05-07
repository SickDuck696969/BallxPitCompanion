package com.example.ballforger

import android.app.Application
import androidx.room.Room
import com.example.ballforger.data.AppDatabase

class BallForgerApp : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "ball_forger_db"
        ).build()
    }
}