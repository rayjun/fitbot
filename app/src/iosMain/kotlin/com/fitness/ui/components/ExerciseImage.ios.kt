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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.readResourceBytes
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.CGImageSourceGetCount
import platform.UIKit.UIImage
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

@OptIn(ExperimentalResourceApi::class, ExperimentalForeignApi::class, InternalResourceApi::class)
@Composable
actual fun ExerciseImage(
    gifResPath: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    var animatedImage by remember(gifResPath) { mutableStateOf<UIImage?>(null) }

    LaunchedEffect(gifResPath) {
        try {
            val bytes = readResourceBytes("files/$gifResPath")
            animatedImage = decodeGif(bytes)
        } catch (e: Exception) {
            println("ExerciseImage: failed to load files/$gifResPath: $e")
        }
    }

    val image = animatedImage
    if (image != null) {
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
            update = { view ->
                view.image = image
                if ((image.images()?.size ?: 0) > 1) {
                    view.startAnimating()
                }
            }
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun decodeGif(bytes: ByteArray): UIImage? {
    // Fallback: plain UIImage from NSData
    val nsData: NSData = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    } ?: return null
    val singleImage = UIImage.imageWithData(nsData) ?: return null

    // Build CFDataRef directly from the byte array (avoids NSData toll-free-bridge cast)
    val cfData = bytes.usePinned { pinned ->
        CFDataCreate(kCFAllocatorDefault, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
    } ?: return singleImage

    val source = CGImageSourceCreateWithData(cfData, null)
    CFRelease(cfData)
    if (source == null) return singleImage

    val frameCount = CGImageSourceGetCount(source).toInt()
    if (frameCount <= 1) return singleImage

    val frames = mutableListOf<UIImage>()
    for (i in 0 until frameCount) {
        val cgImage = CGImageSourceCreateImageAtIndex(source, i.toULong(), null) ?: continue
        frames.add(UIImage.imageWithCGImage(cgImage))
    }

    if (frames.isEmpty()) return singleImage

    val totalDuration = frames.size * 0.08
    return UIImage.animatedImageWithImages(frames, totalDuration) ?: singleImage
}
