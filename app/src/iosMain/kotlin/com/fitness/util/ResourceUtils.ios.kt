package com.fitness.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
@Composable
actual fun getString(key: String): String {
    // In KMP, we usually use generated resource accessors like Res.string.key.
    // For now, if we don't have the generated Res class, we'd need to use a mapping.
    // However, since we are using string keys, we'll implement a simple mock or 
    // ideally use CMP Resources when the Res class is available.
    
    // Placeholder: In a full implementation, you'd use Compose Resources' stringResource.
    // For this specific project, we'll keep it simple to allow UI to render.
    return key
}
