package com.sam.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class MediaRepositoryTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockCursorInternal: Cursor
    private lateinit var mockCursorExternal: Cursor
    private lateinit var mediaRepository: MediaRepository

    @Before
    fun setup() {
        mockContext = mockk()
        mockContentResolver = mockk()
        mockCursorInternal = mockk(relaxed = true)
        mockCursorExternal = mockk(relaxed = true)

        every { mockContext.contentResolver } returns mockContentResolver

        mediaRepository = MediaRepository(mockContext)
    }

    @Test
    fun getMediaItems_returnsSortedMediaList() = runTest {
        // Setup internal volume query
        every {
            mockContentResolver.query(
                MediaStore.Files.getContentUri("internal"),
                any(), any(), any(), any()
            )
        } returns mockCursorInternal

        // Setup external volume query
        every {
            mockContentResolver.query(
                MediaStore.Files.getContentUri("external"),
                any(), any(), any(), any()
            )
        } returns mockCursorExternal

        // Mock cursor data for Internal
        setupMockCursor(mockCursorInternal, listOf(
            MockMediaData(1L, "InternalImage", 1024, 1000L, MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
        ))

        // Mock cursor data for External
        setupMockCursor(mockCursorExternal, listOf(
            MockMediaData(2L, "ExternalVideo", 2048, 2000L, MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
        ))

        val result = mediaRepository.getMediaItems()

        assertEquals(2, result.size)
        // Should be sorted by descending date added
        assertEquals("ExternalVideo", result[0].name)
        assertEquals("InternalImage", result[1].name)

        assertEquals(2L, result[0].id)
        assertEquals(true, result[0].isVideo)
        assertEquals(2048, result[0].size)

        assertEquals(1L, result[1].id)
        assertEquals(false, result[1].isVideo)
        assertEquals(1024, result[1].size)
    }

    private fun setupMockCursor(mockCursor: Cursor, data: List<MockMediaData>) {
        var currentIndex = -1
        
        every { mockCursor.moveToNext() } answers {
            currentIndex++
            currentIndex < data.size
        }

        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID) } returns 0
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME) } returns 1
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE) } returns 2
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED) } returns 3
        every { mockCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE) } returns 4

        every { mockCursor.getLong(0) } answers { data[currentIndex].id }
        every { mockCursor.getString(1) } answers { data[currentIndex].name }
        every { mockCursor.getInt(2) } answers { data[currentIndex].size }
        every { mockCursor.getLong(3) } answers { data[currentIndex].dateAdded }
        every { mockCursor.getInt(4) } answers { data[currentIndex].mediaType }
    }

    private data class MockMediaData(
        val id: Long,
        val name: String,
        val size: Int,
        val dateAdded: Long,
        val mediaType: Int
    )
}
