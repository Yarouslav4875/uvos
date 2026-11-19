package com.uvos.files.data.engine

object RootCommandBuilder {

    fun escapeShellArg(arg: String): String {
        return "'" + arg.replace("'", "'\\''") + "'"
    }

    fun buildLsCommand(path: String): String {
        val normalized = if (path.endsWith("/")) path else "$path/"
        return "ls -la ${escapeShellArg(normalized)}"
    }

    fun buildCatCommand(path: String): String {
        return "cat ${escapeShellArg(path)}"
    }

    fun buildRmCommand(path: String): String {
        return "rm -rf ${escapeShellArg(path)}"
    }

    fun buildMvCommand(from: String, to: String): String {
        return "mv ${escapeShellArg(from)} ${escapeShellArg(to)}"
    }

    fun buildCpCommand(from: String, to: String): String {
        return "cp -a ${escapeShellArg(from)} ${escapeShellArg(to)}"
    }

    fun buildChmodCommand(path: String, octal: String): String {
        return "chmod $octal ${escapeShellArg(path)}"
    }

    fun buildRemountCommands(path: String): List<String> {
        return when {
            path.startsWith("/system") -> listOf("mount -o remount,rw /system", "mount -o remount,rw /")
            path.startsWith("/vendor") -> listOf("mount -o remount,rw /vendor", "mount -o remount,rw /")
            path.startsWith("/apex") -> listOf("mount -o remount,rw /apex")
            path == "/" -> listOf("mount -o remount,rw /")
            else -> emptyList()
        }
    }
}
