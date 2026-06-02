package com.sam.core.navigation

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

fun Fragment.observeNavigation(
    navigator: Navigator,
    navControllerProvider: () -> NavController = { findNavController() }
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navigator.navigationCommands.collect { command ->
                val navController = navControllerProvider()
                when (command) {
                    is NavigationCommand.ToAction -> {
                        navController.navigate(
                            command.actionId,
                            command.args,
                            null,
                            command.extras
                        )
                    }
                    is NavigationCommand.NavigateUp -> {
                        navController.navigateUp()
                    }
                    is NavigationCommand.PopUpTo -> {
                        navController.popBackStack(command.destinationId, command.inclusive)
                    }
                }
            }
        }
    }
}
