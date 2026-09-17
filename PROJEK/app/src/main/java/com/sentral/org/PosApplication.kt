package com.sentral.org

import android.app.Application
import com.sentral.org.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class PosApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            // ERROR agar rilis tidak membocorkan detail internal;
            // naikkan sementara ke DEBUG saat investigasi masalah DI.
            androidLogger(Level.ERROR)
            androidContext(this@PosApplication)
            modules(appModule)
        }
    }
}