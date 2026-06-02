package com.sam.ui.duplicate

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sam.data.DuplicateGroup
import com.sam.data.ImageSimilarityAnalyzer
import com.sam.data.MediaRepository
import com.sam.ui.base.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DuplicatePhotoViewModel(
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
                    _uiState.value = DuplicatePhotoUiState.Complete(
                        groups = emptyList(),
                        imageCount = 0,
                        fingerprintCount = 0
                    )
                    return@launch
                }

                val result = imageSimilarityAnalyzer.findSimilarGroups(
                    context = context.applicationContext,
                    images = images
                ) { processed, total ->
                    _uiState.value = DuplicatePhotoUiState.Scanning(processed, total)
                }
                _uiState.value = DuplicatePhotoUiState.Complete(
                    groups = result.groups,
                    imageCount = result.imageCount,
                    fingerprintCount = result.fingerprintCount
                )
            } catch (exception: Exception) {
                _uiState.value = DuplicatePhotoUiState.Error(
                    exception.message ?: "Failed to scan photos"
                )
            }
        }
    }
}

sealed interface DuplicatePhotoUiState {
    data object Idle : DuplicatePhotoUiState
    data class Scanning(val processed: Int, val total: Int) : DuplicatePhotoUiState
    data class Complete(
        val groups: List<DuplicateGroup>,
        val imageCount: Int,
        val fingerprintCount: Int
    ) : DuplicatePhotoUiState
    data class Error(val message: String) : DuplicatePhotoUiState
}
