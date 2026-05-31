package com.sam.ui.duplicate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sam.data.DuplicateGroup
import com.sam.data.MediaItem
import com.sam.databinding.ItemDuplicateGroupBinding

class DuplicateGroupAdapter(
    private val onPhotoClick: (MediaItem, android.view.View) -> Unit
) : ListAdapter<DuplicateGroup, DuplicateGroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemDuplicateGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: ItemDuplicateGroupBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val photoAdapter = DuplicatePhotoThumbAdapter(onPhotoClick)

        init {
            binding.rvGroupPhotos.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = photoAdapter
            }
        }

        fun bind(group: DuplicateGroup) {
            binding.tvGroupTitle.text = binding.root.context.getString(
                com.sam.R.string.duplicate_photos_group_title,
                group.items.size,
                group.similarityPercent
            )
            photoAdapter.submitList(group.items)
        }
    }
}

class GroupDiffCallback : DiffUtil.ItemCallback<DuplicateGroup>() {
    override fun areItemsTheSame(oldItem: DuplicateGroup, newItem: DuplicateGroup): Boolean {
        return oldItem.items.firstOrNull()?.id == newItem.items.firstOrNull()?.id
    }

    override fun areContentsTheSame(oldItem: DuplicateGroup, newItem: DuplicateGroup): Boolean {
        return oldItem == newItem
    }
}
