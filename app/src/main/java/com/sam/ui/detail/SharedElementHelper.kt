package com.sam.ui.detail

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialContainerTransform
import com.sam.R
import com.sam.data.DuplicateGroup
import com.sam.data.MediaItem

object SharedElementHelper {

    fun createContainerTransform(): MaterialContainerTransform {
        return MaterialContainerTransform().apply {
            duration = 300
            scrimColor = Color.TRANSPARENT
            fitMode = MaterialContainerTransform.FIT_MODE_AUTO
            drawingViewId = R.id.innerNavHost
        }
    }

    fun applyListFragmentTransitions(fragment: Fragment) {
        fragment.exitTransition = Hold()
        fragment.reenterTransition = Hold()
    }

    fun applyDetailFragmentTransitions(fragment: Fragment) {
        fragment.sharedElementEnterTransition = createContainerTransform()
        fragment.sharedElementReturnTransition = createContainerTransform()
    }

    fun findViewWithTransitionName(root: View, name: String): View? {
        if (ViewCompat.getTransitionName(root) == name) return root
        if (root is ViewGroup) {
            for (index in 0 until root.childCount) {
                findViewWithTransitionName(root.getChildAt(index), name)?.let { return it }
            }
        }
        return null
    }

    fun mapGallerySharedElement(
        recyclerView: RecyclerView,
        items: List<MediaItem>,
        uri: String,
        sharedElements: MutableMap<String, View>,
        getSharedView: (RecyclerView.ViewHolder) -> View?
    ): Boolean {
        val position = items.indexOfFirst { it.uri.toString() == uri }
        if (position < 0) return false

        recyclerView.scrollToPosition(position)
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        if (holder != null) {
            getSharedView(holder)?.let { sharedElements[uri] = it }
            return sharedElements.containsKey(uri)
        }
        return false
    }

    fun mapDuplicateSharedElement(
        recyclerView: RecyclerView,
        groups: List<DuplicateGroup>,
        uri: String,
        sharedElements: MutableMap<String, View>
    ): Boolean {
        var itemIndex = -1
        val groupIndex = groups.indexOfFirst { group ->
            itemIndex = group.items.indexOfFirst { it.uri.toString() == uri }
            itemIndex >= 0
        }
        if (groupIndex < 0) return false

        recyclerView.scrollToPosition(groupIndex)
        val groupHolder = recyclerView.findViewHolderForAdapterPosition(groupIndex) ?: return false
        val groupPhotos = groupHolder.itemView.findViewById<RecyclerView>(R.id.rvGroupPhotos)
        groupPhotos?.scrollToPosition(itemIndex)

        val target = groupPhotos
            ?.findViewHolderForAdapterPosition(itemIndex)
            ?.itemView
            ?.let { findViewWithTransitionName(it, uri) }
            ?: findViewWithTransitionName(groupHolder.itemView, uri)
        if (target != null) {
            sharedElements[uri] = target
            return true
        }
        return false
    }
}
