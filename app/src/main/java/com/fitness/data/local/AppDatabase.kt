package com.fitness.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SetEntity::class, PlanEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun planDao(): PlanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercise_sets ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE exercise_sets ADD COLUMN distance REAL")
                database.execSQL("ALTER TABLE exercise_sets ADD COLUMN duration INTEGER")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_db"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration() // 核心修复：防止因唯一索引冲突导致的启动闪退
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
