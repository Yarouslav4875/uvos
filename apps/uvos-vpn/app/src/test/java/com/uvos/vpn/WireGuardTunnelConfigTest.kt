package com.uvos.vpn

import com.uvos.vpn.core.WireGuardTunnelConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WireGuardTunnelConfigTest {

    @Test
    fun testDefaultConfigIsValid() {
        val config = WireGuardTunnelConfig()
        val errors = config.validate()
        assertTrue("Default config should have no validation errors, got: $errors", errors.isEmpty())
        assertTrue("Default config should be leak-protected", config.isFullTunnelLeakProtected())
    }

    @Test
    fun testInvalidIpAddressValidation() {
        val config = WireGuardTunnelConfig(interfaceAddress = "999.999.999.999")
        val errors = config.validate()
        assertEquals(1, errors.size)
        assertTrue(errors[0].contains("Invalid interface address"))
    }

    @Test
    fun testInvalidMtuValidation() {
        val configLowMtu = WireGuardTunnelConfig(mtu = 1000)
        assertTrue(configLowMtu.validate().any { it.contains("MTU") })

        val configHighMtu = WireGuardTunnelConfig(mtu = 1600)
        assertTrue(configHighMtu.validate().any { it.contains("MTU") })
    }

    @Test
    fun testKillSwitchLeakProtection() {
        val secured = WireGuardTunnelConfig(
            allowedRoutes = listOf("0.0.0.0/0"),
            isKillSwitchActive = true
        )
        assertTrue(secured.isFullTunnelLeakProtected())

        val splitTunnel = WireGuardTunnelConfig(
            allowedRoutes = listOf("10.0.0.0/8"),
            isKillSwitchActive = true
        )
        assertFalse(splitTunnel.isFullTunnelLeakProtected())

        val unkilled = WireGuardTunnelConfig(
            allowedRoutes = listOf("0.0.0.0/0"),
            isKillSwitchActive = false
        )
        assertFalse(unkilled.isFullTunnelLeakProtected())
    }

    @Test
    fun testIpv4Helper() {
        assertTrue(WireGuardTunnelConfig.isValidIpv4("10.8.0.2"))
        assertTrue(WireGuardTunnelConfig.isValidIpv4("1.1.1.1"))
        assertTrue(WireGuardTunnelConfig.isValidIpv4("0.0.0.0"))
        assertTrue(WireGuardTunnelConfig.isValidIpv4("255.255.255.255"))

        assertFalse(WireGuardTunnelConfig.isValidIpv4("256.0.0.1"))
        assertFalse(WireGuardTunnelConfig.isValidIpv4("1.2.3"))
        assertFalse(WireGuardTunnelConfig.isValidIpv4("abc.def.ghi.jkl"))
    }
}
