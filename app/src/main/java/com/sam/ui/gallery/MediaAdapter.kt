package com.sam.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sam.data.MediaItem
import com.sam.databinding.ItemMediaBinding

class MediaAdapter(private val onClick: (MediaItem, View) -> Unit) :
    ListAdapter<MediaItem, MediaAdapter.MediaViewHolder>(MediaDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(media: MediaItem) {
            binding.ivMedia.transitionName = media.uri.toString()
            binding.ivMedia.load(media.uri) {
                crossfade(true)
            }
            binding.ivVideoIcon.visibility = if (media.isVideo) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                onClick(media, binding.ivMedia)
            }
        }
    }
}

class MediaDiffCallback : DiffUtil.ItemCallback<MediaItem>() {
    override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
        return oldItem == newItem
    }
}
