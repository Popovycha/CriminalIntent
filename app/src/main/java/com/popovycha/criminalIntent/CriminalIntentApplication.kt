package com.popovycha.criminalIntent

import android.app.Application

class CriminalIntentApplication : Application() {
    override fun onCreate() {
        //good place to do any kind of one-time initialization operations.
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}