package com.sam.ui.duplicate

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.SharedElementCallback
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sam.R
import com.sam.data.DuplicateGroup
import com.sam.databinding.FragmentDuplicatePhotoBinding
import com.sam.ui.detail.MediaDetailNav
import com.sam.ui.detail.SharedElementHelper
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class DuplicatePhotoFragment : Fragment() {

    private var _binding: FragmentDuplicatePhotoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DuplicatePhotoViewModel by viewModel()
    private lateinit var groupAdapter: DuplicateGroupAdapter
    private var currentGroups: List<DuplicateGroup> = emptyList()

    private val sharedElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(
            names: MutableList<String>,
            sharedElements: MutableMap<String, View>
        ) {
            var mappedAll = true
            names.forEach { uri ->
                val mapped = SharedElementHelper.mapDuplicateSharedElement(
                    recyclerView = binding.rvDuplicateGroups,
                    groups = currentGroups,
                    uri = uri,
                    sharedElements = sharedElements
                )
                if (!mapped) mappedAll = false
            }
            if (!mappedAll && names.isNotEmpty()) {
                postponeEnterTransition()
                binding.rvDuplicateGroups.doOnPreDraw {
                    names.forEach { uri ->
                        SharedElementHelper.mapDuplicateSharedElement(
                            recyclerView = binding.rvDuplicateGroups,
                            groups = currentGroups,
                            uri = uri,
                            sharedElements = sharedElements
                        )
                    }
                    startPostponedEnterTransition()
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            viewModel.startScan(requireContext())
        } else {
            binding.tvStatus.text = getString(R.string.duplicate_photos_permission_denied)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SharedElementHelper.applyListFragmentTransitions(this)
        requestMediaPermissions()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDuplicatePhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEnterSharedElementCallback(sharedElementCallback)
        setExitSharedElementCallback(sharedElementCallback)

        groupAdapter = DuplicateGroupAdapter { items, index, sharedView ->
            MediaDetailNav.open(
                fragment = this,
                actionId = R.id.action_fragmentDuplicatePhotos_to_fragmentMediaDetail,
                items = items,
                index = index,
                sharedView = sharedView
            )
        }
        binding.rvDuplicateGroups.adapter = groupAdapter
        binding.btnRescan.setOnClickListener {
            viewModel.startScan(requireContext())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: DuplicatePhotoUiState) {
        when (state) {
            DuplicatePhotoUiState.Idle -> {
                binding.progressBar.isVisible = false
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = getString(R.string.duplicate_photos_idle)
                binding.rvDuplicateGroups.isVisible = false
                binding.tvEmpty.isVisible = false
                binding.btnRescan.isVisible = false
            }
            is DuplicatePhotoUiState.Scanning -> {
                binding.progressBar.isVisible = true
                binding.tvStatus.isVisible = true
                binding.rvDuplicateGroups.isVisible = false
                binding.tvEmpty.isVisible = false
                binding.btnRescan.isVisible = false
                if (state.total == 0) {
                    binding.progressBar.isIndeterminate = true
                    binding.tvStatus.text = getString(R.string.duplicate_photos_loading)
                } else {
                    binding.progressBar.isIndeterminate = false
                    binding.progressBar.max = state.total
                    binding.progressBar.progress = state.processed
                    binding.tvStatus.text = getString(
                        R.string.duplicate_photos_analyzing,
                        state.processed,
                        state.total
                    )
                }
            }
            is DuplicatePhotoUiState.Complete -> {
                currentGroups = state.groups
                binding.progressBar.isVisible = false
                binding.tvStatus.isVisible = true
                groupAdapter.submitList(state.groups)
                binding.rvDuplicateGroups.isVisible = state.groups.isNotEmpty()
                binding.tvEmpty.isVisible = state.groups.isEmpty()
                binding.tvStatus.text = when {
                    state.imageCount == 0 -> getString(R.string.duplicate_photos_no_images)
                    state.fingerprintCount < 2 -> getString(
                        R.string.duplicate_photos_unreadable,
                        state.fingerprintCount,
                        state.imageCount
                    )
                    state.groups.isEmpty() -> getString(
                        R.string.duplicate_photos_scan_summary_none,
                        state.imageCount,
                        state.fingerprintCount
                    )
                    else -> getString(
                        R.string.duplicate_photos_scan_summary,
                        state.imageCount,
                        state.fingerprintCount,
                        state.groups.size
                    )
                }
                binding.btnRescan.isVisible = true
            }
            is DuplicatePhotoUiState.Error -> {
                binding.progressBar.isVisible = false
                binding.tvStatus.isVisible = true
                binding.rvDuplicateGroups.isVisible = false
                binding.tvEmpty.isVisible = false
                binding.btnRescan.isVisible = true
                binding.tvStatus.text = state.message
            }
        }
    }

    private fun requestMediaPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    override fun onDestroyView() {
        setEnterSharedElementCallback(null)
        setExitSharedElementCallback(null)
        super.onDestroyView()
        _binding = null
    }
}
