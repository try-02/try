package com.sentral.org.backup

import android.content.Context
import android.content.Intent
import org.koin.core.context.stopKoin

object AppReloadHandler {
    fun restartApp(context: Context) {
        try {
            stopKoin()
        } catch (_: Exception) {}

        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        if (intent != null) {
            context.startActivity(intent)
        }
        Runtime.getRuntime().exit(0)
    }
}