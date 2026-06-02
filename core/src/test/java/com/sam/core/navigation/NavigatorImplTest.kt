package com.sam.core.navigation

import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.mockk.mockk

@OptIn(ExperimentalCoroutinesApi::class)
class NavigatorImplTest {

    private lateinit var navigator: NavigatorImpl

    @Before
    fun setup() {
        navigator = NavigatorImpl()
    }

    @Test
    fun navigate_emitsToActionCommand() = runTest {
        val actionId = 123
        val bundle = mockk<Bundle>(relaxed = true)
        
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            val command = navigator.navigationCommands.first() as NavigationCommand.ToAction
            assertEquals(actionId, command.actionId)
            assertEquals(bundle, command.args)
        }
        
        navigator.navigate(actionId, bundle)
        job.cancel()
    }

    @Test
    fun navigateUp_emitsNavigateUpCommand() = runTest {
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            val command = navigator.navigationCommands.first()
            assertTrue(command is NavigationCommand.NavigateUp)
        }
        
        navigator.navigateUp()
        job.cancel()
    }

    @Test
    fun popUpTo_emitsPopUpToCommand() = runTest {
        val destinationId = 456
        val inclusive = true
        
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            val command = navigator.navigationCommands.first() as NavigationCommand.PopUpTo
            assertEquals(destinationId, command.destinationId)
            assertEquals(inclusive, command.inclusive)
        }
        
        navigator.popUpTo(destinationId, inclusive)
        job.cancel()
    }
}
