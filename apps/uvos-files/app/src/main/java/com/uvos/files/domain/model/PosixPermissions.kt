package com.uvos.files.domain.model

data class PosixPermissions(
    val userRead: Boolean,
    val userWrite: Boolean,
    val userExecute: Boolean,
    val groupRead: Boolean,
    val groupWrite: Boolean,
    val groupExecute: Boolean,
    val otherRead: Boolean,
    val otherWrite: Boolean,
    val otherExecute: Boolean,
    val octalString: String
) {
    fun toFormattedString(): String {
        val sb = StringBuilder()
        sb.append(if (userRead) 'r' else '-')
        sb.append(if (userWrite) 'w' else '-')
        sb.append(if (userExecute) 'x' else '-')
        sb.append(if (groupRead) 'r' else '-')
        sb.append(if (groupWrite) 'w' else '-')
        sb.append(if (groupExecute) 'x' else '-')
        sb.append(if (otherRead) 'r' else '-')
        sb.append(if (otherWrite) 'w' else '-')
        sb.append(if (otherExecute) 'x' else '-')
        return sb.toString()
    }

    companion object {
        fun fromOctal(octal: String): PosixPermissions {
            val clean = octal.trim().takeLast(3).padStart(3, '0')
            val u = clean[0].digitToIntOrNull() ?: 0
            val g = clean[1].digitToIntOrNull() ?: 0
            val o = clean[2].digitToIntOrNull() ?: 0

            return PosixPermissions(
                userRead = (u and 4) != 0,
                userWrite = (u and 2) != 0,
                userExecute = (u and 1) != 0,
                groupRead = (g and 4) != 0,
                groupWrite = (g and 2) != 0,
                groupExecute = (g and 1) != 0,
                otherRead = (o and 4) != 0,
                otherWrite = (o and 2) != 0,
                otherExecute = (o and 1) != 0,
                octalString = clean
            )
        }

        fun fromRwx(rwx: String): PosixPermissions {
            val s = rwx.takeLast(9).padEnd(9, '-')
            val ur = s[0] == 'r'
            val uw = s[1] == 'w'
            val ux = s[2] == 'x' || s[2] == 's'
            val gr = s[3] == 'r'
            val gw = s[4] == 'w'
            val gx = s[5] == 'x' || s[5] == 's'
            val or = s[6] == 'r'
            val ow = s[7] == 'w'
            val ox = s[8] == 'x' || s[8] == 't'

            val u = (if (ur) 4 else 0) + (if (uw) 2 else 0) + (if (ux) 1 else 0)
            val g = (if (gr) 4 else 0) + (if (gw) 2 else 0) + (if (gx) 1 else 0)
            val o = (if (or) 4 else 0) + (if (ow) 2 else 0) + (if (ox) 1 else 0)

            return PosixPermissions(
                userRead = ur,
                userWrite = uw,
                userExecute = ux,
                groupRead = gr,
                groupWrite = gw,
                groupExecute = gx,
                otherRead = or,
                otherWrite = ow,
                otherExecute = ox,
                octalString = "$u$g$o"
            )
        }
    }
}
