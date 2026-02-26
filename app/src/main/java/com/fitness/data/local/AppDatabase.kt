package com.fitness.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SetEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}
