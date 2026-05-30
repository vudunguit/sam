package com.sam.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sam.databinding.FragmentMainBinding

import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject
import com.sam.core.navigation.Navigator
import com.sam.core.navigation.observeNavigation

import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.core.view.doOnPreDraw
import com.sam.R
import com.sam.core.navigation.NavArgs

class MainFragment: Fragment() {
    private var binding: FragmentMainBinding? = null
    private val viewModel: MainViewModel by viewModel()
    private val navigator: Navigator by inject()
    private lateinit var adapter: MediaAdapter

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
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        
        observeNavigation(navigator)
        
        adapter = MediaAdapter { mediaItem, sharedView ->
            val extras = FragmentNavigatorExtras(sharedView to mediaItem.uri.toString())
            val bundle = Bundle().apply { putString(NavArgs.MEDIA_URI, mediaItem.uri.toString()) }
            if (mediaItem.isVideo) {
                findNavController().navigate(R.id.action_fragmentMain_to_fragmentVideoDetail, bundle, null, extras)
            } else {
                findNavController().navigate(R.id.action_fragmentMain_to_fragmentSecond, bundle, null, extras)
            }
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

    private fun requestMediaPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }
}