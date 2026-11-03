package com.uvos.settings.data.datasource

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import com.uvos.settings.domain.model.AppIsolationBadge
import com.uvos.settings.domain.model.AuditedAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppAuditDataSource(
    private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager
    private val blockedUids = mutableSetOf<Int>()

    suspend fun scanInstalledApps(): List<AuditedAppInfo> = withContext(Dispatchers.IO) {
        val installedPackages: List<PackageInfo> = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
            }
        } catch (_: Exception) {
            emptyList()
        }

        installedPackages.mapNotNull { pkgInfo ->
            val appInfo = pkgInfo.applicationInfo ?: return@mapNotNull null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appName = try {
                appInfo.loadLabel(packageManager).toString()
            } catch (_: Exception) {
                pkgInfo.packageName
            }

            val requestedPermissions = pkgInfo.requestedPermissions ?: emptyArray()
            val hasInternet = requestedPermissions.contains(Manifest.permission.INTERNET)
            val hasLocation = requestedPermissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    requestedPermissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
            val hasSensors = requestedPermissions.contains(Manifest.permission.CAMERA) ||
                    requestedPermissions.contains(Manifest.permission.RECORD_AUDIO)

            val uid = appInfo.uid
            val isIptablesBlocked = blockedUids.contains(uid)

            val badge = when {
                isSystem -> AppIsolationBadge.SYSTEM
                !hasInternet || isIptablesBlocked -> AppIsolationBadge.ISOLATED
                hasInternet && (hasLocation || hasSensors) -> AppIsolationBadge.EXPOSED
                else -> AppIsolationBadge.SINGLE_CHANNEL_NET
            }

            var activeChannels = 0
            if (hasInternet && !isIptablesBlocked) activeChannels++
            if (hasLocation) activeChannels++
            if (hasSensors) activeChannels++

            AuditedAppInfo(
                packageName = pkgInfo.packageName,
                appName = appName,
                uid = uid,
                isSystemApp = isSystem,
                hasInternetPermission = hasInternet,
                isIptablesBlocked = isIptablesBlocked,
                isolationBadge = badge,
                activeChannelsCount = activeChannels
            )
        }.sortedWith(
            compareBy<AuditedAppInfo> {
                when (it.isolationBadge) {
                    AppIsolationBadge.EXPOSED -> 0
                    AppIsolationBadge.SINGLE_CHANNEL_NET -> 1
                    AppIsolationBadge.ISOLATED -> 2
                    AppIsolationBadge.SYSTEM -> 3
                }
            }.thenBy { it.appName }
        )
    }

    suspend fun setAppIsolation(packageName: String, isolate: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val uid = appInfo.uid
            if (isolate) {
                blockedUids.add(uid)
                runShellCommand("iptables -I OUTPUT 1 -m owner --uid-owner $uid -j DROP")
                runShellCommand("cmd appops set $packageName INTERNET ignore")
            } else {
                blockedUids.remove(uid)
                runShellCommand("iptables -D OUTPUT -m owner --uid-owner $uid -j DROP")
                runShellCommand("cmd appops set $packageName INTERNET allow")
            }
        }
    }

    suspend fun setAppNetworkAccess(packageName: String, allowNetwork: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val uid = appInfo.uid
            if (!allowNetwork) {
                blockedUids.add(uid)
                runShellCommand("iptables -I OUTPUT 1 -m owner --uid-owner $uid -j DROP")
            } else {
                blockedUids.remove(uid)
                runShellCommand("iptables -D OUTPUT -m owner --uid-owner $uid -j DROP")
            }
        }
    }

    suspend fun terminateApp(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.killBackgroundProcesses(packageName)
            runShellCommand("am force-stop $packageName")
        }
    }

    private fun runShellCommand(cmd: String) {
        try {
            if (Shell.isAppGrantedRoot() == true) {
                Shell.cmd(cmd).exec()
            }
        } catch (_: Exception) {}
    }
}
