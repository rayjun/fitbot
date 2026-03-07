package com.fitness.util

import androidx.compose.runtime.Composable

@Composable
actual fun getString(key: String): String {
    // Placeholder for iOS: Return the key for now.
    // In a real app, this would use NSLocalizedString or CMP Resources.
    return key
}
