package com.sam.ui.main

import com.sam.ui.base.BaseViewModel

import com.sam.core.navigation.Navigator
import com.sam.data.MediaRepository
import com.sam.data.MediaItem
import com.sam.core.navigation.NavArgs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigator as NavNavigator
import android.os.Bundle
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

    fun navigateToMediaDetail(mediaItem: MediaItem, extras: NavNavigator.Extras? = null) {
        val bundle = Bundle().apply {
            putString(NavArgs.MEDIA_URI, mediaItem.uri.toString())
        }
        val actionId = if (mediaItem.isVideo) {
            R.id.action_fragmentMain_to_fragmentVideoDetail
        } else {
            R.id.action_fragmentMain_to_fragmentImageDetail
        }
        navigator.navigate(actionId, bundle, extras)
    }

    fun navigateToSecondActivity() {
        navigator.navigate(R.id.action_fragmentMain_to_activitySecond)
    }
}
