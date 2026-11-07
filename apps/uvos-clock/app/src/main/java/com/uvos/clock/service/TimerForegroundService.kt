package com.uvos.clock.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.uvos.clock.R
import com.uvos.clock.UvosClockApplication
import com.uvos.clock.audio.AscendingToneGenerator
import com.uvos.clock.ui.main.ClockTasksActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var timerJob: Job? = null
    private var toneGenerator: AscendingToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        toneGenerator = AscendingToneGenerator(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val totalMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 60_000L)
                startTimer(totalMillis)
            }
            ACTION_PAUSE -> {
                pauseTimer()
            }
            ACTION_RESUME -> {
                resumeTimer()
            }
            ACTION_RESET -> {
                resetTimer()
            }
            ACTION_STOP -> {
                stopTimerService()
            }
        }

        return START_NOT_STICKY
    }

    private fun startTimer(totalMillis: Long) {
        _totalTimeMillis.value = totalMillis
        _remainingTimeMillis.value = totalMillis
        _timerState.value = TimerState.RUNNING

        startForegroundNotification()

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            val endTime = System.currentTimeMillis() + totalMillis
            while (isActive && _remainingTimeMillis.value > 0) {
                delay(50)
                val remaining = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                _remainingTimeMillis.value = remaining
                if (remaining % 1000 < 60) {
                    updateNotification(remaining)
                }
            }

            if (_remainingTimeMillis.value <= 0) {
                onTimerFinished()
            }
        }
    }

    private fun pauseTimer() {
        if (_timerState.value == TimerState.RUNNING) {
            timerJob?.cancel()
            _timerState.value = TimerState.PAUSED
            updateNotification(_remainingTimeMillis.value)
        }
    }

    private fun resumeTimer() {
        if (_timerState.value == TimerState.PAUSED && _remainingTimeMillis.value > 0) {
            val remaining = _remainingTimeMillis.value
            _timerState.value = TimerState.RUNNING
            startForegroundNotification()

            timerJob?.cancel()
            timerJob = serviceScope.launch {
                val endTime = System.currentTimeMillis() + remaining
                while (isActive && _remainingTimeMillis.value > 0) {
                    delay(50)
                    val rem = (endTime - System.currentTimeMillis()).coerceAtLeast(0L)
                    _remainingTimeMillis.value = rem
                    if (rem % 1000 < 60) {
                        updateNotification(rem)
                    }
                }

                if (_remainingTimeMillis.value <= 0) {
                    onTimerFinished()
                }
            }
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        toneGenerator?.stop()
        _remainingTimeMillis.value = _totalTimeMillis.value
        _timerState.value = TimerState.IDLE
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerFinished() {
        _timerState.value = TimerState.FINISHED
        updateNotification(0)
        toneGenerator?.start(rampDurationSec = 10)
    }

    private fun stopTimerService() {
        timerJob?.cancel()
        toneGenerator?.stop()
        _timerState.value = TimerState.IDLE
        _remainingTimeMillis.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundNotification() {
        val notification = buildNotification(_remainingTimeMillis.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(remainingMillis: Long) {
        val notification = buildNotification(remainingMillis)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(remainingMillis: Long): Notification {
        val seconds = (remainingMillis / 1000) % 60
        val minutes = (remainingMillis / (1000 * 60)) % 60
        val hours = (remainingMillis / (1000 * 60 * 60))
        val formatted = if (hours > 0) {
            "%02d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }

        val openIntent = Intent(this, ClockTasksActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_TAB", 2) // TAB 2 is Timer
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            900,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (_timerState.value) {
            TimerState.RUNNING -> "Отсчет времени: $formatted"
            TimerState.PAUSED -> "Пауза: $formatted"
            TimerState.FINISHED -> "ВРЕМЯ ИСТЕКЛО!"
            TimerState.IDLE -> "Готов к запуску"
        }

        return NotificationCompat.Builder(this, UvosClockApplication.CHANNEL_TIMER_ID)
            .setSmallIcon(R.drawable.ic_launcher_clock)
            .setContentTitle("Приборный таймер M4200")
            .setContentText(statusText)
            .setOngoing(_timerState.value == TimerState.RUNNING)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingOpen)
            .build()
    }

    override fun onDestroy() {
        timerJob?.cancel()
        toneGenerator?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    enum class TimerState {
        IDLE, RUNNING, PAUSED, FINISHED
    }

    companion object {
        const val NOTIFICATION_ID = 4200

        const val ACTION_START = "com.uvos.clock.timer.START"
        const val ACTION_PAUSE = "com.uvos.clock.timer.PAUSE"
        const val ACTION_RESUME = "com.uvos.clock.timer.RESUME"
        const val ACTION_RESET = "com.uvos.clock.timer.RESET"
        const val ACTION_STOP = "com.uvos.clock.timer.STOP"

        const val EXTRA_DURATION_MILLIS = "extra_duration_millis"

        private val _remainingTimeMillis = MutableStateFlow(0L)
        val remainingTimeMillis: StateFlow<Long> = _remainingTimeMillis.asStateFlow()

        private val _totalTimeMillis = MutableStateFlow(0L)
        val totalTimeMillis: StateFlow<Long> = _totalTimeMillis.asStateFlow()

        private val _timerState = MutableStateFlow(TimerState.IDLE)
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

        fun startTimer(context: Context, durationMillis: Long) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_MILLIS, durationMillis)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun resetTimer(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_RESET
            }
            context.startService(intent)
        }
    }
}
