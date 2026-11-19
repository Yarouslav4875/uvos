package com.uvos.security.domain

import com.uvos.security.domain.model.AppIsolationInfo

object PackageSecurityValidator {
    private val PACKAGE_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$".toRegex()

    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isBlank() || packageName.length > 255) return false
        return PACKAGE_REGEX.matches(packageName)
    }

    fun isSystemUid(uid: Int): Boolean {
        return uid in 0..9999
    }

    fun isApplicationUid(uid: Int): Boolean {
        return uid >= 10000
    }

    fun evaluateAppRiskScore(info: AppIsolationInfo): Float {
        var score = 0.0f
        if (!info.isNetworkBlocked) score += 0.4f
        if (!info.isSensorsSpoofed) score += 0.3f
        if (!info.isContactsStubbed) score += 0.3f
        return score.coerceIn(0.0f, 1.0f)
    }
}
