package com.uvos.dashboard

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.uvos.dashboard.repository.RuleRepositoryImpl
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UVOS LogActivity - экран журнала попыток доступа (DESIGN.md §7.4).
 *
 * Строки: время • приложение • канал • вердикт.
 * Статичная красная точка слева (#E02020) у непросмотренных заблокированных попыток (без миганий).
 */
class LogActivity : Activity() {

    private lateinit var logContainer: LinearLayout
    private lateinit var btnClear: Button
    private lateinit var repository: RuleRepositoryImpl
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        repository = RuleRepositoryImpl.getInstance(this)
        logContainer = findViewById(R.id.log_items_container)
        btnClear = findViewById(R.id.btn_clear_log)

        btnClear.setOnClickListener {
            repository.clearAttempts()
            renderLog()
        }

        renderLog()
    }

    override fun onPause() {
        super.onPause()
        repository.markAllAttemptsViewed()
    }

    private fun renderLog() {
        logContainer.removeAllViews()
        val attempts = repository.getAttempts()

        if (attempts.isEmpty()) {
            val emptyView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (100 * resources.displayMetrics.density).toInt()
                )
                gravity = android.view.Gravity.CENTER
                text = getString(R.string.log_empty)
                setTextColor(getColor(R.color.uvos_text_dim))
                textSize = 14f
            }
            logContainer.addView(emptyView)
            return
        }

        attempts.forEach { event ->
            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_tile)
                setPadding(
                    (16 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt(),
                    (12 * resources.displayMetrics.density).toInt()
                )
            }

            // Статичная красная точка слева (#E02020) для непросмотренных/заблокированных записей
            val lamp = LampView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (16 * resources.displayMetrics.density).toInt(),
                    (16 * resources.displayMetrics.density).toInt()
                )
                setOn(event.verdict.isBlocked && !event.isViewed)
            }
            row.addView(lamp)

            val textContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = (12 * resources.displayMetrics.density).toInt()
                }
                orientation = LinearLayout.VERTICAL
            }

            val titleText = TextView(this).apply {
                val timeStr = timeFormat.format(Date(event.timestampMillis))
                text = "$timeStr • ${event.appName} • ${event.channel.displayName}"
                setTextColor(getColor(R.color.uvos_text))
                textSize = 14f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val subText = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = (2 * resources.displayMetrics.density).toInt()
                }
                text = "${event.verdict.logLabel} [${event.targetResource}]"
                setTextColor(getColor(if (event.verdict.isBlocked) R.color.uvos_red else R.color.uvos_text_dim))
                textSize = 12f
            }

            textContainer.addView(titleText)
            textContainer.addView(subText)
            row.addView(textContainer)

            logContainer.addView(row)

            val space = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (8 * resources.displayMetrics.density).toInt()
                )
            }
            logContainer.addView(space)
        }
    }
}
