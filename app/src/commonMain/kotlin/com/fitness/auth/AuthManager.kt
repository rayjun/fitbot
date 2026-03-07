package com.fitness.auth

import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val id: String,
    val name: String?,
    val email: String?,
    val photoUrl: String?
)

expect class AuthManager {
    val currentUser: StateFlow<UserProfile?>
    val isSyncing: StateFlow<Boolean>
    
    suspend fun signIn()
    suspend fun signOut()
}
