package com.uvos.terminal.root

import android.os.ParcelFileDescriptor
import android.util.Log
import com.uvos.terminal.jni.PtyBridge
import java.io.File
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class PtyProcess private constructor(
    val pid: Int,
    val inputStream: InputStream,
    val outputStream: OutputStream,
    private val masterFd: FileDescriptor?,
    private val standardProcess: Process?
) {
    companion object {
        private const val TAG = "PtyProcess"

        fun create(
            cmd: String,
            cwd: String,
            environment: Map<String, String>,
            rows: Int,
            cols: Int
        ): PtyProcess {
            val envArray = environment.map { "${it.key}=${it.value}" }.toTypedArray()
            val pidOut = IntArray(1)

            if (PtyBridge.isNativeLoaded) {
                try {
                    val fd = PtyBridge.createSubprocess(cmd, envArray, cwd, pidOut, rows, cols)
                    if (fd != null && pidOut[0] > 0) {
                        Log.i(TAG, "Native PTY created successfully. PID: ${pidOut[0]}")
                        val inStream = FileInputStream(fd)
                        val outStream = FileOutputStream(fd)
                        return PtyProcess(
                            pid = pidOut[0],
                            inputStream = inStream,
                            outputStream = outStream,
                            masterFd = fd,
                            standardProcess = null
                        )
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Native PTY creation failed, falling back to ProcessBuilder: ${t.message}")
                }
            }

            // Fallback: Standard Java / ProcessBuilder execution
            Log.i(TAG, "Spawning process via ProcessBuilder fallback: $cmd")
            val pb = ProcessBuilder()
            val cmdParts = if (cmd.contains(" ")) {
                cmd.split(" ").filter { it.isNotBlank() }
            } else {
                listOf(cmd)
            }
            pb.command(cmdParts)
            pb.directory(File(cwd).takeIf { it.exists() } ?: File("/"))
            val pbEnv = pb.environment()
            pbEnv.putAll(environment)
            pbEnv["TERM"] = "xterm-256color"
            pbEnv["UVOS_ENVIRONMENT"] = "1"
            pb.redirectErrorStream(true)

            val proc = pb.start()
            return PtyProcess(
                pid = -1,
                inputStream = proc.inputStream,
                outputStream = proc.outputStream,
                masterFd = null,
                standardProcess = proc
            )
        }
    }

    val isPtyNative: Boolean
        get() = masterFd != null

    fun resize(rows: Int, cols: Int) {
        masterFd?.let { fd ->
            PtyBridge.setPtyWindowSize(fd, rows, cols)
        }
    }

    fun isAlive(): Boolean {
        return standardProcess?.isAlive ?: true
    }

    fun destroy() {
        try {
            masterFd?.let { fd ->
                PtyBridge.closePty(fd)
            }
        } catch (_: Throwable) {}

        try {
            inputStream.close()
        } catch (_: Throwable) {}

        try {
            outputStream.close()
        } catch (_: Throwable) {}

        try {
            standardProcess?.destroy()
        } catch (_: Throwable) {}
    }
}
