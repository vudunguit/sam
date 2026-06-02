package com.sam.ui.detail

import android.os.Bundle
import android.transition.Transition
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.activity.addCallback
import androidx.core.app.SharedElementCallback
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.sam.core.navigation.NavArgs
import com.sam.databinding.FragmentMediaDetailBinding

class MediaDetailFragment : Fragment() {

    private var binding: FragmentMediaDetailBinding? = null
    private var mediaController: MediaController? = null
    private var pagerAdapter: MediaDetailPagerAdapter? = null
    private var mediaUris: Array<String> = emptyArray()
    private var isVideoFlags: BooleanArray = booleanArrayOf()
    private var currentPosition = 0
    private var transitionName: String? = null
    private var isPagerVisible = false
    private var isReturning = false

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            currentPosition = position
            updateToolbar(position)
            playVideoAt(position)
        }
    }

    private val sharedElementCallback = object : SharedElementCallback() {
        override fun onMapSharedElements(
            names: MutableList<String>,
            sharedElements: MutableMap<String, View>
        ) {
            val sharedView = binding?.ivSharedElement ?: return
            val uri = mediaUris.getOrNull(currentPosition) ?: return
            names.clear()
            names.add(uri)
            sharedElements.clear()
            ViewCompat.setTransitionName(sharedView, uri)
            sharedElements[uri] = sharedView
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SharedElementHelper.applyDetailFragmentTransitions(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMediaDetailBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEnterSharedElementCallback(sharedElementCallback)
        setExitSharedElementCallback(sharedElementCallback)

        mediaUris = arguments?.getStringArray(NavArgs.MEDIA_URIS) ?: emptyArray()
        isVideoFlags = arguments?.getBooleanArray(NavArgs.MEDIA_IS_VIDEO)
            ?: BooleanArray(mediaUris.size)
        currentPosition = arguments?.getInt(NavArgs.MEDIA_INDEX, 0) ?: 0
        transitionName = arguments?.getString(NavArgs.MEDIA_URI)

        if (mediaUris.isEmpty()) {
            arguments?.getString(NavArgs.MEDIA_URI)?.let { singleUri ->
                mediaUris = arrayOf(singleUri)
                isVideoFlags = booleanArrayOf(false)
                transitionName = singleUri
            }
        }

        if (mediaUris.isEmpty()) {
            MediaDetailNav.navigateUp(this)
            return
        }

        currentPosition = currentPosition.coerceIn(0, mediaUris.lastIndex)
        transitionName = transitionName ?: mediaUris[currentPosition]

        binding?.toolbar?.setNavigationOnClickListener { navigateBack() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { navigateBack() }

        mediaController = MediaController(requireContext())

        pagerAdapter = MediaDetailPagerAdapter(mediaUris, isVideoFlags)
        binding?.viewPager?.apply {
            adapter = pagerAdapter
            offscreenPageLimit = 1
            setCurrentItem(currentPosition, false)
            registerOnPageChangeCallback(pageChangeCallback)
        }

        updateToolbar(currentPosition)
        setupSharedElementEnter()
    }

    private fun setupSharedElementEnter() {
        val binding = binding ?: return
        val uri = mediaUris[currentPosition]

        binding.viewPager.isVisible = false
        binding.ivSharedElement.isVisible = true
        isPagerVisible = false
        ViewCompat.setTransitionName(binding.ivSharedElement, transitionName)

        postponeEnterTransition()

        binding.ivSharedElement.load(uri.toUri()) {
            crossfade(false)
            listener(
                onSuccess = { _, _ ->
                    binding.ivSharedElement.doOnPreDraw {
                        startPostponedEnterTransition()
                    }
                    schedulePagerRevealFallback()
                },
                onError = { _, _ ->
                    binding.ivSharedElement.doOnPreDraw {
                        startPostponedEnterTransition()
                    }
                    schedulePagerRevealFallback()
                }
            )
        }

        (sharedElementEnterTransition as? Transition)?.addListener(object : Transition.TransitionListener {
            override fun onTransitionStart(transition: Transition?) = Unit
            override fun onTransitionCancel(transition: Transition?) = showViewPager()
            override fun onTransitionPause(transition: Transition?) = Unit
            override fun onTransitionResume(transition: Transition?) = Unit
            override fun onTransitionEnd(transition: Transition?) = showViewPager()
        })
    }

    private fun schedulePagerRevealFallback() {
        binding?.root?.postDelayed({
            if (!isPagerVisible) showViewPager()
        }, 350)
    }

    private fun showViewPager() {
        if (isPagerVisible) return
        isPagerVisible = true
        binding?.ivSharedElement?.isVisible = false
        binding?.viewPager?.isVisible = true
        playVideoAt(currentPosition)
    }

    private fun navigateBack() {
        if (isReturning) return
        isReturning = true

        if (!isPagerVisible) {
            MediaDetailNav.navigateUp(this)
            return
        }

        val binding = binding ?: return
        val uri = mediaUris[currentPosition]

        ViewCompat.setTransitionName(binding.ivSharedElement, uri)

        val copiedCurrentPage = findPageViewHolder(currentPosition)
            ?.copyImageTo(binding.ivSharedElement) == true
        if (copiedCurrentPage) {
            finishReturnTransition()
            return
        }

        binding.ivSharedElement.load(uri.toUri()) {
            crossfade(false)
            listener(
                onSuccess = { _, _ ->
                    finishReturnTransition()
                },
                onError = { _, _ ->
                    finishReturnTransition()
                }
            )
        }
    }

    private fun finishReturnTransition() {
        val binding = binding ?: return
        stopAllVideos()
        binding.appBarLayout.isVisible = false
        binding.ivSharedElement.isVisible = true
        binding.viewPager.isVisible = false
        isPagerVisible = false
        binding.ivSharedElement.doOnPreDraw {
            MediaDetailNav.navigateUp(this@MediaDetailFragment)
        }
    }

    private fun updateToolbar(position: Int) {
        binding?.toolbar?.subtitle = if (mediaUris.size > 1) {
            getString(com.sam.R.string.media_detail_position, position + 1, mediaUris.size)
        } else {
            null
        }
    }

    private fun playVideoAt(position: Int) {
        if (!isPagerVisible) return
        stopAllVideos()
        if (!isVideoFlags.getOrElse(position) { false }) return

        binding?.viewPager?.post {
            val holder = findPageViewHolder(position) ?: return@post
            val controller = mediaController ?: return@post
            holder.playVideo(mediaUris[position], controller)
        }
    }

    private fun stopAllVideos() {
        val recyclerView = binding?.viewPager?.getChildAt(0) as? RecyclerView ?: return
        for (index in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(index))
            (holder as? MediaDetailPagerAdapter.PageViewHolder)?.stopVideo()
        }
    }

    private fun findPageViewHolder(position: Int): MediaDetailPagerAdapter.PageViewHolder? {
        val recyclerView = binding?.viewPager?.getChildAt(0) as? RecyclerView ?: return null
        return recyclerView.findViewHolderForAdapterPosition(position)
            as? MediaDetailPagerAdapter.PageViewHolder
    }

    override fun onDestroyView() {
        binding?.viewPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        setEnterSharedElementCallback(null)
        setExitSharedElementCallback(null)
        stopAllVideos()
        mediaController = null
        pagerAdapter = null
        binding = null
        super.onDestroyView()
    }
}
