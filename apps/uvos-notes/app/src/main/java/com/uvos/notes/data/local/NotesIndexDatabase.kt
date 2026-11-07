package com.uvos.notes.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uvos.notes.domain.model.Note

class NotesIndexDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "uvos_notes_fts.db"
        private const val DB_VERSION = 1
        private const val TABLE_FTS = "notes_fts"

        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_CONTENT = "content"
        private const val COL_FOLDER = "folder"
        private const val COL_FILE_PATH = "file_path"
        private const val COL_IS_ENCRYPTED = "is_encrypted"
        private const val COL_MODIFIED_AT = "modified_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts4(
                $COL_ID,
                $COL_TITLE,
                $COL_CONTENT,
                $COL_FOLDER,
                $COL_FILE_PATH,
                $COL_IS_ENCRYPTED,
                $COL_MODIFIED_AT
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FTS")
        onCreate(db)
    }

    fun indexNote(note: Note) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Delete existing
            db.delete(TABLE_FTS, "$COL_ID = ?", arrayOf(note.id))

            val values = ContentValues().apply {
                put(COL_ID, note.id)
                put(COL_TITLE, note.title)
                // For encrypted notes, do not leak plain text into the searchable index
                put(COL_CONTENT, if (note.isEncrypted) "[Зашифровано AES-256-GCM]" else note.content)
                put(COL_FOLDER, note.folder)
                put(COL_FILE_PATH, note.filePath)
                put(COL_IS_ENCRYPTED, if (note.isEncrypted) 1 else 0)
                put(COL_MODIFIED_AT, note.modifiedAt)
            }
            db.insert(TABLE_FTS, null, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteNote(id: String) {
        writableDatabase.delete(TABLE_FTS, "$COL_ID = ?", arrayOf(id))
    }

    fun rebuildIndex(notes: List<Note>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FTS, null, null)
            for (note in notes) {
                val values = ContentValues().apply {
                    put(COL_ID, note.id)
                    put(COL_TITLE, note.title)
                    put(COL_CONTENT, if (note.isEncrypted) "[Зашифровано AES-256-GCM]" else note.content)
                    put(COL_FOLDER, note.folder)
                    put(COL_FILE_PATH, note.filePath)
                    put(COL_IS_ENCRYPTED, if (note.isEncrypted) 1 else 0)
                    put(COL_MODIFIED_AT, note.modifiedAt)
                }
                db.insert(TABLE_FTS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun searchNotes(query: String): List<Note> {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return emptyList()

        val results = mutableListOf<Note>()
        val db = readableDatabase

        // FTS match syntax: token* for prefix matching
        val ftsQuery = cleaned.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }

        try {
            val cursor = db.rawQuery(
                """
                SELECT $COL_ID, $COL_TITLE, $COL_CONTENT, $COL_FOLDER, $COL_FILE_PATH, $COL_IS_ENCRYPTED, $COL_MODIFIED_AT,
                       snippet($TABLE_FTS, '<b>', '</b>', '...', -1, 20) AS match_snippet
                FROM $TABLE_FTS
                WHERE $TABLE_FTS MATCH ?
                ORDER BY $COL_MODIFIED_AT DESC
                """.trimIndent(),
                arrayOf(ftsQuery)
            )

            cursor.use {
                val idIdx = it.getColumnIndexOrThrow(COL_ID)
                val titleIdx = it.getColumnIndexOrThrow(COL_TITLE)
                val contentIdx = it.getColumnIndexOrThrow(COL_CONTENT)
                val folderIdx = it.getColumnIndexOrThrow(COL_FOLDER)
                val pathIdx = it.getColumnIndexOrThrow(COL_FILE_PATH)
                val encIdx = it.getColumnIndexOrThrow(COL_IS_ENCRYPTED)
                val modIdx = it.getColumnIndexOrThrow(COL_MODIFIED_AT)
                val snipIdx = it.getColumnIndexOrThrow("match_snippet")

                while (it.moveToNext()) {
                    results.add(
                        Note(
                            id = it.getString(idIdx),
                            title = it.getString(titleIdx),
                            content = it.getString(contentIdx),
                            folder = it.getString(folderIdx),
                            filePath = it.getString(pathIdx),
                            isEncrypted = it.getInt(encIdx) == 1,
                            modifiedAt = it.getLong(modIdx),
                            snippet = it.getString(snipIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to LIKE query if FTS expression encounters syntax issues
            val cursor = db.rawQuery(
                """
                SELECT $COL_ID, $COL_TITLE, $COL_CONTENT, $COL_FOLDER, $COL_FILE_PATH, $COL_IS_ENCRYPTED, $COL_MODIFIED_AT
                FROM $TABLE_FTS
                WHERE $COL_TITLE LIKE ? OR $COL_CONTENT LIKE ?
                ORDER BY $COL_MODIFIED_AT DESC
                """.trimIndent(),
                arrayOf("%$cleaned%", "%$cleaned%")
            )
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow(COL_ID)
                val titleIdx = it.getColumnIndexOrThrow(COL_TITLE)
                val contentIdx = it.getColumnIndexOrThrow(COL_CONTENT)
                val folderIdx = it.getColumnIndexOrThrow(COL_FOLDER)
                val pathIdx = it.getColumnIndexOrThrow(COL_FILE_PATH)
                val encIdx = it.getColumnIndexOrThrow(COL_IS_ENCRYPTED)
                val modIdx = it.getColumnIndexOrThrow(COL_MODIFIED_AT)

                while (it.moveToNext()) {
                    results.add(
                        Note(
                            id = it.getString(idIdx),
                            title = it.getString(titleIdx),
                            content = it.getString(contentIdx),
                            folder = it.getString(folderIdx),
                            filePath = it.getString(pathIdx),
                            isEncrypted = it.getInt(encIdx) == 1,
                            modifiedAt = it.getLong(modIdx),
                            snippet = null
                        )
                    )
                }
            }
        }

        return results
    }
}
