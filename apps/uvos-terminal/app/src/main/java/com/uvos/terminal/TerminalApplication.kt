package com.uvos.terminal

import android.app.Application
import com.topjohnwu.superuser.Shell

class TerminalApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Configure libsu defaults globally
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_MOUNT_MASTER)
                .setTimeout(10)
        )
    }
}
