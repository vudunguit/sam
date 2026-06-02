package com.sam.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.sam.R
import com.sam.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager.findFragmentById(R.id.innerNavHost) as NavHostFragment
        val navController = navHostFragment.navController
        binding?.bottomNavigation?.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding?.bottomNavigation?.isVisible = destination.id != R.id.fragmentMediaDetail
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
