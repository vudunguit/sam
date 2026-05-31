package com.sam.ui.duplicate

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigator as NavNavigator
import com.sam.R
import com.sam.core.navigation.NavArgs
import com.sam.core.navigation.Navigator
import com.sam.data.DuplicateGroup
import com.sam.data.ImageSimilarityAnalyzer
import com.sam.data.MediaItem
import com.sam.data.MediaRepository
import com.sam.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuplicatePhotoViewModel(
    private val navigator: Navigator,
    private val mediaRepository: MediaRepository,
    private val imageSimilarityAnalyzer: ImageSimilarityAnalyzer
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<DuplicatePhotoUiState>(DuplicatePhotoUiState.Idle)
    val uiState: StateFlow<DuplicatePhotoUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun startScan(context: Context) {
        if (scanJob?.isActive == true) return

        scanJob = viewModelScope.launch {
            _uiState.value = DuplicatePhotoUiState.Scanning(0, 0)
            try {
                val images = mediaRepository.getImageItems()
                if (images.isEmpty()) {
                    _uiState.value = DuplicatePhotoUiState.Complete(emptyList())
                    return@launch
                }

                val groups = imageSimilarityAnalyzer.findSimilarGroups(
                    context = context.applicationContext,
                    images = images
                ) { processed, total ->
                    _uiState.value = DuplicatePhotoUiState.Scanning(processed, total)
                }
                _uiState.value = DuplicatePhotoUiState.Complete(groups)
            } catch (exception: Exception) {
                _uiState.value = DuplicatePhotoUiState.Error(
                    exception.message ?: "Failed to scan photos"
                )
            }
        }
    }

    fun navigateToMediaDetail(mediaItem: MediaItem, extras: NavNavigator.Extras? = null) {
        val bundle = Bundle().apply {
            putString(NavArgs.MEDIA_URI, mediaItem.uri.toString())
        }
        navigator.navigate(R.id.action_fragmentMain_to_fragmentImageDetail, bundle, extras)
    }
}

sealed interface DuplicatePhotoUiState {
    data object Idle : DuplicatePhotoUiState
    data class Scanning(val processed: Int, val total: Int) : DuplicatePhotoUiState
    data class Complete(val groups: List<DuplicateGroup>) : DuplicatePhotoUiState
    data class Error(val message: String) : DuplicatePhotoUiState
}
