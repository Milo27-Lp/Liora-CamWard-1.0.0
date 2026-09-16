package com.aistudio.lioracamward

import android.app.Application
import com.aistudio.lioracamward.data.local.ScanDatabase
import com.aistudio.lioracamward.data.repository.ScanRepository

class LioraApp : Application() {

    lateinit var database: ScanDatabase
        private set

    lateinit var scanRepository: ScanRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = ScanDatabase.getDatabase(this)
        scanRepository = ScanRepository(database.scanDao())
    }
}
