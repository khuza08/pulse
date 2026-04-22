package com.elza.pulse

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

class PulseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Dependencies.init(this)
    }
}

object Dependencies {
    lateinit var application: PulseApplication
        private set

    val py by lazy {
        if (!Python.isStarted()) Python.start(AndroidPlatform(application))
        Python.getInstance()
    }

    private val module by lazy { py.getModule("download") }

    fun runDownload(id: String): String = module
        .callAttr("download", "", id) // Empty quickjsPath for now as yt-dlp has fallbacks
        .toString()

    internal fun init(application: PulseApplication) {
        this.application = application
    }
}
