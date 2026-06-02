package com.sam

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.sam.di.appModule
import com.sam.di.networkModule
import com.sam.di.databaseModule
import com.sam.core.di.coreModule

class MainApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@MainApplication)
            modules(coreModule, appModule, networkModule, databaseModule)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }
}
