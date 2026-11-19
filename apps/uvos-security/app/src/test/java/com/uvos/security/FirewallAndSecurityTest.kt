package com.uvos.security

import com.uvos.security.domain.FirewallRuleBuilder
import com.uvos.security.domain.PackageSecurityValidator
import com.uvos.security.domain.model.AppIsolationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirewallAndSecurityTest {

    @Test
    fun testInitChainCommandsStructure() {
        val cmds = FirewallRuleBuilder.buildInitChainCommands()
        assertEquals(4, cmds.size)
        assertTrue(cmds[0].contains("iptables -N UVOS_SHIELD"))
        assertTrue(cmds[1].contains("iptables -C OUTPUT -j UVOS_SHIELD"))
        assertTrue(cmds[2].contains("ip6tables -N UVOS_SHIELD"))
        assertTrue(cmds[3].contains("ip6tables -C OUTPUT -j UVOS_SHIELD"))
    }

    @Test
    fun testBlockUidCommandsGeneratesOwnerRule() {
        val uid = 10142
        val cmds = FirewallRuleBuilder.buildBlockUidCommands(uid)
        assertEquals(3, cmds.size)
        assertTrue(cmds[0].contains("iptables -I UVOS_SHIELD 1 -m owner --uid-owner 10142 -j DROP"))
        assertTrue(cmds[1].contains("ip6tables -I UVOS_SHIELD 1 -m owner --uid-owner 10142 -j DROP"))
        assertTrue(cmds[2].contains("ss -K"))
    }

    @Test
    fun testUnblockUidCommandsDeletesOwnerRule() {
        val uid = 10142
        val cmds = FirewallRuleBuilder.buildUnblockUidCommands(uid)
        assertEquals(2, cmds.size)
        assertTrue(cmds[0].contains("iptables -D UVOS_SHIELD -m owner --uid-owner 10142 -j DROP"))
        assertTrue(cmds[1].contains("ip6tables -D UVOS_SHIELD -m owner --uid-owner 10142 -j DROP"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeUidThrowsException() {
        FirewallRuleBuilder.buildBlockUidCommands(-1)
    }

    @Test
    fun testPackageNameValidation() {
        assertTrue(PackageSecurityValidator.isValidPackageName("com.uvos.security"))
        assertTrue(PackageSecurityValidator.isValidPackageName("org.telegram.messenger"))
        assertTrue(PackageSecurityValidator.isValidPackageName("com.android.settings"))

        assertFalse(PackageSecurityValidator.isValidPackageName(""))
        assertFalse(PackageSecurityValidator.isValidPackageName("invalid_single_word"))
        assertFalse(PackageSecurityValidator.isValidPackageName("123.numeric.start"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.uvos..emptysegment"))
        assertFalse(PackageSecurityValidator.isValidPackageName("com.uvos.bad-char"))
    }

    @Test
    fun testUidCategorization() {
        assertTrue(PackageSecurityValidator.isSystemUid(0)) // root
        assertTrue(PackageSecurityValidator.isSystemUid(1000)) // system
        assertFalse(PackageSecurityValidator.isSystemUid(10001))

        assertTrue(PackageSecurityValidator.isApplicationUid(10001))
        assertFalse(PackageSecurityValidator.isApplicationUid(1000))
    }

    @Test
    fun testAppRiskScoring() {
        val fullyProtected = AppIsolationInfo(
            packageName = "com.untrusted.app",
            appName = "Untrusted",
            uid = 10200,
            isNetworkBlocked = true,
            isSensorsSpoofed = true,
            isContactsStubbed = true
        )
        assertEquals(0.0f, PackageSecurityValidator.evaluateAppRiskScore(fullyProtected), 0.001f)

        val unisolated = AppIsolationInfo(
            packageName = "com.untrusted.app",
            appName = "Untrusted",
            uid = 10200,
            isNetworkBlocked = false,
            isSensorsSpoofed = false,
            isContactsStubbed = false
        )
        assertEquals(1.0f, PackageSecurityValidator.evaluateAppRiskScore(unisolated), 0.001f)
    }
}
