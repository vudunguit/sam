package com.sam.core.navigation

import android.os.Bundle

sealed class NavigationCommand {
    data class ToAction(val actionId: Int, val args: Bundle? = null) : NavigationCommand()
    object NavigateUp : NavigationCommand()
    data class PopUpTo(val destinationId: Int, val inclusive: Boolean) : NavigationCommand()
}
