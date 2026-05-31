package com.sam.ui.duplicate

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.sam.R
import com.sam.core.navigation.Navigator
import com.sam.core.navigation.observeNavigation
import com.sam.databinding.FragmentDuplicatePhotoBinding
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class DuplicatePhotoFragment : Fragment() {

    private var _binding: FragmentDuplicatePhotoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DuplicatePhotoViewModel by viewModel()
    private val navigator: Navigator by inject()
    private lateinit var groupAdapter: DuplicateGroupAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.entries.all { it.value }) {
            viewModel.startScan(requireContext())
        } else {
            binding.tvStatus.text = getString(R.string.duplicate_photos_permission_denied)
        }
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

        observeNavigation(navigator) {
            parentFragment?.parentFragment?.findNavController() ?: findNavController()
        }

        groupAdapter = DuplicateGroupAdapter { mediaItem, sharedView ->
            val extras = FragmentNavigatorExtras(sharedView to mediaItem.uri.toString())
            viewModel.navigateToMediaDetail(mediaItem, extras)
        }
        binding.rvDuplicateGroups.adapter = groupAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }

        requestMediaPermissions()
    }

    private fun renderState(state: DuplicatePhotoUiState) {
        when (state) {
            DuplicatePhotoUiState.Idle -> {
                binding.progressBar.isVisible = false
                binding.tvStatus.isVisible = true
                binding.tvStatus.text = getString(R.string.duplicate_photos_idle)
                binding.rvDuplicateGroups.isVisible = false
                binding.tvEmpty.isVisible = false
            }
            is DuplicatePhotoUiState.Scanning -> {
                binding.progressBar.isVisible = true
                binding.tvStatus.isVisible = true
                binding.rvDuplicateGroups.isVisible = false
                binding.tvEmpty.isVisible = false
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
                binding.progressBar.isVisible = false
                binding.tvStatus.isVisible = true
                groupAdapter.submitList(state.groups)
                binding.rvDuplicateGroups.isVisible = state.groups.isNotEmpty()
                binding.tvEmpty.isVisible = state.groups.isEmpty()
                binding.tvStatus.text = if (state.groups.isEmpty()) {
                    getString(R.string.duplicate_photos_none_found)
                } else {
                    getString(R.string.duplicate_photos_found, state.groups.size)
                }
            }
            is DuplicatePhotoUiState.Error -> {
                binding.progressBar.isVisible = false
                binding.tvStatus.isVisible = true
                binding.rvDuplicateGroups.isVisible = false
                binding.tvEmpty.isVisible = false
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
        super.onDestroyView()
        _binding = null
    }
}
