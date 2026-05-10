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

class MainFragment: Fragment() {
    private var binding: FragmentMainBinding? = null
    private val viewModel: MainViewModel by viewModel()
    private val navigator: Navigator by inject()
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
        
        observeNavigation(navigator)
        
        binding?.btnMove?.setOnClickListener {
            viewModel.navigateToSecondFragment()
        }
    }
}