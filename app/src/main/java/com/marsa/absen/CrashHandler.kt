package com.marsa.absen

import android.app.Application
import android.content.Intent
import android.util.Log
import kotlin.system.exitProcess

object CrashHandler {
    fun install(app: Application) {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                val trace = Log.getStackTraceString(e).take(20000)
                app.startActivity(
                    Intent(app, CrashActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .putExtra(CrashActivity.EXTRA_TRACE, trace)
                )
            } catch (_: Throwable) {
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}
