package com.sam.ui.main

import com.sam.ui.base.BaseViewModel

import com.sam.core.navigation.Navigator
import com.sam.R

class MainViewModel(
    private val navigator: Navigator
) : BaseViewModel() {
    fun navigateToSecondFragment() {
        navigator.navigate(R.id.action_fragmentMain_to_fragmentSecond)
    }

    fun navigateToSecondActivity() {
        navigator.navigate(R.id.action_fragmentMain_to_activitySecond)
    }
}
