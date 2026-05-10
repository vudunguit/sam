package com.sam.di

import org.koin.dsl.module
import com.sam.ui.main.MainViewModel
import org.koin.androidx.viewmodel.dsl.viewModel

import com.sam.data.MediaRepository

val appModule = module {
    single { MediaRepository(get()) }
    viewModel { MainViewModel(get(), get()) }
}
