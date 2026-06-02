package com.sam.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

class ImageSimilarityAnalyzer {

    suspend fun findSimilarGroups(
        context: Context,
        images: List<MediaItem>,
        onProgress: (processed: Int, total: Int) -> Unit
    ): DuplicateScanResult = withContext(Dispatchers.Default) {
        if (images.size < 2) {
            return@withContext DuplicateScanResult(
                groups = emptyList(),
                imageCount = images.size,
                fingerprintCount = 0,
                pairCount = 0
            )
        }

        val appContext = context.applicationContext
        val totalSteps = images.size + pairCount(images.size)
        var completedSteps = 0

        val fingerprints = ArrayList<ImageFingerprint>(images.size)
        images.forEachIndexed { index, item ->
            computeFingerprint(appContext, item, index)?.let { fingerprints.add(it) }
            completedSteps++
            onProgress(completedSteps, totalSteps)
        }

        if (fingerprints.size < 2) {
            return@withContext DuplicateScanResult(
                groups = emptyList(),
                imageCount = images.size,
                fingerprintCount = fingerprints.size,
                pairCount = 0
            )
        }

        val unionFind = UnionFind(fingerprints.size)
        val compared = HashSet<Long>()
        var pairComparisons = 0

        fun comparePair(left: Int, right: Int) {
            if (left == right) return
            val first = min(left, right)
            val second = max(left, right)
            val pairKey = (first.toLong() shl 32) or second.toLong()
            if (!compared.add(pairKey)) return

            pairComparisons++
            if (pairComparisons % 500 == 0) {
                completedSteps = min(completedSteps + 1, totalSteps)
                onProgress(completedSteps, totalSteps)
            }

            if (areSimilar(fingerprints[first], fingerprints[second])) {
                unionFind.union(first, second)
            }
        }

        val hashBuckets = mutableMapOf<String, MutableList<Int>>()
        fingerprints.forEachIndexed { index, fingerprint ->
            fingerprint.contentHash?.let { hash ->
                hashBuckets.getOrPut(hash) { mutableListOf() }.add(index)
            }
        }
        hashBuckets.values.filter { it.size >= 2 }.forEach { indices ->
            for (i in indices.indices) {
                for (j in i + 1 until indices.size) {
                    unionFind.union(indices[i], indices[j])
                }
            }
        }

        for (i in fingerprints.indices) {
            for (j in i + 1 until fingerprints.size) {
                comparePair(i, j)
            }
        }

        onProgress(totalSteps, totalSteps)

        val grouped = mutableMapOf<Int, MutableList<Int>>()
        fingerprints.indices.forEach { index ->
            val root = unionFind.find(index)
            grouped.getOrPut(root) { mutableListOf() }.add(index)
        }

        val groups = grouped.values
            .filter { it.size >= 2 }
            .map { indices -> buildGroup(images, fingerprints, indices) }
            .sortedByDescending { it.items.size }

        DuplicateScanResult(
            groups = groups,
            imageCount = images.size,
            fingerprintCount = fingerprints.size,
            pairCount = pairComparisons
        )
    }

    private fun pairCount(imageCount: Int): Int {
        return imageCount * max(imageCount - 1, 0) / 2
    }

    private fun areSimilar(first: ImageFingerprint, second: ImageFingerprint): Boolean {
        if (first.contentHash != null && first.contentHash == second.contentHash) {
            return true
        }

        val averageSimilarity = hashSimilarity(first.averageHash, second.averageHash)
        val differenceSimilarity = hashSimilarity(first.differenceHash, second.differenceHash)
        val verticalSimilarity = hashSimilarity(first.verticalHash, second.verticalHash)
        val bestHashSimilarity = maxOf(averageSimilarity, differenceSimilarity, verticalSimilarity)
        val averageHashSimilarity = (averageSimilarity + differenceSimilarity + verticalSimilarity) / 3.0

        if (bestHashSimilarity >= STRONG_HASH_THRESHOLD) {
            return true
        }

        if (first.width == second.width &&
            first.height == second.height &&
            first.fileSizeBytes == second.fileSizeBytes &&
            averageHashSimilarity >= EXACT_DIMENSION_THRESHOLD
        ) {
            return true
        }

        if (averageHashSimilarity < MIN_HASH_AVERAGE) {
            return false
        }

        val pixelSimilarity = pixelSimilarity(first.pixels, second.pixels)
        val combined = averageHashSimilarity * HASH_WEIGHT + pixelSimilarity * PIXEL_WEIGHT
        return combined >= SIMILARITY_THRESHOLD
    }

    private fun buildGroup(
        images: List<MediaItem>,
        fingerprints: List<ImageFingerprint>,
        indices: List<Int>
    ): DuplicateGroup {
        var minSimilarity = 1.0
        for (i in indices.indices) {
            for (j in i + 1 until indices.size) {
                minSimilarity = min(
                    minSimilarity,
                    similarityScore(fingerprints[indices[i]], fingerprints[indices[j]])
                )
            }
        }
        val items = indices
            .map { fingerprints[it].sourceIndex }
            .distinct()
            .map { images[it] }
            .sortedByDescending { it.dateAdded }
        return DuplicateGroup(
            items = items,
            similarityPercent = (minSimilarity * 100).toInt().coerceIn(50, 100)
        )
    }

