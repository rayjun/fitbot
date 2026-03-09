package com.fitness.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class AuthManager(private val context: Context) {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    actual val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    actual val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        // Initialize from last signed in account
        GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            _currentUser.value = UserProfile(
                id = account.id ?: "",
                name = account.displayName,
                email = account.email,
                photoUrl = account.photoUrl?.toString()
            )
        }
    }

    actual suspend fun signIn() {
        // Android specific: handled by ActivityResultLauncher in NavHost
    }

    actual suspend fun restoreSignIn() {
        GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            _currentUser.value = UserProfile(
                id = account.id ?: "",
                name = account.displayName,
                email = account.email,
                photoUrl = account.photoUrl?.toString()
            )
        }
    }

    actual suspend fun signOut() {
        _currentUser.value = null
    }
    
    fun updateProfile(profile: UserProfile?) {
        _currentUser.value = profile
    }

    actual suspend fun sync() {
        // Android sync is handled by WorkManager SyncWorker
    }
}
