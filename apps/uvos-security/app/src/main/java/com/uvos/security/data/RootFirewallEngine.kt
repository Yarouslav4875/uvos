package com.uvos.security.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RootFirewallEngine {

    suspend fun checkRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.isAppGrantedRoot() == true || Shell.cmd("id").exec().out.any { it.contains("uid=0") }
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun initFirewallChain(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Создаем цепочку UVOS_SHIELD если ее нет
            val cmds = listOf(
                "iptables -N UVOS_SHIELD 2>/dev/null",
                "iptables -C OUTPUT -j UVOS_SHIELD 2>/dev/null || iptables -I OUTPUT 1 -j UVOS_SHIELD",
                "ip6tables -N UVOS_SHIELD 2>/dev/null",
                "ip6tables -C OUTPUT -j UVOS_SHIELD 2>/dev/null || ip6tables -I OUTPUT 1 -j UVOS_SHIELD"
            )
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun blockUidNetwork(uid: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = listOf(
                "iptables -I UVOS_SHIELD 1 -m owner --uid-owner $uid -j DROP",
                "ip6tables -I UVOS_SHIELD 1 -m owner --uid-owner $uid -j DROP",
                "ss -K -p -u -t dst * 2>/dev/null" // Kill active sockets
            )
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun unblockUidNetwork(uid: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = listOf(
                "iptables -D UVOS_SHIELD -m owner --uid-owner $uid -j DROP 2>/dev/null",
                "ip6tables -D UVOS_SHIELD -m owner --uid-owner $uid -j DROP 2>/dev/null"
            )
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun dropAllNetwork(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = listOf(
                "iptables -I UVOS_SHIELD 1 -j DROP",
                "ip6tables -I UVOS_SHIELD 1 -j DROP",
                "ss -K 2>/dev/null"
            )
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun flushFirewall(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = listOf(
                "iptables -F UVOS_SHIELD 2>/dev/null",
                "ip6tables -F UVOS_SHIELD 2>/dev/null"
            )
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }
}
