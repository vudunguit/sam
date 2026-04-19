package com.sam

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.sam.di.appModule
import com.sam.di.networkModule
import com.sam.di.databaseModule

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@MainApplication)
            modules(appModule, networkModule, databaseModule)
        }
    }
}
