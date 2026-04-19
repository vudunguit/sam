package com.sam.di

import org.koin.dsl.module
import com.sam.ui.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel

val appModule = module {
    viewModel { MainViewModel() }
}
