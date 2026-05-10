package com.sam.core.navigation

import android.os.Bundle
import kotlinx.coroutines.flow.SharedFlow

interface Navigator {
    val navigationCommands: SharedFlow<NavigationCommand>

    fun navigate(actionId: Int, args: Bundle? = null)
    fun navigateUp()
    fun popUpTo(destinationId: Int, inclusive: Boolean)
}
