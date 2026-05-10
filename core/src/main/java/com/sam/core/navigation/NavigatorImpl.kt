package com.sam.core.navigation

import android.os.Bundle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NavigatorImpl : Navigator {
    private val _navigationCommands = MutableSharedFlow<NavigationCommand>(extraBufferCapacity = 1)
    override val navigationCommands: SharedFlow<NavigationCommand> = _navigationCommands.asSharedFlow()

    override fun navigate(actionId: Int, args: Bundle?) {
        _navigationCommands.tryEmit(NavigationCommand.ToAction(actionId, args))
    }

    override fun navigateUp() {
        _navigationCommands.tryEmit(NavigationCommand.NavigateUp)
    }

    override fun popUpTo(destinationId: Int, inclusive: Boolean) {
        _navigationCommands.tryEmit(NavigationCommand.PopUpTo(destinationId, inclusive))
    }
}
