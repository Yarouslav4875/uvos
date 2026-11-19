package com.uvos.vpn.core

data class WireGuardTunnelConfig(
    val interfaceAddress: String = "10.8.0.2",
    val prefixLength: Int = 24,
    val dnsServer: String = "1.1.1.1",
    val mtu: Int = 1420,
    val allowedRoutes: List<String> = listOf("0.0.0.0/0", "::/0"),
    val sessionName: String = "UnderVoltOS Tunnel",
    val isKillSwitchActive: Boolean = true
) {
    companion object {
        fun isValidIpv4(ip: String): Boolean {
            val parts = ip.trim().split(".")
            if (parts.size != 4) return false
            return parts.all { it.toIntOrNull() in 0..255 }
        }
    }

    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (!isValidIpv4(interfaceAddress)) {
            errors.add("Invalid interface address: $interfaceAddress")
        }
        if (prefixLength !in 1..32) {
            errors.add("Prefix length must be between 1 and 32, got: $prefixLength")
        }
        if (!isValidIpv4(dnsServer)) {
            errors.add("Invalid DNS server address: $dnsServer")
        }
        if (mtu !in 1280..1500) {
            errors.add("MTU must be between 1280 and 1500, got: $mtu")
        }
        if (allowedRoutes.isEmpty()) {
            errors.add("Allowed routes cannot be empty")
        }
        return errors
    }

    fun isFullTunnelLeakProtected(): Boolean {
        return allowedRoutes.contains("0.0.0.0/0") && isKillSwitchActive
    }
}
