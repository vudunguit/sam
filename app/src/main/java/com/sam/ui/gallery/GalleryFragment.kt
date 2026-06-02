package com.sam.ui.gallery

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sam.R
import com.sam.databinding.FragmentGalleryBinding
import com.sam.ui.detail.MediaDetailNav
import com.sam.ui.detail.SharedElementHelper
import com.sam.ui.main.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class GalleryFragment : Fragment() {
    private var binding: FragmentGalleryBinding? = null
    private val viewModel: MainViewModel by viewModel()
    private lateinit var adapter: MediaAdapter

    private val sharedElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(
            names: MutableList<String>,
            sharedElements: MutableMap<String, View>
        ) {
            mapListSharedElements(names, sharedElements)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            viewModel.loadMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SharedElementHelper.applyListFragmentTransitions(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEnterSharedElementCallback(sharedElementCallback)
        setExitSharedElementCallback(sharedElementCallback)

        adapter = MediaAdapter { mediaItem, sharedView ->
            val list = adapter.currentList
            val index = list.indexOfFirst { it.id == mediaItem.id }.coerceAtLeast(0)
            MediaDetailNav.open(
                fragment = this,
                actionId = R.id.action_fragmentGallery_to_fragmentMediaDetail,
                items = list,
                index = index,
                sharedView = sharedView
            )
        }
        binding?.rvMedia?.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mediaList.collect { list ->
                    adapter.submitList(list)
                }
            }
        }

        requestMediaPermissions()
    }

    private fun mapListSharedElements(
        names: MutableList<String>,
        sharedElements: MutableMap<String, View>
    ) {
        val recyclerView = binding?.rvMedia ?: return
        var mappedAll = true
        names.forEach { uri ->
            val mapped = SharedElementHelper.mapGallerySharedElement(
                recyclerView = recyclerView,
                items = adapter.currentList,
                uri = uri,
                sharedElements = sharedElements
            ) { holder ->
                (holder as? MediaAdapter.MediaViewHolder)?.sharedView
            }
            if (!mapped) mappedAll = false
        }
        if (!mappedAll && names.isNotEmpty()) {
            postponeEnterTransition()
            recyclerView.doOnPreDraw {
                names.forEach { uri ->
                    SharedElementHelper.mapGallerySharedElement(
                        recyclerView = recyclerView,
                        items = adapter.currentList,
                        uri = uri,
                        sharedElements = sharedElements
                    ) { holder ->
                        (holder as? MediaAdapter.MediaViewHolder)?.sharedView
                    }
                }
                startPostponedEnterTransition()
            }
        }
    }

    override fun onDestroyView() {
        setEnterSharedElementCallback(null)
        setExitSharedElementCallback(null)
        binding = null
        super.onDestroyView()
    }

    private fun requestMediaPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }
}
