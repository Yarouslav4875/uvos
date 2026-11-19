package com.uvos.dashboard

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView

/**
 * IconPackManager - Управление наборами значков и стилистикой рабочего стола UVOS:
 * 1. DARK_GLASS: Тёмное полупрозрачное стекло с холодным неоновым отливом (Ice Cyan).
 * 2. RETRO_M4200: Индустриальный янтарный винтаж, металлические фаски, приборная шкала.
 * 3. SYSTEM_DEFAULT: Стандартный тактический тёмный минимализм UnderVoltOS.
 *
 * Обеспечивает мгновенное переключение без перезапуска Activity (120Hz Hot-Swap).
 */
class IconPackManager(private val context: Context) {

    enum class IconPackType(val title: String, val description: String) {
        DARK_GLASS("ТЁМНОЕ СТЕКЛО", "Полупрозрачные стеклянные плитки с неоновым свечением"),
        RETRO_M4200("РЕТРО M4200", "Индустриальный янтарный корпус и приборные шкалы"),
        SYSTEM_DEFAULT("СТАНДАРТ UVOS", "Тактический тёмный минимализм UnderVoltOS")
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("uvos_icon_pack_prefs", Context.MODE_PRIVATE)
    private val keyIconPack = "active_icon_pack"

    var onIconPackChanged: (() -> Unit)? = null

    fun getIconPack(): IconPackType {
        val name = prefs.getString(keyIconPack, IconPackType.DARK_GLASS.name) ?: IconPackType.DARK_GLASS.name
        return try {
            IconPackType.valueOf(name)
        } catch (_: Exception) {
            IconPackType.DARK_GLASS
        }
    }

    fun setIconPack(pack: IconPackType) {
        prefs.edit().putString(keyIconPack, pack.name).apply()
        onIconPackChanged?.invoke()
    }

    /**
     * Создание динамического фонового Drawable для плитки значка с поддержкой кликабельности (Ripple / Pressed)
     */
    fun createTileDrawable(pack: IconPackType): Drawable {
        if (pack == IconPackType.DARK_GLASS) {
            return GlassTileDrawable(cornerRadiusDp = 18f, hasNeonGlow = true, neonColor = Color.parseColor("#FF3B30"))
        }

        val stateList = StateListDrawable()
        val dp = context.resources.displayMetrics.density

        when (pack) {
            IconPackType.DARK_GLASS -> {
                // Handled above via GlassTileDrawable
            }

            IconPackType.RETRO_M4200 -> {
                val radius = 12f * dp
                val strokeWidth = (1.6f * dp).toInt()

                val pressed = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(Color.parseColor("#2C2214"))
                    setStroke(strokeWidth, Color.parseColor("#FFB74D"))
                }
                val normal = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(Color.parseColor("#14110C"))
                    setStroke(strokeWidth, Color.parseColor("#E8A238"))
                }
                stateList.addState(intArrayOf(android.R.attr.state_pressed), pressed)
                stateList.addState(intArrayOf(), normal)
            }

