package com.haprial.app

import android.app.Application
import com.haprial.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HaprialApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@HaprialApp)
            modules(appModule)
        }
    }
}
