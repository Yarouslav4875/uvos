package com.uvos.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-тесты для AppLauncher: валидация белого списка пакетов UVOS/AOSP
 * и защиты от Intent Spoofing (SEC-03).
 */
class AppLauncherSecurityTest {

    @Test
    fun testUvosPackagesWhitelistIntegrity() {
        val uvosPackages = AppLauncher.UVOS_PACKAGES

        // Проверяем наличие всех 15 нативных приложений UVOS + dashboard
        val expectedPackages = listOf(
            "com.uvos.calendar",
            "com.uvos.camera",
            "com.uvos.clock",
            "com.uvos.compass",
            "com.uvos.downloader",
            "com.uvos.files",
            "com.uvos.gallery",
            "com.uvos.mail",
            "com.uvos.messages",
            "com.uvos.music",
            "com.uvos.notes",
            "com.uvos.security",
            "com.uvos.settings",
            "com.uvos.terminal",
            "com.uvos.vpn",
            "com.uvos.dashboard"
        )

        assertEquals(16, uvosPackages.size)
        for (pkg in expectedPackages) {
            assertTrue("Пакет $pkg обязан входить в белый список UVOS", uvosPackages.contains(pkg))
            assertTrue("Пакет $pkg обязан подтверждаться методом isUvosPackage", AppLauncher.isUvosPackage(pkg))
            assertTrue("Пакет $pkg обязан проходить проверку isPackageAllowed", AppLauncher.isPackageAllowed(pkg))
        }
    }

    @Test
    fun testTrustedSystemPackagesWhitelist() {
        val trustedAosp = AppLauncher.TRUSTED_SYSTEM_PACKAGES

        // Проверяем доверенные AOSP / системные вендорные пакеты
        assertTrue(trustedAosp.contains("com.google.android.dialer"))
        assertTrue(trustedAosp.contains("com.android.dialer"))
        assertTrue(trustedAosp.contains("com.android.camera2"))
        assertTrue(trustedAosp.contains("com.google.android.GoogleCamera"))
        assertTrue(trustedAosp.contains("com.android.documentsui"))
        assertTrue(trustedAosp.contains("com.google.android.apps.messaging"))
        assertTrue(trustedAosp.contains("com.android.settings"))
        assertTrue(trustedAosp.contains("org.mozilla.firefox"))
        assertTrue(trustedAosp.contains("org.torproject.torbrowser"))

        // Проверяем предикат isPackageAllowed
        assertTrue(AppLauncher.isPackageAllowed("com.android.settings"))
        assertTrue(AppLauncher.isPackageAllowed("com.google.android.dialer"))
        assertTrue(AppLauncher.isPackageAllowed("com.android.internal.app"))
    }

    @Test
    fun testRejectRogueUntrustedPackages() {
        val roguePackages = listOf(
            "com.evil.malware",
            "org.spyware.tracker",
            "com.fake.dialer.spoof",
            "net.phishing.camera",
            "com.trojan.banker"
        )

        for (pkg in roguePackages) {
            assertFalse("Вредоносный пакет $pkg не должен входить в список UVOS", AppLauncher.isUvosPackage(pkg))
            assertFalse("Вредоносный пакет $pkg не должен проходить валидацию доверенных пакетов", AppLauncher.isPackageAllowed(pkg))
        }
    }
}