            IconPackType.SYSTEM_DEFAULT -> {
                val radius = 24f * dp
                val strokeWidth = (1f * dp).toInt()

                val pressed = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(Color.parseColor("#161822"))
                    setStroke(strokeWidth, Color.parseColor("#2D3346"))
                }
                val normal = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radius
                    setColor(Color.parseColor("#0C0D13"))
                    setStroke(strokeWidth, Color.parseColor("#1F2230"))
                }
                stateList.addState(intArrayOf(android.R.attr.state_pressed), pressed)
                stateList.addState(intArrayOf(), normal)
            }
        }
        return stateList
    }

    /**
     * Разрешение ресурса системного значка в зависимости от активного IconPack
     */
    fun resolveSystemIconRes(iconName: String?): Int {
        val pack = getIconPack()
        if (pack == IconPackType.DARK_GLASS) {
            return when (iconName) {
                "ic_sys_phone", "ic_glass_phone" -> R.drawable.ic_glass_phone
                "ic_sys_messages", "ic_glass_messages", "ic_glass_chat" -> R.drawable.ic_glass_messages
                "ic_sys_radar", "ic_glass_radar", "ic_glass_browser" -> R.drawable.ic_glass_radar
                "ic_sys_camera", "ic_glass_camera" -> R.drawable.ic_glass_camera
                "ic_sys_files", "ic_glass_files" -> R.drawable.ic_glass_files
                "ic_sys_player", "ic_glass_music" -> R.drawable.ic_glass_music
                "ic_sys_settings", "ic_glass_settings" -> R.drawable.ic_glass_settings
                "ic_sys_shield", "ic_glass_shield", "ic_glass_security" -> R.drawable.ic_glass_shield
                "ic_sys_oscilloscope", "ic_glass_network", "ic_glass_cloud", "ic_glass_vpn" -> R.drawable.ic_glass_cloud
                "ic_sys_terminal", "ic_glass_terminal" -> R.drawable.ic_glass_terminal
                "ic_sys_packages", "ic_glass_rocket", "ic_glass_downloader" -> R.drawable.ic_glass_rocket
                "ic_sys_vault", "ic_glass_vault" -> R.drawable.ic_glass_vault
                else -> R.drawable.ic_glass_shield
            }
        }

        return when (iconName) {
            "ic_sys_phone" -> R.drawable.ic_sys_phone
            "ic_sys_messages" -> R.drawable.ic_sys_messages
            "ic_sys_radar" -> R.drawable.ic_sys_radar
            "ic_sys_camera" -> R.drawable.ic_sys_camera
            "ic_sys_files" -> R.drawable.ic_sys_files
            "ic_sys_player" -> R.drawable.ic_sys_player
            "ic_sys_calc" -> R.drawable.ic_sys_calc
            "ic_sys_settings" -> R.drawable.ic_sys_settings
            "ic_sys_shield" -> R.drawable.ic_sys_shield
            "ic_sys_oscilloscope" -> R.drawable.ic_sys_oscilloscope
            "ic_sys_terminal" -> R.drawable.ic_sys_terminal
            "ic_sys_packages" -> R.drawable.ic_sys_packages
            "ic_sys_vault" -> R.drawable.ic_sys_vault
            else -> R.drawable.ic_sys_shield
        }
    }

    /**
     * Применение стилизации к значку и подписи слота
     */
    fun applySlotStyling(iconView: ImageView, labelView: TextView, isSystemOrFolder: Boolean, isAppIcon: Boolean) {
        val pack = getIconPack()
        val dp = context.resources.displayMetrics.density

        // Назначаем динамический фон значка
        iconView.background = createTileDrawable(pack)

        when (pack) {
            IconPackType.DARK_GLASS -> {
                labelView.setTextColor(Color.parseColor("#D4E6FA"))
                labelView.typeface = Typeface.DEFAULT
                if (isSystemOrFolder) {
                    iconView.clearColorFilter()
                    val pad = (10f * dp).toInt()
                    iconView.setPadding(pad, pad, pad, pad)
                } else if (isAppIcon) {
                    iconView.clearColorFilter()
                    val pad = (6f * dp).toInt()
                    iconView.setPadding(pad, pad, pad, pad)
                }
            }

            IconPackType.RETRO_M4200 -> {
                labelView.setTextColor(Color.parseColor("#FFD285"))
                labelView.typeface = Typeface.MONOSPACE
                if (isSystemOrFolder) {
                    iconView.setColorFilter(Color.parseColor("#FFA82E"), PorterDuff.Mode.SRC_IN)
                    val pad = (12f * dp).toInt()
                    iconView.setPadding(pad, pad, pad, pad)
                } else if (isAppIcon) {
                    iconView.clearColorFilter()
                    val pad = (6f * dp).toInt()
                    iconView.setPadding(pad, pad, pad, pad)
                }
            }

            IconPackType.SYSTEM_DEFAULT -> {
                labelView.setTextColor(Color.parseColor("#E2E4EC"))
                labelView.typeface = Typeface.DEFAULT
                if (isSystemOrFolder) {
                    iconView.clearColorFilter()
                    val pad = (12f * dp).toInt()
                    iconView.setPadding(pad, pad, pad, pad)
                } else if (isAppIcon) {
                    iconView.clearColorFilter()
                    iconView.setPadding(0, 0, 0, 0)
                }
            }
        }
    }
}
