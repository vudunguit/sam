package com.sam

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.sam.di.appModule
import com.sam.di.networkModule
import com.sam.di.databaseModule
import com.sam.core.di.coreModule

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidContext(this@MainApplication)
            modules(coreModule, appModule, networkModule, databaseModule)
        }
    }
}
