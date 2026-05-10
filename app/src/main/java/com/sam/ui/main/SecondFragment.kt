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
import coil.load
import android.net.Uri
import androidx.core.net.toUri

class SecondFragment : Fragment() {
    private var binding: FragmentSecondBinding? = null
    private val navigator: Navigator by inject()

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
        
        observeNavigation(navigator)
        
        binding?.toolbar?.setNavigationOnClickListener {
            navigator.navigateUp()
        }

        arguments?.getString("media_uri")?.let { uriString ->
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
