package com.sam.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: Int,
    val dateAdded: Long,
    val isVideo: Boolean
)
