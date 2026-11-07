package com.uvos.notes.ui.list

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.uvos.notes.NotesApplication
import com.uvos.notes.domain.model.Note
import com.uvos.notes.ui.editor.MarkdownEditorScreen
import com.uvos.notes.ui.theme.UVOSBackground
import com.uvos.notes.ui.theme.UVOSNotesTheme

sealed class ScreenState {
    object List : ScreenState()
    data class Editor(val note: Note) : ScreenState()
}

class NotesActivity : FragmentActivity() {

    private val app by lazy { application as NotesApplication }

    private val viewModel: NotesViewModel by viewModels {
        NotesViewModel.Factory(
            repository = app.repository,
            saveNoteUseCase = app.saveNoteUseCase,
            encryptNoteUseCase = app.encryptNoteUseCase,
            searchNotesUseCase = app.searchNotesUseCase
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Seed demo notes if directory is completely empty
        seedSampleNotesIfNeeded()

        setContent {
            UVOSNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UVOSBackground
                ) {
                    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.List) }

                    AnimatedContent(
                        targetState = screenState,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screenTransition"
                    ) { state ->
                        when (state) {
                            is ScreenState.List -> {
                                NotesListScreen(
                                    viewModel = viewModel,
                                    onNoteClick = { note ->
                                        if (note.isEncrypted) {
                                            authenticateAndOpen(note) { unlocked ->
                                                screenState = ScreenState.Editor(unlocked)
                                            }
                                        } else {
                                            screenState = ScreenState.Editor(note)
                                        }
                                    },
                                    onCreateNote = { isEncrypted ->
                                        val newNote = viewModel.createNote(isEncrypted = isEncrypted)
                                        screenState = ScreenState.Editor(newNote)
                                    }
                                )
                            }
                            is ScreenState.Editor -> {
                                MarkdownEditorScreen(
                                    note = state.note,
                                    onSave = { updatedNote ->
                                        viewModel.saveNote(updatedNote)
                                    },
                                    onDelete = { noteToDelete ->
                                        viewModel.deleteNote(noteToDelete)
                                    },
                                    onBack = {
                                        screenState = ScreenState.List
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun authenticateAndOpen(note: Note, onSuccess: (Note) -> Unit) {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(this)
            val prompt = BiometricPrompt(
                this,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        viewModel.unlockNote(note) { unlocked ->
                            onSuccess(unlocked)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        Toast.makeText(this@NotesActivity, "Биометрия: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Сейф UnderVoltOS")
                .setSubtitle("Разблокируйте доступ к секретной заметке AES-256")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()

            prompt.authenticate(promptInfo)
        } else {
            // Hardware doesn't have biometric enrolled or enabled; directly decrypt with KeyStore key
            viewModel.unlockNote(note) { unlocked ->
                onSuccess(unlocked)
            }
        }
    }

    private fun seedSampleNotesIfNeeded() {
        val notes = app.noteFileManager.loadAllNotes()
        if (notes.isEmpty()) {
            val sampleMarkdown = """
                # UnderVoltOS Notes
                
                Добро пожаловать в **UVOS Notes** — полностью оффлайн инструмент для заметок.
                
                ### Возможности
                - [x] Открытый формат файлов `.md` на диске
                - [x] Аппаратное шифрование AES-256-GCM для Сейфа
                - [x] Локальный полнотекстовый поиск FTS4
                - [ ] Написать черновик для Syncthing
                
                ```kotlin
                // Стрелочный вольтметр M4200
                fun measureWords(count: Int) = count.coerceIn(0, 2000)
                ```
                
                > Безопасность начинается с отсутствия сетевого стека.
            """.trimIndent()

            val demoNote = Note(
                id = "welcome_guide",
                title = "UnderVoltOS Notes",
                content = sampleMarkdown,
                folder = "General"
            )
            app.noteFileManager.saveNote(demoNote)
        }
    }
}
