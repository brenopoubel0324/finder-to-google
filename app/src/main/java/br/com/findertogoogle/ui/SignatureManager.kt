package br.com.findertogoogle.ui

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import br.com.findertogoogle.R
import br.com.findertogoogle.data.FinderSignatureStore
import br.com.findertogoogle.data.SignatureSaveResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignatureManager(
    private val activity: AppCompatActivity,
    private val store: FinderSignatureStore,
    private val onChanged: () -> Unit,
) {
    fun show(): Unit = with(activity) {
        val rules = store.all()
        val builder = MaterialAlertDialogBuilder(this).setTitle(R.string.signatures_title)
        if (rules.isEmpty()) builder.setMessage(R.string.signatures_empty)
        else builder.setItems(rules.map {
            getString(if (it.enabled) R.string.signature_enabled else R.string.signature_disabled, it.client)
        }.toTypedArray()) { _, index ->
            val rule = rules[index]
            MaterialAlertDialogBuilder(this).setTitle(rule.client)
                .setItems(arrayOf(getString(if (rule.enabled) R.string.signature_disable else R.string.signature_enable),
                    getString(R.string.signature_remove))) { _, action ->
                    if (action == 0) store.setEnabled(rule.client, !rule.enabled) else store.remove(rule.client)
                    onChanged(); show()
                }.setNegativeButton(R.string.cancel, null).show()
        }
        builder.setPositiveButton(R.string.signature_add) { _, _ -> showAdd() }
            .setNegativeButton(R.string.close, null).show()
    }

    private fun showAdd(): Unit = with(activity) {
        val view = layoutInflater.inflate(R.layout.dialog_signature, null)
        val input = view.findViewById<TextInputEditText>(R.id.signature_input)
        val field = view.findViewById<TextInputLayout>(R.id.signature_input_layout)
        val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.signature_add)
            .setView(view).setPositiveButton(R.string.signature_save, null)
            .setNegativeButton(R.string.cancel, null).create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val result = store.save(input.text?.toString().orEmpty())
                if (result == SignatureSaveResult.SAVED) {
                    dialog.dismiss(); onChanged(); show()
                } else field.error = getString(result.messageRes())
            }
        }
        dialog.show()
    }
}

fun SignatureSaveResult.messageRes(): Int = when (this) {
    SignatureSaveResult.SAVED -> R.string.signature_saved
    SignatureSaveResult.INVALID -> R.string.signature_invalid
    SignatureSaveResult.LIMIT_REACHED -> R.string.signature_limit
}
