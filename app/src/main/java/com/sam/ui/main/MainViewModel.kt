package com.sam.ui.main

import com.sam.ui.base.BaseViewModel

import com.sam.core.navigation.Navigator
import com.sam.data.MediaRepository
import com.sam.data.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.sam.R

class MainViewModel(
    private val navigator: Navigator,
    private val mediaRepository: MediaRepository
) : BaseViewModel() {

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    fun loadMedia() {
        viewModelScope.launch {
            _mediaList.value = mediaRepository.getMediaItems()
        }
    }

    fun navigateToSecondActivity() {
        navigator.navigate(R.id.action_fragmentMain_to_activitySecond)
    }
}
