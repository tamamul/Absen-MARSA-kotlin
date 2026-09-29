package com.marsa.absen

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MarsaApp : Application() {
    override fun onCreate() {
        CrashHandler.install(this) // tampilkan stack trace jika crash (hapus di rilis)
        super.onCreate()
    }
}
