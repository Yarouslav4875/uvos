package com.uvos.downloader.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uvos.downloader.data.local.entity.DownloadChunkEntity
import com.uvos.downloader.data.local.entity.DownloadTaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DownloadDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    private val dbScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val taskChangeNotifier = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    private val chunkChangeNotifier = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val writeMutex = Mutex()

    init {
        setWriteAheadLoggingEnabled(true)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS download_tasks (
                id TEXT PRIMARY KEY NOT NULL,
                url TEXT NOT NULL,
                destinationPath TEXT NOT NULL,
                fileName TEXT NOT NULL,
                totalBytes INTEGER NOT NULL,
                downloadedBytes INTEGER NOT NULL,
                status TEXT NOT NULL,
                numThreads INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                onlyWifi INTEGER NOT NULL,
                errorMessage TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS download_chunks (
                chunkId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                taskId TEXT NOT NULL,
                chunkIndex INTEGER NOT NULL,
                startByte INTEGER NOT NULL,
                endByte INTEGER NOT NULL,
                currentByte INTEGER NOT NULL,
                FOREIGN KEY(taskId) REFERENCES download_tasks(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chunks_task ON download_chunks(taskId)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS download_chunks")
        db.execSQL("DROP TABLE IF EXISTS download_tasks")
        onCreate(db)
    }

    fun notifyTasksChanged() {
        taskChangeNotifier.tryEmit(Unit)
    }

    fun notifyChunksChanged(taskId: String) {
        chunkChangeNotifier.tryEmit(taskId)
    }

    val downloadTaskDao: DownloadTaskDao = object : DownloadTaskDao {
        override fun getAllTasks(): Flow<List<DownloadTaskEntity>> = flow {
            emit(queryAll())
            taskChangeNotifier.collect {
                emit(queryAll())
            }
        }.flowOn(Dispatchers.IO).distinctUntilChanged()

        override fun getTasksByStatus(statuses: List<String>): Flow<List<DownloadTaskEntity>> = flow {
            emit(queryByStatus(statuses))
            taskChangeNotifier.collect {
                emit(queryByStatus(statuses))
            }
        }.flowOn(Dispatchers.IO).distinctUntilChanged()

        override fun getTaskById(id: String): DownloadTaskEntity? {
            val db = readableDatabase
            val cursor = db.query(
                "download_tasks",
                null,
                "id = ?",
                arrayOf(id),
                null,
                null,
                null
            )
            return cursor.use {
                if (it.moveToFirst()) it.toTaskEntity() else null
            }
        }

        override fun insert(task: DownloadTaskEntity) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("id", task.id)
                put("url", task.url)
                put("destinationPath", task.destinationPath)
                put("fileName", task.fileName)
                put("totalBytes", task.totalBytes)
                put("downloadedBytes", task.downloadedBytes)
                put("status", task.status)
                put("numThreads", task.numThreads)
                put("createdAt", task.createdAt)
                put("onlyWifi", if (task.onlyWifi) 1 else 0)
                put("errorMessage", task.errorMessage)
            }
            db.insertWithOnConflict(
                "download_tasks",
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            notifyTasksChanged()
        }

        override fun update(task: DownloadTaskEntity) {
            insert(task)
        }

        override fun updateProgress(id: String, downloadedBytes: Long, status: String) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("downloadedBytes", downloadedBytes)
                put("status", status)
            }
            db.update("download_tasks", values, "id = ?", arrayOf(id))
            notifyTasksChanged()
        }

        override fun updateStatus(id: String, status: String, errorMessage: String?) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("status", status)
                put("errorMessage", errorMessage)
            }
            db.update("download_tasks", values, "id = ?", arrayOf(id))
            notifyTasksChanged()
        }

        override fun updateMetadata(id: String, totalBytes: Long, fileName: String, destinationPath: String) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("totalBytes", totalBytes)
                put("fileName", fileName)
                put("destinationPath", destinationPath)
            }
            db.update("download_tasks", values, "id = ?", arrayOf(id))
            notifyTasksChanged()
        }

        override fun deleteById(id: String) {
            val db = writableDatabase
            db.delete("download_chunks", "taskId = ?", arrayOf(id))
            db.delete("download_tasks", "id = ?", arrayOf(id))
            notifyTasksChanged()
            notifyChunksChanged(id)
        }

        private fun queryAll(): List<DownloadTaskEntity> {
            val list = mutableListOf<DownloadTaskEntity>()
            val db = readableDatabase
            val cursor = db.query(
                "download_tasks",
                null,
                null,
                null,
                null,
                null,
                "createdAt DESC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(it.toTaskEntity())
                }
            }
            return list
        }

        private fun queryByStatus(statuses: List<String>): List<DownloadTaskEntity> {
            val list = mutableListOf<DownloadTaskEntity>()
            val db = readableDatabase
            val placeholders = statuses.joinToString(",") { "?" }
            val cursor = db.query(
                "download_tasks",
                null,
                "status IN ($placeholders)",
                statuses.toTypedArray(),
                null,
                null,
                "createdAt DESC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(it.toTaskEntity())
                }
            }
            return list
        }
    }

    val downloadChunkDao: DownloadChunkDao = object : DownloadChunkDao {
        override fun getChunksForTask(taskId: String): List<DownloadChunkEntity> {
            val list = mutableListOf<DownloadChunkEntity>()
            val db = readableDatabase
            val cursor = db.query(
                "download_chunks",
                null,
                "taskId = ?",
                arrayOf(taskId),
                null,
                null,
                "chunkIndex ASC"
            )
            cursor.use {
                while (it.moveToNext()) {
                    list.add(it.toChunkEntity())
                }
            }
            return list
        }

        override fun observeChunksForTask(taskId: String): Flow<List<DownloadChunkEntity>> = flow {
            emit(getChunksForTask(taskId))
            chunkChangeNotifier.collect { changedTaskId ->
                if (changedTaskId == taskId) {
                    emit(getChunksForTask(taskId))
                }
            }
        }.flowOn(Dispatchers.IO)

        override fun insertChunks(chunks: List<DownloadChunkEntity>) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (chunk in chunks) {
                    val values = ContentValues().apply {
                        if (chunk.chunkId > 0) {
                            put("chunkId", chunk.chunkId)
                        }
                        put("taskId", chunk.taskId)
                        put("chunkIndex", chunk.chunkIndex)
                        put("startByte", chunk.startByte)
                        put("endByte", chunk.endByte)
                        put("currentByte", chunk.currentByte)
                    }
                    db.insertWithOnConflict(
                        "download_chunks",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            if (chunks.isNotEmpty()) {
                notifyChunksChanged(chunks.first().taskId)
            }
        }

        override fun updateProgress(chunkId: Long, currentByte: Long) {
            val db = writableDatabase
            val values = ContentValues().apply {
                put("currentByte", currentByte)
            }
            db.update("download_chunks", values, "chunkId = ?", arrayOf(chunkId.toString()))
        }

        override fun deleteChunksForTask(taskId: String) {
            val db = writableDatabase
            db.delete("download_chunks", "taskId = ?", arrayOf(taskId))
            notifyChunksChanged(taskId)
        }
    }

    companion object {
        private const val DB_NAME = "uvos_downloader.db"
        private const val DB_VERSION = 1

        @Volatile
        private var INSTANCE: DownloadDatabase? = null

        fun getInstance(context: Context): DownloadDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

private fun Cursor.toTaskEntity(): DownloadTaskEntity {
    return DownloadTaskEntity(
        id = getString(getColumnIndexOrThrow("id")),
        url = getString(getColumnIndexOrThrow("url")),
        destinationPath = getString(getColumnIndexOrThrow("destinationPath")),
        fileName = getString(getColumnIndexOrThrow("fileName")),
        totalBytes = getLong(getColumnIndexOrThrow("totalBytes")),
        downloadedBytes = getLong(getColumnIndexOrThrow("downloadedBytes")),
        status = getString(getColumnIndexOrThrow("status")),
        numThreads = getInt(getColumnIndexOrThrow("numThreads")),
        createdAt = getLong(getColumnIndexOrThrow("createdAt")),
        onlyWifi = getInt(getColumnIndexOrThrow("onlyWifi")) == 1,
        errorMessage = getString(getColumnIndexOrThrow("errorMessage"))
    )
}

private fun Cursor.toChunkEntity(): DownloadChunkEntity {
    return DownloadChunkEntity(
        chunkId = getLong(getColumnIndexOrThrow("chunkId")),
        taskId = getString(getColumnIndexOrThrow("taskId")),
        chunkIndex = getInt(getColumnIndexOrThrow("chunkIndex")),
        startByte = getLong(getColumnIndexOrThrow("startByte")),
        endByte = getLong(getColumnIndexOrThrow("endByte")),
        currentByte = getLong(getColumnIndexOrThrow("currentByte"))
    )
}
