package com.fitness.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.fitness.R

@Composable
actual fun getString(key: String): String {
    val context = LocalContext.current
    val resId = remember(key) { key.toResId(context) }
    return if (resId != 0) {
        stringResource(resId)
    } else {
        // Fallback for keys that might not be found by getIdentifier but exist as R.string
        // This is a safety net
        when (key) {
            "nav_library" -> stringResource(R.string.nav_library)
            "category_all" -> stringResource(R.string.category_all)
            else -> key
        }
    }
}
