package com.uvos.files

import com.uvos.files.data.engine.RootCommandBuilder
import com.uvos.files.domain.model.PosixPermissions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilesAndRootCommandsTest {

    @Test
    fun testShellArgumentEscaping() {
        assertEquals("'file.txt'", RootCommandBuilder.escapeShellArg("file.txt"))
        assertEquals("'file with spaces.txt'", RootCommandBuilder.escapeShellArg("file with spaces.txt"))
        // Command injection prevention test
        assertEquals("'file; rm -rf /'", RootCommandBuilder.escapeShellArg("file; rm -rf /"))
        assertEquals("'it'\\''s complicated'", RootCommandBuilder.escapeShellArg("it's complicated"))
    }

    @Test
    fun testCommandGeneration() {
        assertEquals("ls -la '/system/'", RootCommandBuilder.buildLsCommand("/system"))
        assertEquals("ls -la '/system/'", RootCommandBuilder.buildLsCommand("/system/"))
        assertEquals("rm -rf '/data/local/tmp/test'", RootCommandBuilder.buildRmCommand("/data/local/tmp/test"))
        assertEquals("mv '/source/a' '/dest/b'", RootCommandBuilder.buildMvCommand("/source/a", "/dest/b"))
        assertEquals("chmod 755 '/system/bin/uvosd'", RootCommandBuilder.buildChmodCommand("/system/bin/uvosd", "755"))
    }

    @Test
    fun testRemountCommandGeneration() {
        val systemRemount = RootCommandBuilder.buildRemountCommands("/system/app")
        assertEquals(2, systemRemount.size)
        assertTrue(systemRemount[0].contains("mount -o remount,rw /system"))

        val vendorRemount = RootCommandBuilder.buildRemountCommands("/vendor/etc")
        assertEquals(2, vendorRemount.size)
        assertTrue(vendorRemount[0].contains("mount -o remount,rw /vendor"))

        val sdcardRemount = RootCommandBuilder.buildRemountCommands("/sdcard/Download")
        assertTrue(sdcardRemount.isEmpty())
    }

    @Test
    fun testPosixPermissionsFromOctal() {
        val perm755 = PosixPermissions.fromOctal("755")
        assertTrue(perm755.userRead)
        assertTrue(perm755.userWrite)
        assertTrue(perm755.userExecute)
        assertTrue(perm755.groupRead)
        assertFalse(perm755.groupWrite)
        assertTrue(perm755.groupExecute)
        assertTrue(perm755.otherRead)
        assertFalse(perm755.otherWrite)
        assertTrue(perm755.otherExecute)
        assertEquals("rwxr-xr-x", perm755.toFormattedString())

        val perm644 = PosixPermissions.fromOctal("644")
        assertEquals("rw-r--r--", perm644.toFormattedString())

        val perm700 = PosixPermissions.fromOctal("700")
        assertEquals("rwx------", perm700.toFormattedString())
    }

    @Test
    fun testPosixPermissionsFromRwx() {
        val perm = PosixPermissions.fromRwx("drwxr-xr-x")
        assertEquals("755", perm.octalString)
        assertEquals("rwxr-xr-x", perm.toFormattedString())

        val permFile = PosixPermissions.fromRwx("-rw-r--r--")
        assertEquals("644", permFile.octalString)
        assertEquals("rw-r--r--", permFile.toFormattedString())
    }
}
