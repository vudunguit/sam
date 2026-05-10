package com.sam.core.navigation

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

fun Fragment.observeNavigation(navigator: Navigator) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.navigationCommands.collect { command ->
                when (command) {
                    is NavigationCommand.ToAction -> {
                        findNavController().navigate(command.actionId, command.args)
                    }
                    is NavigationCommand.NavigateUp -> {
                        findNavController().navigateUp()
                    }
                    is NavigationCommand.PopUpTo -> {
                        findNavController().popBackStack(command.destinationId, command.inclusive)
                    }
                }
            }
        }
    }
}
