package com.uvos.terminal.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootShellManager {
    private var isRootGranted: Boolean = false

    /**
     * Asynchronously tests root access on Dispatchers.IO to prevent freezing the UI.
     */
    suspend fun checkAndAcquireRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Test if app is granted root
            val root = Shell.isAppGrantedRoot() == true
            if (root) {
                isRootGranted = true
                return@withContext true
            }

            // Fallback active test
            val result = Shell.cmd("id").exec()
            isRootGranted = result.isSuccess && result.out.any { it.contains("uid=0") }
            isRootGranted
        } catch (t: Throwable) {
            isRootGranted = false
            false
        }
    }

    fun getPreferredShellCommand(): String {
        return if (isRootGranted) "su --mount-master" else "/system/bin/sh"
    }

    fun isRoot(): Boolean = isRootGranted
}
