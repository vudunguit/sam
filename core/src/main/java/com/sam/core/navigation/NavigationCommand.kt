package com.sam.core.navigation

import android.os.Bundle
import androidx.navigation.Navigator as NavNavigator

sealed class NavigationCommand {
    data class ToAction(
        val actionId: Int,
        val args: Bundle? = null,
        val extras: NavNavigator.Extras? = null
    ) : NavigationCommand()
    object NavigateUp : NavigationCommand()
    data class PopUpTo(val destinationId: Int, val inclusive: Boolean) : NavigationCommand()
}
