package com.uvos.security.domain

object FirewallRuleBuilder {
    const val CHAIN_NAME = "UVOS_SHIELD"

    fun buildInitChainCommands(): List<String> = listOf(
        "iptables -N $CHAIN_NAME 2>/dev/null",
        "iptables -C OUTPUT -j $CHAIN_NAME 2>/dev/null || iptables -I OUTPUT 1 -j $CHAIN_NAME",
        "ip6tables -N $CHAIN_NAME 2>/dev/null",
        "ip6tables -C OUTPUT -j $CHAIN_NAME 2>/dev/null || ip6tables -I OUTPUT 1 -j $CHAIN_NAME"
    )

    fun buildBlockUidCommands(uid: Int): List<String> {
        require(uid >= 0) { "UID must be non-negative, got: $uid" }
        return listOf(
            "iptables -I $CHAIN_NAME 1 -m owner --uid-owner $uid -j DROP",
            "ip6tables -I $CHAIN_NAME 1 -m owner --uid-owner $uid -j DROP",
            "ss -K -p -u -t dst * 2>/dev/null"
        )
    }

    fun buildUnblockUidCommands(uid: Int): List<String> {
        require(uid >= 0) { "UID must be non-negative, got: $uid" }
        return listOf(
            "iptables -D $CHAIN_NAME -m owner --uid-owner $uid -j DROP 2>/dev/null",
            "ip6tables -D $CHAIN_NAME -m owner --uid-owner $uid -j DROP 2>/dev/null"
        )
    }

    fun buildDropAllCommands(): List<String> = listOf(
        "iptables -I $CHAIN_NAME 1 -j DROP",
        "ip6tables -I $CHAIN_NAME 1 -j DROP",
        "ss -K 2>/dev/null"
    )

    fun buildFlushCommands(): List<String> = listOf(
        "iptables -F $CHAIN_NAME 2>/dev/null",
        "ip6tables -F $CHAIN_NAME 2>/dev/null"
    )
}
