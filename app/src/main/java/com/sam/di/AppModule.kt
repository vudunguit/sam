package com.sam.di

import org.koin.dsl.module
import com.sam.ui.main.MainViewModel
import com.sam.ui.duplicate.DuplicatePhotoViewModel
import org.koin.androidx.viewmodel.dsl.viewModel

import com.sam.data.MediaRepository
import com.sam.data.ImageSimilarityAnalyzer

val appModule = module {
    single { MediaRepository(get()) }
    single { ImageSimilarityAnalyzer() }
    viewModel { MainViewModel(get(), get()) }
    viewModel { DuplicatePhotoViewModel(get(), get(), get()) }
}
