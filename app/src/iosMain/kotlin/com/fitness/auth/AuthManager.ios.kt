package com.fitness.auth

import com.fitness.data.DataStoreRepository
import com.fitness.sync.IosDriveSyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class AuthManager(private val repository: DataStoreRepository) {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    actual val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    actual val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var currentAccessToken: String? = null

    /**
     * Set by Swift's GoogleSignInBridge before any sign-in is attempted.
     * The launcher invokes the callback on the main thread.
     */
    var signInLauncher: ((GoogleAuthCallback) -> Unit)? = null

    actual suspend fun signIn() {
        suspendCancellableCoroutine { cont ->
            val launcher = signInLauncher
            if (launcher == null) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }
            launcher(object : GoogleAuthCallback {
                override fun onSignInSuccess(
                    userId: String,
                    userName: String?,
                    userEmail: String?,
                    photoUrl: String?,
                    accessToken: String
                ) {
                    currentAccessToken = accessToken
                    _currentUser.value = UserProfile(
                        id = userId,
                        name = userName,
                        email = userEmail,
                        photoUrl = photoUrl
                    )
                    cont.resume(Unit)
                }

                override fun onSignInFailed(error: String) {
                    cont.resume(Unit)
                }
            })
        }
    }

    actual suspend fun signOut() {
        currentAccessToken = null
        _currentUser.value = null
    }

    actual suspend fun sync() {
        val token = currentAccessToken ?: return
        _isSyncing.value = true
        try {
            IosDriveSyncEngine(token, repository).sync()
        } finally {
            _isSyncing.value = false
        }
    }
}
