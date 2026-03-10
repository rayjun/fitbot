package com.fitness.model

import kotlinx.serialization.Serializable

/**
 * 代表一天的所有训练数据，用于 JSON 序列化同步到 Google Drive
 */
@Serializable
data class TrainingDay(
    val date: String,
    val sessions: List<TrainingSession>
)

@Serializable
data class TrainingSession(
    val sessionId: String,
    val startTime: String,
    val endTime: String,
    val exercises: List<ExerciseRecord>
)

@Serializable
data class ExerciseRecord(
    val name: String,
    val sets: List<SetRecord>
)

@Serializable
data class SetRecord(
    val reps: Int,
    val weight: Double,
    val time: String,
    val remoteId: String,
    val isDeleted: Boolean = false
)

fun mergeTrainingDays(local: TrainingDay, remote: TrainingDay): TrainingDay {
    if (local.date != remote.date) return local

    val allSessions = mutableMapOf<String, TrainingSession>()
    
    // Add remote sessions first
    remote.sessions.forEach { allSessions[it.sessionId] = it }
    
    // Merge local sessions
    local.sessions.forEach { localSession ->
        val remoteSession = allSessions[localSession.sessionId]
        if (remoteSession == null) {
            allSessions[localSession.sessionId] = localSession
        } else {
            val allExercises = mutableMapOf<String, ExerciseRecord>()
            remoteSession.exercises.forEach { allExercises[it.name] = it }
            
            localSession.exercises.forEach { localExercise ->
                val remoteExercise = allExercises[localExercise.name]
                if (remoteExercise == null) {
                    allExercises[localExercise.name] = localExercise
                } else {
                    val remoteIds = remoteExercise.sets.map { it.remoteId }.filter { it.isNotEmpty() }.toSet()
                    val remoteKeys = remoteExercise.sets.map { "${localExercise.name}|${it.time}" }.toSet()
                    
                    val mergedSets = remoteExercise.sets.toMutableList()
                    localExercise.sets.forEach { localSet ->
                        val dedupKey = "${localExercise.name}|${localSet.time}"
                        val exists = (localSet.remoteId.isNotEmpty() && remoteIds.contains(localSet.remoteId)) || remoteKeys.contains(dedupKey)
                        if (!exists) {
                            mergedSets.add(localSet)
                        } else {
                            // If it exists, let local tombstone win over remote active (simplest CRDT-like approach)
                            if (localSet.isDeleted) {
                                val idx = mergedSets.indexOfFirst { it.remoteId == localSet.remoteId || "${localExercise.name}|${it.time}" == dedupKey }
                                if (idx != -1) {
                                    mergedSets[idx] = localSet
                                }
                            }
                        }
                    }
                    allExercises[localExercise.name] = ExerciseRecord(localExercise.name, mergedSets.sortedBy { it.time })
                }
            }
            
            allSessions[localSession.sessionId] = TrainingSession(
                sessionId = localSession.sessionId,
                startTime = if (localSession.startTime < remoteSession.startTime) localSession.startTime else remoteSession.startTime,
                endTime = if (localSession.endTime > remoteSession.endTime) localSession.endTime else remoteSession.endTime,
                exercises = allExercises.values.toList()
            )
        }
    }
    
    return TrainingDay(local.date, allSessions.values.toList())
}
