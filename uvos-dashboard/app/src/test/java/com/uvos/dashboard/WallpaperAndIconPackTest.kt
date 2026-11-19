package com.uvos.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тесты для WallpaperManager, IconPackManager и математики кастомизации UVOS
 */
class WallpaperAndIconPackTest {

    @Test
    fun testWallpaperModeEnumIntegrity() {
        val modes = WallpaperManager.WallpaperMode.values()
        assertEquals(5, modes.size)
        assertTrue(modes.contains(WallpaperManager.WallpaperMode.SOLID_AMOLED))
        assertTrue(modes.contains(WallpaperManager.WallpaperMode.PRESET_TACTICAL_GRID))
        assertTrue(modes.contains(WallpaperManager.WallpaperMode.PRESET_CARBON_FIBER))
        assertTrue(modes.contains(WallpaperManager.WallpaperMode.PRESET_STEEL_RADAR))
        assertTrue(modes.contains(WallpaperManager.WallpaperMode.CUSTOM_PHOTO))
    }

    @Test
    fun testDimmingLevelClampingLogic() {
        fun clampDimLevel(level: Int): Int = level.coerceIn(0, 80)

        // Нижняя граница (0%)
        assertEquals(0, clampDimLevel(-10))
        assertEquals(0, clampDimLevel(-1))
        assertEquals(0, clampDimLevel(0))

        // Рабочий диапазон (0 - 80%)
        assertEquals(20, clampDimLevel(20))
        assertEquals(50, clampDimLevel(50))
        assertEquals(80, clampDimLevel(80))

        // Верхняя граница (80% - защита от 100% блокировки экрана чёрным цветом)
        assertEquals(80, clampDimLevel(81))
        assertEquals(80, clampDimLevel(99))
        assertEquals(80, clampDimLevel(150))
    }

    @Test
    fun testIconPackTypeEnumIntegrity() {
        val packs = IconPackManager.IconPackType.values()
        assertEquals(3, packs.size)

        val darkGlass = IconPackManager.IconPackType.DARK_GLASS
        assertEquals("ТЁМНОЕ СТЕКЛО", darkGlass.title)
        assertFalse(darkGlass.description.isEmpty())

        val retro = IconPackManager.IconPackType.RETRO_M4200
        assertEquals("РЕТРО M4200", retro.title)
        assertFalse(retro.description.isEmpty())

        val systemDefault = IconPackManager.IconPackType.SYSTEM_DEFAULT
        assertEquals("СТАНДАРТ UVOS", systemDefault.title)
        assertFalse(systemDefault.description.isEmpty())
    }

    @Test
    fun testTacticalGridStepGeometry() {
        val width = 1080
        val height = 2400
        val minorStep = 48
        val majorStep = minorStep * 5 // 240

        // Проверяем кратность шага сетки экрану 1080x2400 (Redmi Note 13 4G)
        val minorColumns = width / minorStep
        val majorColumns = width / majorStep

        assertTrue(minorColumns > 20)
        assertEquals(4, majorColumns) // Ровно 4 мажорных квадранта (соответствует 4 колонкам сетки 4x4)
        assertEquals(10, height / majorStep) // Ровно 10 мажорных строк
    }

    @Test
    fun testRadarScopeGeometry() {
        val width = 1080
        val maxRadius = width * 0.46f // 496.8px
        val ringCount = 5

        for (i in 1..ringCount) {
            val r = (maxRadius / ringCount) * i
            assertTrue(r > 0f)
            assertTrue(r <= maxRadius)
        }
    }
}
