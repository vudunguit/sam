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
        val sharedView: View get() = binding.ivMedia

        fun bind(media: MediaItem) {
            binding.ivMedia.transitionName = media.uri.toString()
            binding.ivMedia.load(media.uri) {
                crossfade(false)
            }
            binding.ivVideoIcon.visibility = if (media.isVideo) View.VISIBLE else View.GONE
            binding.tvDuration.visibility = if (media.isVideo) View.VISIBLE else View.GONE
            binding.tvDuration.text = formatDuration(media.durationMillis)
            binding.root.setOnClickListener {
                onClick(media, binding.ivMedia)
            }
        }

        private fun formatDuration(durationMillis: Long): String {
            val totalSeconds = (durationMillis / 1000).coerceAtLeast(0L)
            val seconds = totalSeconds % 60
            val minutes = (totalSeconds / 60) % 60
            val hours = totalSeconds / 3600

            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
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
