package com.sam.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Int,
    val dateAdded: Long,
    val isVideo: Boolean,
    val width: Int = 0,
    val height: Int = 0,
    val durationMillis: Long = 0L
)
