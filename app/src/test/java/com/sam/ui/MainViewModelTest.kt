package com.sam.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.sam.R
import com.sam.core.navigation.Navigator
import com.sam.data.MediaItem
import com.sam.data.MediaRepository
import com.sam.ui.main.MainViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var navigator: Navigator
    private lateinit var mediaRepository: MediaRepository
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        navigator = mockk(relaxed = true)
        mediaRepository = mockk(relaxed = true)
        viewModel = MainViewModel(navigator, mediaRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadMedia_fetchesFromRepositoryAndUpdatesState() = runTest {
        val mediaItems = listOf(
            MediaItem(1, mockk(), "Image1", 1024, 1000L, false),
            MediaItem(2, mockk(), "Video1", 2048, 2000L, true)
        )
        coEvery { mediaRepository.getMediaItems() } returns mediaItems

        viewModel.loadMedia()
        advanceUntilIdle() // Wait for coroutine to finish

        coVerify { mediaRepository.getMediaItems() }
        assertEquals(mediaItems, viewModel.mediaList.value)
    }

    @Test
    fun navigateToSecondFragment_callsNavigatorWithArgs() {
        val uri = "content://media/1"
        
        viewModel.navigateToSecondFragment(uri)

        verify { 
            navigator.navigate(
                actionId = R.id.action_fragmentMain_to_fragmentSecond, 
                args = match { it?.getString("media_uri") == uri }
            ) 
        }
    }

    @Test
    fun navigateToSecondActivity_callsNavigator() {
        viewModel.navigateToSecondActivity()

        verify { navigator.navigate(R.id.action_fragmentMain_to_activitySecond) }
    }
}