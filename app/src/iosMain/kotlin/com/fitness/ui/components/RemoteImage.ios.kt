package com.fitness.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

private val imageHttpClient = HttpClient(Darwin)

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    var image by remember(url) { mutableStateOf<UIImage?>(null) }

    LaunchedEffect(url) {
        try {
            val bytes = imageHttpClient.get(url).readBytes()
            val nsData: NSData = bytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            }
            image = UIImage.imageWithData(nsData)
        } catch (_: Exception) {}
    }

    val img = image
    if (img != null) {
        UIKitView(
            modifier = modifier.fillMaxSize(),
            factory = {
                UIImageView().apply {
                    contentMode = when (contentScale) {
                        ContentScale.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
                        ContentScale.FillBounds -> UIViewContentMode.UIViewContentModeScaleToFill
                        else -> UIViewContentMode.UIViewContentModeScaleAspectFit
                    }
                    clipsToBounds = true
                }
            },
            update = { view -> view.image = img }
        )
    }
}
