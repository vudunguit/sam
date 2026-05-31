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
    private val onClick: (MediaItem, android.view.View) -> Unit
) : ListAdapter<MediaItem, DuplicatePhotoThumbAdapter.ThumbViewHolder>(ThumbDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbViewHolder {
        val binding = ItemDuplicateThumbBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThumbViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ThumbViewHolder(
        private val binding: ItemDuplicateThumbBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(mediaItem: MediaItem) {
            binding.ivThumb.transitionName = mediaItem.uri.toString()
            binding.ivThumb.load(mediaItem.uri) {
                crossfade(true)
            }
            binding.root.setOnClickListener {
                onClick(mediaItem, binding.ivThumb)
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
