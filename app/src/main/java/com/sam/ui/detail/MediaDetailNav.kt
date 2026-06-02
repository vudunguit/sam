package com.sam.ui.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.sam.core.navigation.NavArgs
import com.sam.data.MediaItem

object MediaDetailNav {

    fun createBundle(items: List<MediaItem>, index: Int): Bundle {
        require(items.isNotEmpty()) { "Media list must not be empty" }
        val safeIndex = index.coerceIn(0, items.lastIndex)
        return Bundle().apply {
            putStringArray(NavArgs.MEDIA_URIS, items.map { it.uri.toString() }.toTypedArray())
            putBooleanArray(NavArgs.MEDIA_IS_VIDEO, BooleanArray(items.size) { items[it].isVideo })
            putInt(NavArgs.MEDIA_INDEX, safeIndex)
            putString(NavArgs.MEDIA_URI, items[safeIndex].uri.toString())
        }
    }

    fun open(
        fragment: Fragment,
        actionId: Int,
        items: List<MediaItem>,
        index: Int,
        sharedView: View
    ) {
        if (items.isEmpty()) return
        val safeIndex = index.coerceIn(0, items.lastIndex)
        val transitionName = items[safeIndex].uri.toString()
        val extras = FragmentNavigatorExtras(sharedView to transitionName)
        fragment.findNavController().navigate(
            actionId,
            createBundle(items, safeIndex),
            null,
            extras
        )
    }

    fun navigateUp(fragment: Fragment) {
        fragment.findNavController().navigateUp()
    }
}
