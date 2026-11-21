package com.uvos.dashboard

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * UVOS TorchController - контроллер аппаратного фонарика UnderVoltOS.
 *
 * Архитектура и принципы:
 * - Управление фонариком через CameraManager.setTorchMode(cameraId, enabled).
 * - Подписка на CameraManager.TorchCallback для отслеживания внешних изменений состояния фонарика.
 * - Потокобезопасная доставка событий в главный поток (UI Thread).
 */
class TorchController(private val context: Context) {

    companion object {
        private const val TAG = "TorchController"
    }

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var cameraId: String? = null
    private var isTorchOn: Boolean = false

    var onTorchStateChanged: ((Boolean) -> Unit)? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(camId: String, enabled: Boolean) {
            super.onTorchModeChanged(camId, enabled)
            if (cameraId == null || camId == cameraId) {
                if (cameraId == null) {
                    cameraId = camId
                }
                isTorchOn = enabled
                notifyStateChanged(enabled)
            }
        }

        override fun onTorchModeUnavailable(camId: String) {
            super.onTorchModeUnavailable(camId)
            if (cameraId == null || camId == cameraId) {
                isTorchOn = false
                notifyStateChanged(false)
            }
        }
    }

    init {
        initCamera()
        registerCallback()
    }

    private fun initCamera() {
        val manager = cameraManager ?: return
        try {
            val cameraIds = manager.cameraIdList
            for (id in cameraIds) {
                val characteristics = manager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    break
                } else if (hasFlash && cameraId == null) {
                    cameraId = id
                }
            }
            if (cameraId == null && cameraIds.isNotEmpty()) {
                cameraId = cameraIds[0]
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing camera for torch", e)
        }
    }

    private fun registerCallback() {
        try {
            cameraManager?.registerTorchCallback(torchCallback, mainHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error registering torch callback", e)
        }
    }

    private fun notifyStateChanged(enabled: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onTorchStateChanged?.invoke(enabled)
        } else {
            mainHandler.post {
                onTorchStateChanged?.invoke(enabled)
            }
        }
    }

    fun setTorchMode(enabled: Boolean) {
        if (cameraId == null) {
            initCamera()
        }
        val id = cameraId
        if (id == null) {
            Log.w(TAG, "Cannot set torch mode: cameraId is null")
            return
        }
        try {
            cameraManager?.setTorchMode(id, enabled)
            isTorchOn = enabled
            notifyStateChanged(enabled)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting torch mode to $enabled", e)
        }
    }

    fun toggleTorch(): Boolean {
        setTorchMode(!isTorchOn)
        return isTorchOn
    }

    fun isTorchActive(): Boolean = isTorchOn

    fun release() {
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering torch callback", e)
        }
    }
}
