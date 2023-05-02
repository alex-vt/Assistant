package com.alexvt.assistant

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        androidAppContext = this
    }

    companion object {
        lateinit var androidAppContext: Application
        val dependencies: AppDependencies = AppDependencies::class.create()
    }

}
