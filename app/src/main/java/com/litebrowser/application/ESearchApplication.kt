package com.litebrowser.application

import android.app.Application
import io.github.edsuns.adfilter.AdFilter
import com.litebrowser.database.ESearchDatabase

class ESearchApplication : Application() {

    companion object {
        lateinit var database: ESearchDatabase
        var coeff = 0
    }

    override fun onCreate() {
        super.onCreate()

        AdFilter.create(this)
    }

}