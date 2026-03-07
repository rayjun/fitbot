package com.fitness.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class AuthManager {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    actual val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    actual val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    actual suspend fun signIn() {
        // Native iOS Google Sign-In would be triggered here
        // For now, we mock the result to enable full logic flow
        _currentUser.value = UserProfile(
            id = "ios_mock_id",
            name = "iOS Fitness Pro",
            email = "ios@fitness.com",
            photoUrl = null
        )
    }

    actual suspend fun signOut() {
        _currentUser.value = null
    }
}
