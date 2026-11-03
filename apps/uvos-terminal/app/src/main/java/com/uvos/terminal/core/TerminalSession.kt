package com.uvos.terminal.core

import android.content.Context
import android.util.Log
import com.uvos.terminal.root.PtyProcess
import com.uvos.terminal.root.RootShellManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class TerminalSession(
    private val context: Context,
    private val scope: CoroutineScope,
    var rows: Int = 24,
    var cols: Int = 80
) {
    companion object {
        private const val TAG = "TerminalSession"
    }

    val emulator = TerminalEmulator(rows, cols)
    private var process: PtyProcess? = null
    private var readerJob: Job? = null

    private val _isPtyActive = MutableStateFlow(false)
    val isPtyActive: StateFlow<Boolean> = _isPtyActive.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private var auditFileOutputStream: FileOutputStream? = null

    fun start(commandOverride: String? = null) {
        scope.launch(Dispatchers.IO) {
            try {
                // Ensure Root is checked
                RootShellManager.checkAndAcquireRoot()

                val cmd = commandOverride ?: RootShellManager.getPreferredShellCommand()
                val cwd = context.filesDir.absolutePath
                val env = mutableMapOf(
                    "HOME" to context.filesDir.absolutePath,
                    "PATH" to "/system/bin:/system/xbin:/sbin:/vendor/bin:/apex/com.android.runtime/bin",
                    "TERM" to "xterm-256color",
                    "UVOS_TERMINAL" to "1"
                )

                val pty = PtyProcess.create(cmd, cwd, env, rows, cols)
                process = pty
                _isPtyActive.value = true

                // Initial welcome banner in retro industrial style
                val banner = "\u001B[31m[UVOS TERMINAL v1.0.0-UVOS]\u001B[0m\r\n" +
                        "\u001B[33mActive Shell: $cmd\u001B[0m\r\n" +
                        "\u001B[32mRoot Mode: ${if (RootShellManager.isRoot()) "MASTER MOUNT (UID 0)" else "RESTRICTED (UID != 0)"}\u001B[0m\r\n" +
                        "\u001B[36mZero-Telemetry Invariant: ENFORCED\u001B[0m\r\n\r\n"
                emulator.processBytes(banner.toByteArray(Charsets.UTF_8))

                startReading(pty)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to launch terminal process", t)
                val err = "\r\n\u001B[31mProcess launch error: ${t.message}\u001B[0m\r\n"
                emulator.processBytes(err.toByteArray(Charsets.UTF_8))
                _isPtyActive.value = false
            }
        }
    }

    private fun startReading(pty: PtyProcess) {
        readerJob?.cancel()
        readerJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            try {
                while (isActive && pty.isAlive()) {
                    val read = pty.inputStream.read(buffer)
                    if (read > 0) {
                        emulator.processBytes(buffer, 0, read)
                        recordAudit(buffer, 0, read)
                    } else if (read == -1) {
                        break
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "PTY stream closed: ${e.message}")
            } finally {
                _isPtyActive.value = false
                val exitMsg = "\r\n\u001B[33m[Session Terminated]\u001B[0m\r\n"
                emulator.processBytes(exitMsg.toByteArray(Charsets.UTF_8))
            }
        }
    }

    fun write(bytes: ByteArray) {
        scope.launch(Dispatchers.IO) {
            try {
                process?.outputStream?.write(bytes)
                process?.outputStream?.flush()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to write bytes to PTY", e)
            }
        }
    }

    fun write(text: String) {
        write(text.toByteArray(Charsets.UTF_8))
    }

    /**
     * Sends control keys with latch/lock modifier states.
     */
    fun sendControlKey(baseChar: Char, ctrl: Boolean, alt: Boolean) {
        val bytes = mutableListOf<Byte>()
        if (alt) {
            bytes.add(0x1B.toByte()) // ESC prefix
        }

        if (ctrl) {
            val upper = baseChar.uppercaseChar()
            if (upper in '@'..'_') {
                bytes.add((upper.code - '@'.code).toByte())
            } else if (baseChar == ' ' || baseChar == '2') {
                bytes.add(0.toByte())
            } else {
                bytes.add(baseChar.code.toByte())
            }
        } else {
            bytes.add(baseChar.code.toByte())
        }

        write(bytes.toByteArray())
    }

    fun sendSpecialKey(code: String) {
        when (code) {
            "ESC" -> write(byteArrayOf(0x1B))
            "TAB" -> write(byteArrayOf(0x09))
            "ENTER" -> write("\n")
            "BACKSPACE" -> write(byteArrayOf(0x7F))
            "UP" -> write("\u001B[A")
            "DOWN" -> write("\u001B[B")
            "RIGHT" -> write("\u001B[C")
            "LEFT" -> write("\u001B[D")
            "HOME" -> write("\u001B[H")
            "END" -> write("\u001B[F")
            "PIPE" -> write("|")
            "SLASH" -> write("/")
            "DASH" -> write("-")
            "UNDERSCORE" -> write("_")
            "TILDE" -> write("~")
        }
    }

    fun resize(newRows: Int, newCols: Int) {
        rows = newRows
        cols = newCols
        emulator.resize(newRows, newCols)
        process?.resize(newRows, newCols)
    }

    fun toggleAuditRecording() {
        val newState = !_isRecording.value
        _isRecording.value = newState
        if (newState) {
            try {
                val auditDir = File(context.filesDir, "audit")
                if (!auditDir.exists()) auditDir.mkdirs()
                val logFile = File(auditDir, "session_${System.currentTimeMillis()}.log")
                auditFileOutputStream = FileOutputStream(logFile, true)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to start audit recording", t)
                _isRecording.value = false
            }
        } else {
            try {
                auditFileOutputStream?.close()
                auditFileOutputStream = null
            } catch (_: Throwable) {}
        }
    }

    private fun recordAudit(buffer: ByteArray, offset: Int, length: Int) {
        if (_isRecording.value) {
            try {
                auditFileOutputStream?.write(buffer, offset, length)
            } catch (_: Throwable) {}
        }
    }

    fun destroy() {
        readerJob?.cancel()
        process?.destroy()
        process = null
        _isPtyActive.value = false
        try {
            auditFileOutputStream?.close()
        } catch (_: Throwable) {}
    }
}
