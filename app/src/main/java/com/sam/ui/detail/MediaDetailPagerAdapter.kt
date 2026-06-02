package com.sam.ui.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.sam.databinding.ItemMediaDetailPageBinding

class MediaDetailPagerAdapter(
    private val uris: Array<String>,
    private val isVideo: BooleanArray
) : RecyclerView.Adapter<MediaDetailPagerAdapter.PageViewHolder>() {

    override fun getItemCount(): Int = uris.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemMediaDetailPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(uris[position], isVideo[position])
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        holder.stopVideo()
        super.onViewRecycled(holder)
    }

    inner class PageViewHolder(
        private val binding: ItemMediaDetailPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uriString: String, isVideoItem: Boolean) {
            stopVideo()
            if (isVideoItem) {
                binding.ivMedia.isVisible = false
                binding.videoView.isVisible = true
            } else {
                binding.videoView.isVisible = false
                binding.ivMedia.isVisible = true
                binding.ivMedia.load(uriString.toUri()) {
                    crossfade(false)
                }
            }
        }

        fun copyImageTo(target: ImageView): Boolean {
            val drawable = binding.ivMedia.drawable ?: return false
            target.setImageDrawable(drawable)
            return true
        }

        fun playVideo(uriString: String, mediaController: android.widget.MediaController) {
            binding.ivMedia.isVisible = false
            binding.videoView.isVisible = true
            val videoView = binding.videoView
            videoView.setMediaController(mediaController)
            mediaController.setAnchorView(videoView)
            videoView.setVideoURI(uriString.toUri())
            videoView.requestFocus()
            videoView.setOnPreparedListener { player ->
                player.isLooping = false
                videoView.start()
            }
        }

        fun stopVideo() {
            binding.videoView.setOnPreparedListener(null)
            binding.videoView.stopPlayback()
        }
    }
}
