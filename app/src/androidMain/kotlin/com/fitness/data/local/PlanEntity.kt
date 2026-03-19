package com.fitness.data.local

import androidx.room.*
import kotlinx.serialization.Serializable

@Entity(
    tableName = "training_plans",
    indices = [Index(value = ["createdAt"], unique = true)] // 核心修复：防止同步产生重复计划
)
@Serializable
data class PlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val exercisesJson: String, 
    val isCurrent: Boolean,
    val version: Int,
    val createdAt: Long // 作为唯一的版本标识
)

fun PlanEntity.toModel() = com.fitness.model.WorkoutPlan(
    id = id,
    name = name,
    exercisesJson = exercisesJson,
    createdAt = createdAt,
    isCurrent = isCurrent
)

fun com.fitness.model.WorkoutPlan.toEntity() = PlanEntity(
    id = id,
    name = name,
    exercisesJson = exercisesJson,
    isCurrent = isCurrent,
    version = 1,
    createdAt = createdAt
)

@Dao
interface PlanDao {
    @Query("SELECT * FROM training_plans WHERE isCurrent = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentPlan(): PlanEntity?

    @Query("SELECT * FROM training_plans WHERE isCurrent = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getCurrentPlanFlow(): kotlinx.coroutines.flow.Flow<PlanEntity?>

    @Query("SELECT * FROM training_plans WHERE createdAt <= :timestamp ORDER BY createdAt DESC LIMIT 1")
    suspend fun getPlanForTimestamp(timestamp: Long): PlanEntity?

    @Query("SELECT * FROM training_plans ORDER BY createdAt DESC")
    suspend fun getAllPlans(): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) // 确保版本更新时覆盖或跳过重复
    suspend fun insertPlan(plan: PlanEntity)

    @Query("UPDATE training_plans SET isCurrent = 0 WHERE isCurrent = 1")
    suspend fun archiveCurrentPlans()

    @Transaction
    suspend fun updatePlan(newPlan: PlanEntity) {
        archiveCurrentPlans()
        insertPlan(newPlan)
    }
}
