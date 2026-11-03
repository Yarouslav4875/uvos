package com.uvos.terminal.jni

import android.util.Log
import java.io.FileDescriptor

object PtyBridge {
    private const val TAG = "PtyBridge"
    private var nativeLoaded = false

    init {
        try {
            System.loadLibrary("uvos_pty")
            nativeLoaded = true
            Log.i(TAG, "Native library libuvos_pty loaded successfully.")
        } catch (t: Throwable) {
            nativeLoaded = false
            Log.w(TAG, "Native library libuvos_pty not found or failed to load. Operating in fallback mode: ${t.message}")
        }
    }

    val isNativeLoaded: Boolean
        get() = nativeLoaded

    /**
     * Creates a pseudo-terminal, forks a child process and starts the command.
     * @return Master PTY FileDescriptor or null if error.
     */
    fun createSubprocess(
        cmd: String,
        environment: Array<String>,
        cwd: String,
        processIdOut: IntArray,
        rows: Int,
        cols: Int
    ): FileDescriptor? {
        if (!nativeLoaded) return null
        return try {
            nativeCreateSubprocess(cmd, environment, cwd, processIdOut, rows, cols)
        } catch (t: Throwable) {
            Log.e(TAG, "nativeCreateSubprocess failed", t)
            null
        }
    }

    /**
     * Updates terminal window dimensions for htop/nano/vi.
     */
    fun setPtyWindowSize(fd: FileDescriptor, rows: Int, cols: Int) {
        if (!nativeLoaded) return
        try {
            nativeSetPtyWindowSize(fd, rows, cols)
        } catch (t: Throwable) {
            Log.e(TAG, "nativeSetPtyWindowSize failed", t)
        }
    }

    /**
     * Closes the PTY descriptor.
     */
    fun closePty(fd: FileDescriptor) {
        if (!nativeLoaded) return
        try {
            nativeClosePty(fd)
        } catch (t: Throwable) {
            Log.e(TAG, "nativeClosePty failed", t)
        }
    }

    /**
     * Waits for child process to terminate.
     */
    fun waitForProcess(pid: Int): Int {
        if (!nativeLoaded) return -1
        return try {
            nativeWaitForProcess(pid)
        } catch (t: Throwable) {
            Log.e(TAG, "nativeWaitForProcess failed", t)
            -1
        }
    }

    @JvmStatic
    private external fun nativeCreateSubprocess(
        cmd: String,
        environment: Array<String>,
        cwd: String,
        processIdOut: IntArray,
        rows: Int,
        cols: Int
    ): FileDescriptor?

    @JvmStatic
    private external fun nativeSetPtyWindowSize(fd: FileDescriptor, rows: Int, cols: Int)

    @JvmStatic
    private external fun nativeClosePty(fd: FileDescriptor)

    @JvmStatic
    private external fun nativeWaitForProcess(pid: Int): Int
}
