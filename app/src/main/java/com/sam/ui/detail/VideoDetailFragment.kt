package com.sam.ui.detail

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialContainerTransform
import com.sam.core.navigation.NavArgs
import com.sam.core.navigation.Navigator
import com.sam.core.navigation.observeNavigation
import com.sam.databinding.FragmentVideoDetailBinding
import org.koin.android.ext.android.inject

class VideoDetailFragment : Fragment() {
    private var binding: FragmentVideoDetailBinding? = null
    private val navigator: Navigator by inject()
    private var mediaController: MediaController? = null

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
        binding = FragmentVideoDetailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.videoView?.transitionName = arguments?.getString(NavArgs.MEDIA_URI)

        observeNavigation(navigator)

        binding?.toolbar?.setNavigationOnClickListener {
            navigator.navigateUp()
        }

        arguments?.getString(NavArgs.MEDIA_URI)?.let { uriString ->
            setupVideoPlayer(uriString.toUri())
        }
    }

    private fun setupVideoPlayer(uri: Uri) {
        binding?.videoView?.let { videoView ->
            mediaController = MediaController(requireContext())
            mediaController?.setAnchorView(videoView)
            videoView.setMediaController(mediaController)
            videoView.setVideoURI(uri)
            videoView.requestFocus()
            videoView.setOnPreparedListener {
                videoView.start()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding?.videoView?.stopPlayback()
        mediaController = null
        binding = null
    }
}