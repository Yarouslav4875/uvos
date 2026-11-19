package com.uvos.security.data

import com.topjohnwu.superuser.Shell
import com.uvos.security.domain.FirewallRuleBuilder
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
            val cmds = FirewallRuleBuilder.buildInitChainCommands()
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun blockUidNetwork(uid: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = FirewallRuleBuilder.buildBlockUidCommands(uid)
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun unblockUidNetwork(uid: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = FirewallRuleBuilder.buildUnblockUidCommands(uid)
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun dropAllNetwork(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = FirewallRuleBuilder.buildDropAllCommands()
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun flushFirewall(): Boolean = withContext(Dispatchers.IO) {
        try {
            val cmds = FirewallRuleBuilder.buildFlushCommands()
            Shell.cmd(*cmds.toTypedArray()).exec().isSuccess
        } catch (_: Throwable) {
            false
        }
    }
}
