package com.sam.core.navigation

import android.os.Bundle
import androidx.navigation.Navigator as NavNavigator
import kotlinx.coroutines.flow.SharedFlow

interface Navigator {
    val navigationCommands: SharedFlow<NavigationCommand>

    fun navigate(actionId: Int, args: Bundle? = null)
    fun navigate(actionId: Int, args: Bundle?, extras: NavNavigator.Extras?)
    fun navigateUp()
    fun popUpTo(destinationId: Int, inclusive: Boolean)
}
