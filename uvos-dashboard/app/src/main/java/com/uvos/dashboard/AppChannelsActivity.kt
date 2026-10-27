package com.uvos.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.uvos.dashboard.model.ChannelMode
import com.uvos.dashboard.model.DataChannel
import com.uvos.dashboard.repository.RuleRepositoryImpl

/**
 * UVOS AppChannelsActivity - детальный щиток 13 каналов конкретного приложения (DESIGN.md §7.2).
 */
class AppChannelsActivity : Activity() {

    private lateinit var appTitle: TextView
    private lateinit var appPackage: TextView
    private lateinit var appGauge: GaugeView
    private lateinit var channelsContainer: LinearLayout
    private lateinit var repository: RuleRepositoryImpl
    private var packageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_channels)

        repository = RuleRepositoryImpl.getInstance(this)
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""

        appTitle = findViewById(R.id.app_detail_title)
        appPackage = findViewById(R.id.app_detail_package)
        appGauge = findViewById(R.id.app_gauge)
        channelsContainer = findViewById(R.id.channels_list_container)
    }

    override fun onResume() {
        super.onResume()
        renderChannels()
    }

    private fun renderChannels() {
        val app = repository.getAppRule(packageName)
        if (app == null) {
            finish()
            return
        }

        appTitle.text = app.appName
        appPackage.text = app.packageName

        val openCount = app.openChannelsCount
        appGauge.setLevel(openCount / 13f, "$openCount / 13")

        channelsContainer.removeAllViews()

        DataChannel.values().forEach { channel ->
            val row = layoutInflater.inflate(R.layout.shield_row, channelsContainer, false)
            val nameView = row.findViewById<TextView>(R.id.channel_name)
            val statusView = row.findViewById<TextView>(R.id.channel_status)
            val lamp = row.findViewById<LampView>(R.id.channel_lamp)

            val isNetwork = (channel == DataChannel.NETWORK)
            val currentMode = app.channelModes[channel] ?: ChannelMode.CLOSED
            val isOpen = (currentMode == ChannelMode.OPEN)

            if (isNetwork) {
                nameView.text = "${channel.displayName} (регулятор)"
                statusView.text = "${app.networkConfig.mode.displayName} →"
                lamp.setOn(app.networkConfig.mode != com.uvos.dashboard.model.NetworkMode.FULLY_CLOSED)
            } else {
                nameView.text = channel.displayName
                statusView.text = getString(if (isOpen) R.string.channel_open else R.string.channel_closed)
                lamp.setOn(isOpen)
            }

            statusView.setTextColor(
                getColor(if (isOpen || (isNetwork && lamp.isOn())) R.color.uvos_text else R.color.uvos_text_dim)
            )

            row.setOnClickListener {
                if (isNetwork) {
                    val intent = Intent(this, NetworkRegulatorActivity::class.java).apply {
                        putExtra(NetworkRegulatorActivity.EXTRA_PACKAGE_NAME, packageName)
                    }
                    startActivity(intent)
                } else {
                    if (isOpen) {
                        // Отзыв открытого канала требует диалога подтверждения (DESIGN.md §3.9, CONTROL-SCHEME.md §3b)
                        RevocationDialog(this, channel.displayName) {
                            repository.setChannelMode(packageName, channel, ChannelMode.CLOSED)
                            renderChannels()
                        }.show()
                    } else {
                        // Открытие канала владельцем происходит мгновенно в 1 тап
                        repository.setChannelMode(packageName, channel, ChannelMode.OPEN)
                        renderChannels()
                    }
                }
            }

            channelsContainer.addView(row)

            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, (1 * resources.displayMetrics.density).toInt()
                )
                setBackgroundColor(getColor(R.color.uvos_divider))
            }
            channelsContainer.addView(divider)
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}
