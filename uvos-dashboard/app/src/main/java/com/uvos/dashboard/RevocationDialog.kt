package com.uvos.dashboard

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

/**
 * UVOS RevocationDialog - диалог подтверждения отзыва доступа (DESIGN.md §3.9).
 */
class RevocationDialog(
    context: Context,
    private val channelName: String,
    private val onConfirm: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_revocation)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
        }

        val messageView = findViewById<TextView>(R.id.dialog_message)
        messageView.text = context.getString(R.string.revocation_message, channelName)

        findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dismiss()
        }

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            dismiss()
            onConfirm()
        }
    }
}
