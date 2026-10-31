package com.uvos.dashboard

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.uvos.dashboard.model.VaultNote
import com.uvos.dashboard.repository.VaultRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * UVOS VaultActivity - экран аналогового защищенного сейфа с механическим замком.
 */
class VaultActivity : Activity() {

    private lateinit var repository: VaultRepository

    private lateinit var containerDialLock: LinearLayout
    private lateinit var containerVaultContent: LinearLayout
    private lateinit var safeDialView: SafeDialView
    private lateinit var statusLamp: LampView
    private lateinit var instructionText: TextView
    private lateinit var errorText: TextView

    private lateinit var codeSlot0: TextView
    private lateinit var codeSlot1: TextView
    private lateinit var codeSlot2: TextView

    private lateinit var editTitle: EditText
    private lateinit var editContent: EditText
    private lateinit var btnSaveNote: Button
    private lateinit var btnLockVault: Button
    private lateinit var notesCountText: TextView
    private lateinit var notesContainer: LinearLayout

    private val enteredCombination = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Защита от скриншотов и перехвата в диспетчере задач (OPSEC)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_vault)
        repository = VaultRepository.getInstance(this)

        initViews()
        setupListeners()
        updateLockUiState()
    }

    override fun onStop() {
        super.onStop()
        // При уходе с экрана сейф мгновенно защелкивается
        repository.lock()
        enteredCombination.clear()
        updateLockUiState()
    }

    private fun initViews() {
        containerDialLock = findViewById(R.id.container_dial_lock)
        containerVaultContent = findViewById(R.id.container_vault_content)
        safeDialView = findViewById(R.id.safe_dial_view)
        statusLamp = findViewById(R.id.vault_status_lamp)
        instructionText = findViewById(R.id.vault_instruction_text)
        errorText = findViewById(R.id.vault_error_text)

        codeSlot0 = findViewById(R.id.code_slot_0)
        codeSlot1 = findViewById(R.id.code_slot_1)
        codeSlot2 = findViewById(R.id.code_slot_2)

        editTitle = findViewById(R.id.edit_note_title)
        editContent = findViewById(R.id.edit_note_content)
        btnSaveNote = findViewById(R.id.btn_save_note)
        btnLockVault = findViewById(R.id.btn_lock_vault)
        notesCountText = findViewById(R.id.vault_notes_count)
        notesContainer = findViewById(R.id.vault_notes_container)

        findViewById<Button>(R.id.vault_btn_back).setOnClickListener {
            finish()
        }
    }

    private fun setupListeners() {
        // Подтверждение деления нажатием центральной ручки лимба
        safeDialView.onNumberConfirmed = { number ->
            recordCombinationSlot(number)
        }

        // Кнопка подтверждения деления
        findViewById<Button>(R.id.btn_confirm_slot).setOnClickListener {
            val currentNum = safeDialView.getCurrentNumber()
            recordCombinationSlot(currentNum)
        }

        // Кнопка сброса набора
        findViewById<Button>(R.id.btn_reset_combination).setOnClickListener {
            resetCombinationInput()
        }

        // Кнопка сохранения секретной записи
        btnSaveNote.setOnClickListener {
            val title = editTitle.text.toString().trim()
            val content = editContent.text.toString().trim()
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Заполните заголовок и текст записи", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            repository.addNote(title, content)
            editTitle.text.clear()
            editContent.text.clear()
            renderNotesList()
            Toast.makeText(this, "Запись зашифрована в Keystore", Toast.LENGTH_SHORT).show()
        }

        // Кнопка ручной блокировки
        btnLockVault.setOnClickListener {
            repository.lock()
            resetCombinationInput()
            updateLockUiState()
        }
    }

    private fun recordCombinationSlot(number: Int) {
        errorText.visibility = View.INVISIBLE
        if (enteredCombination.size < 3) {
            enteredCombination.add(number)
            updateSlotDisplays()
        }

        // Набрано 3 деления
        if (enteredCombination.size == 3) {
            if (!repository.isInitialized()) {
                // Первичная установка кода
                val success = repository.setupVault(enteredCombination)
                if (success) {
                    Toast.makeText(this, "Комбинация сейфа установлена!", Toast.LENGTH_LONG).show()
                    updateLockUiState()
                } else {
                    showError("Ошибка создания хранилища")
                }
            } else {
                // Открытие существующего сейфа
                val success = repository.unlock(enteredCombination)
                if (success) {
                    updateLockUiState()
                } else {
                    showError("НЕВЕРНАЯ КОМБИНАЦИЯ ЗАМКА")
                    resetCombinationInput()
                }
            }
        }
    }

    private fun resetCombinationInput() {
        enteredCombination.clear()
        updateSlotDisplays()
    }

    private fun updateSlotDisplays() {
        codeSlot0.text = if (enteredCombination.size > 0) String.format("%02d", enteredCombination[0]) else "--"
        codeSlot1.text = if (enteredCombination.size > 1) String.format("%02d", enteredCombination[1]) else "--"
        codeSlot2.text = if (enteredCombination.size > 2) String.format("%02d", enteredCombination[2]) else "--"
    }

    private fun showError(msg: String) {
        errorText.text = msg
        errorText.visibility = View.VISIBLE
    }

    private fun updateLockUiState() {
        val isUnlocked = repository.isUnlocked()
        statusLamp.setOn(isUnlocked)

        if (isUnlocked) {
            containerDialLock.visibility = View.GONE
            containerVaultContent.visibility = View.VISIBLE
            renderNotesList()
        } else {
            containerDialLock.visibility = View.VISIBLE
            containerVaultContent.visibility = View.GONE
            if (!repository.isInitialized()) {
                instructionText.text = "УСТАНОВИТЕ НОВЫЙ КОД (3 ДЕЛЕНИЯ ЛИМБА)"
            } else {
                instructionText.text = "ВРАЩАЙТЕ ЛИМБ ДЛЯ ВВОДА КОДА"
            }
            updateSlotDisplays()
        }
    }

    private fun renderNotesList() {
        notesContainer.removeAllViews()
        val notes = repository.getNotes()
        notesCountText.text = "СОХРАНЁННЫЕ ЗАПИСИ (${notes.size})"

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        notes.forEach { note ->
            val row = layoutInflater.inflate(R.layout.item_vault_note, notesContainer, false)
            val titleView = row.findViewById<TextView>(R.id.note_title)
            val contentView = row.findViewById<TextView>(R.id.note_content)
            val dateView = row.findViewById<TextView>(R.id.note_date)
            val btnDelete = row.findViewById<TextView>(R.id.btn_delete_note)

            titleView.text = note.title
            contentView.text = note.content
            dateView.text = dateFormat.format(Date(note.timestamp))

            btnDelete.setOnClickListener {
                repository.deleteNote(note.id)
                renderNotesList()
            }

            notesContainer.addView(row)
        }
    }
}
