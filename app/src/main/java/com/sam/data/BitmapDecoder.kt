package com.sam.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Size

object BitmapDecoder {

    fun readDimensions(context: Context, uri: Uri): Pair<Int, Int>? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null
        return if (bounds.outWidth > 0 && bounds.outHeight > 0) {
            bounds.outWidth to bounds.outHeight
        } else {
            null
        }
    }

    fun decodeSquareThumbnail(context: Context, uri: Uri, size: Int): Bitmap? {
        val decoded = decodeWithThumbnailApi(context, uri, size)
            ?: decodeWithImageDecoder(context, uri, size)
            ?: decodeWithBitmapFactory(context, uri, size)
            ?: return null
        return normalizeToSquare(decoded, size)
    }

    private fun normalizeToSquare(bitmap: Bitmap, size: Int): Bitmap {
        if (bitmap.width == size && bitmap.height == size) {
            return bitmap
        }
        val normalized = Bitmap.createScaledBitmap(bitmap, size, size, true)
        if (normalized !== bitmap) {
            bitmap.recycle()
        }
        return normalized
    }

    private fun decodeWithThumbnailApi(context: Context, uri: Uri, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            context.contentResolver.loadThumbnail(uri, Size(size, size), null)
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeWithImageDecoder(context: Context, uri: Uri, size: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return try {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setTargetSize(size, size)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeWithBitmapFactory(context: Context, uri: Uri, size: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(bounds, size)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        return decoded
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, targetSize: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > targetSize || width > targetSize) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= targetSize && halfWidth / inSampleSize >= targetSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
