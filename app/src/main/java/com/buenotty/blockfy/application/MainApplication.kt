package com.buenotty.blockfy.application

import android.app.Application
import com.buenotty.blockfy.AppLocale
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApplication : Application() {

    override fun onCreate() {
        AppLocale.applyStored(this)
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            androidLogger()
            modules(AppModule.modules())
        }
    }
}
