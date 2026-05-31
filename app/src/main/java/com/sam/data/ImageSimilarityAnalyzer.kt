package com.sam.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class ImageSimilarityAnalyzer {

    suspend fun findSimilarGroups(
        context: Context,
        images: List<MediaItem>,
        onProgress: (processed: Int, total: Int) -> Unit
    ): List<DuplicateGroup> = withContext(Dispatchers.Default) {
        if (images.size < 2) return@withContext emptyList()

        val hashes = LongArray(images.size) { -1L }
        images.forEachIndexed { index, item ->
            hashes[index] = computeHash(context, item.uri) ?: -1L
            onProgress(index + 1, images.size)
        }

        val validIndices = hashes.indices.filter { hashes[it] != -1L }
        if (validIndices.size < 2) return@withContext emptyList()

        val unionFind = UnionFind(validIndices.size)
        val indexMap = validIndices.withIndex().associate { (ufIndex, imageIndex) -> imageIndex to ufIndex }
        val compared = HashSet<Long>()

        val buckets = mutableMapOf<Int, MutableList<Int>>()
        validIndices.forEach { imageIndex ->
            val hash = hashes[imageIndex]
            for (segment in 0 until SEGMENT_COUNT) {
                val bucketKey = bucketKey(hash, segment)
                buckets.getOrPut(bucketKey) { mutableListOf() }.add(imageIndex)
            }
        }

        buckets.values.forEach { bucket ->
            if (bucket.size < 2) return@forEach
            for (i in bucket.indices) {
                for (j in i + 1 until bucket.size) {
                    val left = min(bucket[i], bucket[j])
                    val right = max(bucket[i], bucket[j])
                    val pairKey = (left.toLong() shl 32) or right.toLong()
                    if (!compared.add(pairKey)) continue

                    val leftUf = indexMap.getValue(left)
                    val rightUf = indexMap.getValue(right)
                    if (similarity(hashes[left], hashes[right]) >= SIMILARITY_THRESHOLD) {
                        unionFind.union(leftUf, rightUf)
                    }
                }
            }
        }

        val grouped = mutableMapOf<Int, MutableList<Int>>()
        validIndices.forEach { imageIndex ->
            val ufIndex = indexMap.getValue(imageIndex)
            val root = unionFind.find(ufIndex)
            grouped.getOrPut(root) { mutableListOf() }.add(imageIndex)
        }

        grouped.values
            .filter { it.size >= 2 }
            .map { indices -> buildGroup(images, hashes, indices) }
            .sortedByDescending { it.items.size }
    }

    private fun buildGroup(
        images: List<MediaItem>,
        hashes: LongArray,
        indices: List<Int>
    ): DuplicateGroup {
        var minSimilarity = 1.0
        for (i in indices.indices) {
            for (j in i + 1 until indices.size) {
                val value = similarity(hashes[indices[i]], hashes[indices[j]])
                minSimilarity = min(minSimilarity, value)
            }
        }
        val items = indices.map { images[it] }.sortedByDescending { it.dateAdded }
        return DuplicateGroup(
            items = items,
            similarityPercent = (minSimilarity * 100).toInt().coerceIn(80, 100)
        )
    }

    private fun computeHash(context: Context, uri: Uri): Long? {
        val bitmap = decodeSampledBitmap(context, uri) ?: return null
        return try {
            computeDifferenceHash(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null

        val sampleSize = calculateInSampleSize(bounds, HASH_SIZE * 4)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
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

    private fun computeDifferenceHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, HASH_SIZE + 1, HASH_SIZE, true)
        try {
            var hash = 0L
            var bitIndex = 0
            for (y in 0 until HASH_SIZE) {
                for (x in 0 until HASH_SIZE) {
                    val left = Color.red(scaled.getPixel(x, y))
                    val right = Color.red(scaled.getPixel(x + 1, y))
                    if (left > right) {
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

    private fun hammingDistance(first: Long, second: Long): Int {
        return java.lang.Long.bitCount(first xor second)
    }

    private fun similarity(first: Long, second: Long): Double {
        return 1.0 - hammingDistance(first, second).toDouble() / HASH_BITS
    }

    private fun bucketKey(hash: Long, segment: Int): Int {
        val segmentValue = ((hash ushr (segment * 16)) and 0xFFFF).toInt()
        return segment * 0x10000 + segmentValue
    }

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
        private const val SIMILARITY_THRESHOLD = 0.80
        private const val SEGMENT_COUNT = 4
    }
}
