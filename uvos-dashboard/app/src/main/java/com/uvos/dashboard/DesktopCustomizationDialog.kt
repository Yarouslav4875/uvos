package com.uvos.dashboard

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

/**
 * DesktopCustomizationDialog - Диалог кастомизации рабочего стола UVOS.
 * Содержит две вкладки:
 * 1. «ОБОИ» - выбор системных пресетов (AMOLED, Тактическая сетка, Карбон, Радар-сталь),
 *    выбор пользовательского фото через SAF и интерактивный регулятор затемнения (0-80%).
 * 2. «ИКОНКИ» - мгновенное переключение наборов значков (DARK_GLASS, RETRO_M4200, SYSTEM_DEFAULT).
 */
class DesktopCustomizationDialog(
    private val activity: Activity,
    private val wallpaperManager: WallpaperManager,
    private val iconPackManager: IconPackManager,
    private val onPickImageRequested: () -> Unit
) {

    private val dialog: Dialog = Dialog(activity)

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_desktop_customization)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupViews()
    }

    private fun setupViews() {
        val btnTabWallpaper = dialog.findViewById<TextView>(R.id.btn_tab_wallpaper)
        val btnTabIcons = dialog.findViewById<TextView>(R.id.btn_tab_icons)
        val containerWallpaper = dialog.findViewById<LinearLayout>(R.id.container_tab_wallpaper)
        val containerIcons = dialog.findViewById<LinearLayout>(R.id.container_tab_icons)

        // Переключение вкладок
        btnTabWallpaper.setOnClickListener {
            btnTabWallpaper.setBackgroundResource(R.drawable.bg_button_primary)
            btnTabWallpaper.setTextColor(activity.getColor(R.color.uvos_text))
            btnTabIcons.setBackgroundColor(Color.TRANSPARENT)
            btnTabIcons.setTextColor(activity.getColor(R.color.uvos_text_dim))

            containerWallpaper.visibility = View.VISIBLE
            containerIcons.visibility = View.GONE
        }

        btnTabIcons.setOnClickListener {
            btnTabIcons.setBackgroundResource(R.drawable.bg_button_primary)
            btnTabIcons.setTextColor(activity.getColor(R.color.uvos_text))
            btnTabWallpaper.setBackgroundColor(Color.TRANSPARENT)
            btnTabWallpaper.setTextColor(activity.getColor(R.color.uvos_text_dim))

            containerWallpaper.visibility = View.GONE
            containerIcons.visibility = View.VISIBLE
        }

        // =======================
        // ВКЛАДКА 1: ОБОИ
        // =======================
        val btnAmoled = dialog.findViewById<View>(R.id.btn_preset_amoled)
        val btnGrid = dialog.findViewById<View>(R.id.btn_preset_grid)
        val btnCarbon = dialog.findViewById<View>(R.id.btn_preset_carbon)
        val btnRadar = dialog.findViewById<View>(R.id.btn_preset_radar)
        val btnCustom = dialog.findViewById<View>(R.id.btn_choose_custom_photo)
        val txtCustomPhotoTitle = dialog.findViewById<TextView>(R.id.txt_custom_photo_title)

        val dotAmoled = dialog.findViewById<View>(R.id.indicator_preset_amoled)
        val dotGrid = dialog.findViewById<View>(R.id.indicator_preset_grid)
        val dotCarbon = dialog.findViewById<View>(R.id.indicator_preset_carbon)
        val dotRadar = dialog.findViewById<View>(R.id.indicator_preset_radar)
        val dotCustom = dialog.findViewById<View>(R.id.indicator_preset_custom)

        fun updateWallpaperIndicators() {
            val mode = wallpaperManager.getWallpaperMode()
            dotAmoled.setBackgroundResource(if (mode == WallpaperManager.WallpaperMode.SOLID_AMOLED) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotGrid.setBackgroundResource(if (mode == WallpaperManager.WallpaperMode.PRESET_TACTICAL_GRID) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotCarbon.setBackgroundResource(if (mode == WallpaperManager.WallpaperMode.PRESET_CARBON_FIBER) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotRadar.setBackgroundResource(if (mode == WallpaperManager.WallpaperMode.PRESET_STEEL_RADAR) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotCustom.setBackgroundResource(if (mode == WallpaperManager.WallpaperMode.CUSTOM_PHOTO) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)

            txtCustomPhotoTitle.text = if (wallpaperManager.hasCustomWallpaper()) {
                if (mode == WallpaperManager.WallpaperMode.CUSTOM_PHOTO) "Пользовательское фото (Активно)" else "Пользовательское фото (Загружено)"
            } else {
                "Выбрать фото через SAF..."
            }
        }

        btnAmoled.setOnClickListener {
            wallpaperManager.setWallpaperMode(WallpaperManager.WallpaperMode.SOLID_AMOLED)
            updateWallpaperIndicators()
        }

        btnGrid.setOnClickListener {
            wallpaperManager.setWallpaperMode(WallpaperManager.WallpaperMode.PRESET_TACTICAL_GRID)
            updateWallpaperIndicators()
        }

        btnCarbon.setOnClickListener {
            wallpaperManager.setWallpaperMode(WallpaperManager.WallpaperMode.PRESET_CARBON_FIBER)
            updateWallpaperIndicators()
        }

        btnRadar.setOnClickListener {
            wallpaperManager.setWallpaperMode(WallpaperManager.WallpaperMode.PRESET_STEEL_RADAR)
            updateWallpaperIndicators()
        }

        btnCustom.setOnClickListener {
            if (wallpaperManager.hasCustomWallpaper() && wallpaperManager.getWallpaperMode() != WallpaperManager.WallpaperMode.CUSTOM_PHOTO) {
                wallpaperManager.setWallpaperMode(WallpaperManager.WallpaperMode.CUSTOM_PHOTO)
                updateWallpaperIndicators()
            } else {
                onPickImageRequested()
            }
        }

        // Регулятор затемнения (0 - 80%)
        val txtDimLabel = dialog.findViewById<TextView>(R.id.txt_dim_level_label)
        val seekbarDim = dialog.findViewById<SeekBar>(R.id.seekbar_dim_level)

        val currentDim = wallpaperManager.getDimLevel()
        seekbarDim.progress = currentDim
        txtDimLabel.text = "УРОВЕНЬ ЗАТЕМНЕНИЯ ФОНА: $currentDim%"

        seekbarDim.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    txtDimLabel.text = "УРОВЕНЬ ЗАТЕМНЕНИЯ ФОНА: $progress%"
                    wallpaperManager.setDimLevel(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // =======================
        // ВКЛАДКА 2: ИКОНКИ
        // =======================
        val cardDarkGlass = dialog.findViewById<View>(R.id.card_pack_dark_glass)
        val cardRetroM4200 = dialog.findViewById<View>(R.id.card_pack_retro_m4200)
        val cardSystemDefault = dialog.findViewById<View>(R.id.card_pack_system_default)

        val dotDarkGlass = dialog.findViewById<View>(R.id.indicator_pack_dark_glass)
        val dotRetroM4200 = dialog.findViewById<View>(R.id.indicator_pack_retro_m4200)
        val dotSystemDefault = dialog.findViewById<View>(R.id.indicator_pack_system_default)

        fun updateIconIndicators() {
            val pack = iconPackManager.getIconPack()
            dotDarkGlass.setBackgroundResource(if (pack == IconPackManager.IconPackType.DARK_GLASS) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotRetroM4200.setBackgroundResource(if (pack == IconPackManager.IconPackType.RETRO_M4200) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
            dotSystemDefault.setBackgroundResource(if (pack == IconPackManager.IconPackType.SYSTEM_DEFAULT) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
        }

        cardDarkGlass.setOnClickListener {
            iconPackManager.setIconPack(IconPackManager.IconPackType.DARK_GLASS)
            updateIconIndicators()
        }

        cardRetroM4200.setOnClickListener {
            iconPackManager.setIconPack(IconPackManager.IconPackType.RETRO_M4200)
            updateIconIndicators()
        }

        cardSystemDefault.setOnClickListener {
            iconPackManager.setIconPack(IconPackManager.IconPackType.SYSTEM_DEFAULT)
            updateIconIndicators()
        }

        // Первичная синхронизация индикаторов
        updateWallpaperIndicators()
        updateIconIndicators()

        // Кнопка закрытия
        dialog.findViewById<Button>(R.id.btn_close_customization).setOnClickListener {
            dialog.dismiss()
        }
    }

    fun show() {
        dialog.show()
    }

    fun updateAfterSafPhotoSelected() {
        dialog.findViewById<View>(R.id.indicator_preset_custom)?.setBackgroundResource(R.drawable.bg_dot_active)
        dialog.findViewById<TextView>(R.id.txt_custom_photo_title)?.text = "Пользовательское фото (Активно)"
    }
}
