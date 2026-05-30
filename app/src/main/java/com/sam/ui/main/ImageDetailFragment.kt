package com.sam.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sam.databinding.FragmentSecondBinding
import org.koin.android.ext.android.inject
import com.sam.core.navigation.Navigator
import com.sam.core.navigation.observeNavigation
import com.sam.core.navigation.NavArgs
import coil.load
import android.net.Uri
import androidx.core.net.toUri
import android.graphics.Color
import com.google.android.material.transition.MaterialContainerTransform

class ImageDetailFragment : Fragment() {
    private var binding: FragmentSecondBinding? = null
    private val navigator: Navigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 300
            scrimColor = Color.TRANSPARENT
        }
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            duration = 300
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding?.ivDetail?.transitionName = arguments?.getString(NavArgs.MEDIA_URI)
        
        observeNavigation(navigator)
        
        binding?.toolbar?.setNavigationOnClickListener {
            navigator.navigateUp()
        }

        arguments?.getString(NavArgs.MEDIA_URI)?.let { uriString ->
            binding?.ivDetail?.load(uriString.toUri()) {
                crossfade(true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
