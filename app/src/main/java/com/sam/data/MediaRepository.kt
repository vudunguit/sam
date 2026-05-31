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

                    mediaList += MediaItem(id, contentUri, name, size, dateAdded, isVideo)
                }
            }
        }
        return@withContext mediaList.sortedByDescending { it.dateAdded }
    }

    suspend fun getImageItems(): List<MediaItem> = withContext(Dispatchers.IO) {
        val imageList = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver
        val volumes = listOf("internal", "external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        for (volume in volumes) {
            val uri = MediaStore.Files.getContentUri(volume)
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val size = cursor.getInt(sizeColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val baseUri = if (volume == "internal") {
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val contentUri = ContentUris.withAppendedId(baseUri, id)
                    imageList += MediaItem(id, contentUri, name, size, dateAdded, isVideo = false)
                }
            }
        }
        return@withContext imageList.sortedByDescending { it.dateAdded }
    }
}
