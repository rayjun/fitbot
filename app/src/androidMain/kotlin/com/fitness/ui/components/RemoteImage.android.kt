@file:JvmName("RemoteImageAndroid")
package com.fitness.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
actual fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