    private fun similarityScore(first: ImageFingerprint, second: ImageFingerprint): Double {
        if (first.contentHash != null && first.contentHash == second.contentHash) return 1.0
        val average = hashSimilarity(first.averageHash, second.averageHash)
        val difference = hashSimilarity(first.differenceHash, second.differenceHash)
        val vertical = hashSimilarity(first.verticalHash, second.verticalHash)
        val hashScore = (average + difference + vertical) / 3.0
        val pixelScore = pixelSimilarity(first.pixels, second.pixels)
        return hashScore * HASH_WEIGHT + pixelScore * PIXEL_WEIGHT
    }

    private fun computeFingerprint(
        context: Context,
        item: MediaItem,
        sourceIndex: Int
    ): ImageFingerprint? {
        val bitmap = BitmapDecoder.decodeSquareThumbnail(context, item.uri, SAMPLE_SIZE) ?: return null
        val dimensions = if (item.width > 0 && item.height > 0) {
            item.width to item.height
        } else {
            BitmapDecoder.readDimensions(context, item.uri) ?: (bitmap.width to bitmap.height)
        }
        return try {
            val gray = toGrayscaleMatrix(bitmap, SAMPLE_SIZE)
            ImageFingerprint(
                sourceIndex = sourceIndex,
                width = dimensions.first,
                height = dimensions.second,
                fileSizeBytes = item.size,
                contentHash = computeContentHash(context, item.uri, item.size),
                averageHash = computeAverageHash(gray, SAMPLE_SIZE),
                differenceHash = computeDifferenceHash(bitmap),
                verticalHash = computeVerticalDifferenceHash(bitmap),
                pixels = downsamplePixels(gray, SAMPLE_SIZE, PIXEL_GRID_SIZE)
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun computeContentHash(context: Context, uri: Uri, fileSizeBytes: Int): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val digest = MessageDigest.getInstance("MD5")
                digest.update(fileSizeBytes.toString().toByteArray())

                if (fileSizeBytes in 1..MAX_FULL_HASH_BYTES) {
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                } else if (fileSizeBytes > MAX_FULL_HASH_BYTES) {
                    val head = ByteArray(PARTIAL_HASH_BYTES)
                    val headRead = input.read(head)
                    if (headRead > 0) {
                        digest.update(head, 0, headRead)
                    }

                    val skipTarget = (fileSizeBytes - PARTIAL_HASH_BYTES * 2).coerceAtLeast(0).toLong()
                    input.skip(skipTarget)

                    val tail = ByteArray(PARTIAL_HASH_BYTES)
                    val tailRead = input.read(tail)
                    if (tailRead > 0) {
                        digest.update(tail, 0, tailRead)
                    }
                }

                digest.digest().toHexString()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun toGrayscaleMatrix(bitmap: Bitmap, size: Int): DoubleArray {
        val width = min(bitmap.width, size)
        val height = min(bitmap.height, size)
        val gray = DoubleArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val pixel = if (x < width && y < height) {
                    bitmap.getPixel(x, y)
                } else {
                    android.graphics.Color.BLACK
                }
                gray[y * size + x] = (
                    Color.red(pixel) * 0.299 +
                        Color.green(pixel) * 0.587 +
                        Color.blue(pixel) * 0.114
                    )
            }
        }
        return gray
    }

    private fun downsamplePixels(gray: DoubleArray, sourceSize: Int, targetSize: Int): ByteArray {
        val pixels = ByteArray(targetSize * targetSize)
        val blockSize = sourceSize / targetSize
        for (y in 0 until targetSize) {
            for (x in 0 until targetSize) {
                var sum = 0.0
                var count = 0
                val startY = y * blockSize
                val startX = x * blockSize
                for (offsetY in 0 until blockSize) {
                    for (offsetX in 0 until blockSize) {
                        sum += gray[(startY + offsetY) * sourceSize + (startX + offsetX)]
                        count++
                    }
                }
                pixels[y * targetSize + x] = (sum / count).toInt().coerceIn(0, 255).toByte()
            }
        }
        return pixels
    }

    private fun computeAverageHash(gray: DoubleArray, size: Int): Long {
        val hashSize = HASH_SIZE
        val blockSize = size / hashSize
        val values = DoubleArray(HASH_BITS)
        var index = 0
        for (y in 0 until hashSize) {
            for (x in 0 until hashSize) {
                var sum = 0.0
                var count = 0
                val startY = y * blockSize
                val startX = x * blockSize
                for (offsetY in 0 until blockSize) {
                    for (offsetX in 0 until blockSize) {
                        sum += gray[(startY + offsetY) * size + (startX + offsetX)]
                        count++
                    }
                }
                values[index++] = sum / count
            }
        }
        val average = values.average()
        return values.foldIndexed(0L) { bitIndex, hash, value ->
            if (value >= average) hash or (1L shl bitIndex) else hash
        }
    }

    private fun computeDifferenceHash(bitmap: Bitmap): Long {
        val hashSize = HASH_SIZE
        val scaled = Bitmap.createScaledBitmap(bitmap, hashSize + 1, hashSize, true)
        try {
            if (scaled.width < hashSize + 1 || scaled.height < hashSize) return 0L
            return computeHorizontalDifferenceHash(scaled, hashSize)
        } finally {
            if (scaled !== bitmap) {
                scaled.recycle()
            }
        }
    }

    private fun computeVerticalDifferenceHash(bitmap: Bitmap): Long {
        val hashSize = HASH_SIZE
        val scaled = Bitmap.createScaledBitmap(bitmap, hashSize, hashSize + 1, true)
        try {
            if (scaled.width < hashSize || scaled.height < hashSize + 1) return 0L
            var hash = 0L
            var bitIndex = 0
            for (y in 0 until hashSize) {
                for (x in 0 until hashSize) {
                    val top = Color.red(scaled.getPixel(x, y))
                    val bottom = Color.red(scaled.getPixel(x, y + 1))
                    if (top > bottom) {
                        hash = hash or (1L shl bitIndex)
                    }
                    bitIndex++
                }
            }
            return hash
        } finally {
            if (scaled !== bitmap) {
                scaled.recycle()
            }
        }
    }

    private fun computeHorizontalDifferenceHash(bitmap: Bitmap, hashSize: Int): Long {
        if (bitmap.width < hashSize + 1 || bitmap.height < hashSize) return 0L
        var hash = 0L
        var bitIndex = 0
        for (y in 0 until hashSize) {
            for (x in 0 until hashSize) {
                val left = Color.red(bitmap.getPixel(x, y))
                val right = Color.red(bitmap.getPixel(x + 1, y))
                if (left > right) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex++
            }
        }
        return hash
    }

    private fun hashSimilarity(first: Long, second: Long): Double {
        return 1.0 - hammingDistance(first, second).toDouble() / HASH_BITS
    }

    private fun hammingDistance(first: Long, second: Long): Int {
        return java.lang.Long.bitCount(first xor second)
    }

    private fun pixelSimilarity(first: ByteArray, second: ByteArray): Double {
        if (first.size != second.size) return 0.0
        var sumDiff = 0L
        for (index in first.indices) {
            val diff = (first[index].toInt() and 0xFF) - (second[index].toInt() and 0xFF)
            sumDiff += diff * diff
        }
        val meanSquareError = sumDiff.toDouble() / first.size
        return (1.0 - (meanSquareError / (255.0 * 255.0))).coerceIn(0.0, 1.0)
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { byte -> "%02x".format(byte) }
    }

    private data class ImageFingerprint(
        val sourceIndex: Int,
        val width: Int,
        val height: Int,
        val fileSizeBytes: Int,
        val contentHash: String?,
        val averageHash: Long,
        val differenceHash: Long,
        val verticalHash: Long,
        val pixels: ByteArray
    )

    private class UnionFind(size: Int) {
        private val parent = IntArray(size) { it }
        private val rank = IntArray(size)

        fun find(index: Int): Int {
            if (parent[index] != index) {
                parent[index] = find(parent[index])
            }
            return parent[index]
        }

        fun union(first: Int, second: Int) {
            val rootFirst = find(first)
            val rootSecond = find(second)
            if (rootFirst == rootSecond) return

            if (rank[rootFirst] < rank[rootSecond]) {
                parent[rootFirst] = rootSecond
            } else if (rank[rootFirst] > rank[rootSecond]) {
                parent[rootSecond] = rootFirst
            } else {
                parent[rootSecond] = rootFirst
                rank[rootFirst]++
            }
        }
    }

    companion object {
        private const val HASH_SIZE = 8
        private const val HASH_BITS = HASH_SIZE * HASH_SIZE
        private const val SAMPLE_SIZE = 64
        private const val PIXEL_GRID_SIZE = 16
        private const val SIMILARITY_THRESHOLD = 0.80
        private const val STRONG_HASH_THRESHOLD = 0.84
        private const val EXACT_DIMENSION_THRESHOLD = 0.75
        private const val MIN_HASH_AVERAGE = 0.70
        private const val HASH_WEIGHT = 0.70
        private const val PIXEL_WEIGHT = 0.30
        private const val MAX_FULL_HASH_BYTES = 8 * 1024 * 1024
        private const val PARTIAL_HASH_BYTES = 64 * 1024
    }
}

data class DuplicateScanResult(
    val groups: List<DuplicateGroup>,
    val imageCount: Int,
    val fingerprintCount: Int,
    val pairCount: Int
)
