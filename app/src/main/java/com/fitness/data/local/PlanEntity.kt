package com.fitness.data.local

import androidx.room.*

@Entity(tableName = "training_plans")
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val exercisesJson: String, // 动作 ID 列表，如 "benchpress,pushup"
    val isCurrent: Boolean,
    val version: Int,
    val createdAt: Long
)

@Dao
interface PlanDao {
    @Query("SELECT * FROM training_plans WHERE isCurrent = 1 LIMIT 1")
    suspend fun getCurrentPlan(): PlanEntity?

    @Query("SELECT * FROM training_plans WHERE createdAt <= :timestamp ORDER BY createdAt DESC LIMIT 1")
    suspend fun getPlanForTimestamp(timestamp: Long): PlanEntity?

    @Query("SELECT * FROM training_plans ORDER BY createdAt DESC")
    suspend fun getAllPlans(): List<PlanEntity>

    @Insert
    suspend fun insertPlan(plan: PlanEntity)

    @Query("UPDATE training_plans SET isCurrent = 0 WHERE isCurrent = 1")
    suspend fun archiveCurrentPlans()

    @Transaction
    suspend fun updatePlan(newPlan: PlanEntity) {
        archiveCurrentPlans()
        insertPlan(newPlan)
    }
}
