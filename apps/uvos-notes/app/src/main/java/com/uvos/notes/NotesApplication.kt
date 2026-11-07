package com.uvos.notes

import android.app.Application
import com.uvos.notes.data.local.CryptoManager
import com.uvos.notes.data.local.NoteFileManager
import com.uvos.notes.data.local.NotesIndexDatabase
import com.uvos.notes.data.repository.NotesRepositoryImpl
import com.uvos.notes.domain.usecase.EncryptNoteUseCase
import com.uvos.notes.domain.usecase.SaveNoteUseCase
import com.uvos.notes.domain.usecase.SearchNotesUseCase

class NotesApplication : Application() {

    lateinit var cryptoManager: CryptoManager
        private set

    lateinit var noteFileManager: NoteFileManager
        private set

    lateinit var notesIndexDatabase: NotesIndexDatabase
        private set

    lateinit var repository: NotesRepositoryImpl
        private set

    lateinit var saveNoteUseCase: SaveNoteUseCase
        private set

    lateinit var encryptNoteUseCase: EncryptNoteUseCase
        private set

    lateinit var searchNotesUseCase: SearchNotesUseCase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        cryptoManager = CryptoManager()
        noteFileManager = NoteFileManager(this, cryptoManager)
        notesIndexDatabase = NotesIndexDatabase(this)
        repository = NotesRepositoryImpl(noteFileManager, notesIndexDatabase, cryptoManager)

        saveNoteUseCase = SaveNoteUseCase(repository)
        encryptNoteUseCase = EncryptNoteUseCase(repository)
        searchNotesUseCase = SearchNotesUseCase(repository)
    }

    override fun onTerminate() {
        notesIndexDatabase.close()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: NotesApplication
            private set
    }
}
