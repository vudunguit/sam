package com.sam.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    suspend fun getMediaItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val contentResolver: ContentResolver = context.contentResolver

        val volumes = listOf("internal", "external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        for (volume in volumes) {
            val uri = MediaStore.Files.getContentUri(volume)
            contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getInt(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val mediaType = cursor.getInt(mediaTypeColumn)
                    val isVideo = mediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

                    val baseUri = if (volume == "internal") {
                        if (isVideo) MediaStore.Video.Media.INTERNAL_CONTENT_URI else MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    } else {
                        if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val contentUri = ContentUris.withAppendedId(baseUri, id)
                    val durationMillis = if (isVideo) {
                        getVideoDuration(contentResolver, contentUri)
                    } else {
                        0L
                    }

                    mediaList += MediaItem(
                        id = id,
                        uri = contentUri,
                        name = name,
                        size = size,
                        dateAdded = dateAdded,
                        isVideo = isVideo,
                        durationMillis = durationMillis
                    )
                }
            }
        }
        return@withContext mediaList.sortedByDescending { it.dateAdded }
    }

    private fun getVideoDuration(contentResolver: ContentResolver, uri: android.net.Uri): Long {
        val projection = arrayOf(MediaStore.Video.Media.DURATION)
        return runCatching {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                if (cursor.moveToFirst() && durationColumn >= 0 && !cursor.isNull(durationColumn)) {
                    cursor.getLong(durationColumn)
                } else {
                    0L
                }
            } ?: 0L
        }.getOrDefault(0L)
    }

    suspend fun getImageItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver
        val collections = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.INTERNAL_CONTENT_URI
        )
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        for (collection in collections) {
            contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getInt(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    imageList += MediaItem(
                        id = id,
                        uri = contentUri,
                        name = name,
                        size = size,
                        dateAdded = dateAdded,
                        isVideo = false,
                        width = width,
                        height = height
                    )
                }
            }
        }
        return@withContext imageList
            .distinctBy { it.uri.toString() }
            .sortedByDescending { it.dateAdded }
    }
}
