package com.fitness.auth

interface GoogleAuthCallback {
    fun onSignInSuccess(userId: String, userName: String?, userEmail: String?, photoUrl: String?, accessToken: String)
    fun onSignInFailed(error: String)
}
