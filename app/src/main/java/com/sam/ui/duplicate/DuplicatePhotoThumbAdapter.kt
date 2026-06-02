package com.sam.ui.duplicate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sam.data.MediaItem
import com.sam.databinding.ItemDuplicateThumbBinding

class DuplicatePhotoThumbAdapter(
    private val onClick: (List<MediaItem>, Int, android.view.View) -> Unit
) : ListAdapter<MediaItem, DuplicatePhotoThumbAdapter.ThumbViewHolder>(ThumbDiffCallback()) {

    private var groupItems: List<MediaItem> = emptyList()

    fun submitGroup(items: List<MediaItem>) {
        groupItems = items
        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val binding = ItemDuplicateThumbBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThumbViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        holder.bind(getItem(position), groupItems)
    }

    inner class ThumbViewHolder(
        private val binding: ItemDuplicateThumbBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItem, items: List<MediaItem>) {
            binding.ivThumb.transitionName = mediaItem.uri.toString()
            binding.ivThumb.load(mediaItem.uri) {
                crossfade(false)
            }
            binding.root.setOnClickListener {
                val index = items.indexOfFirst { it.id == mediaItem.id }.coerceAtLeast(0)
                onClick(items, index, binding.ivThumb)
            }
        }
    }
}

class ThumbDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
    override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        return oldItem == newItem
    }
}
