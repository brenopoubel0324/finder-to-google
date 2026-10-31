package br.com.findertogoogle.ui

import android.content.DialogInterface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.findertogoogle.R
import br.com.findertogoogle.data.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SignatureManager(private val activity: AppCompatActivity, private val store: FinderSignatureStore, private val onChanged: () -> Unit) {
    fun bind(root: View) {
        val list = root.findViewById<LinearLayout>(R.id.patterns_list)
        val rules = store.all()
        if (rules.isEmpty()) list.addView(TextView(activity).apply { setTextAppearance(R.style.UiBody); setText(R.string.signatures_empty) })
        rules.forEach { rule ->
            val row = activity.layoutInflater.inflate(R.layout.item_signature, list, false)
            val label = activity.getString(when (rule.client) {
                "tablet-android-samsung-rvo1" -> R.string.pattern_tablet
                "ms-android-samsung-a1-terr1-rvo1aqb" -> R.string.pattern_phone
                else -> R.string.pattern_custom
            })
            row.findViewById<TextView>(R.id.pattern_client).text = rule.client
            row.findViewById<MaterialSwitch>(R.id.pattern_toggle).apply {
                isSaveEnabled = false
                isSaveFromParentEnabled = false
                text = label
                tag = rule.client + ":toggle"
                contentDescription = "$label: ${rule.client}"
                isChecked = rule.enabled
                setOnCheckedChangeListener { _, checked -> store.setEnabled(rule.client, checked); onChanged() }
            }
            row.findViewById<MaterialButton>(R.id.pattern_delete).apply {
                tag = rule.client + ":delete"
                contentDescription = activity.getString(R.string.delete_pattern_description, rule.client)
                setOnClickListener {
                    val removed = store.all().firstOrNull { it.client == rule.client } ?: return@setOnClickListener
                    store.remove(rule.client); onChanged()
                    Snackbar.make(activity.findViewById(R.id.root), R.string.pattern_removed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo) {
                            val message = when (store.restore(removed)) {
                                SignatureRestoreResult.RESTORED -> R.string.undo_restored
                                SignatureRestoreResult.ALREADY_EXISTS -> R.string.undo_conflict
                                SignatureRestoreResult.LIMIT_REACHED -> R.string.signature_limit
                                SignatureRestoreResult.INVALID -> R.string.signature_invalid
                            }
                            onChanged()
                            Snackbar.make(activity.findViewById(R.id.root), message, Snackbar.LENGTH_LONG).show()
                        }.show()
                }
            }
            list.addView(row)
        }
        root.findViewById<MaterialButton>(R.id.add_signature).setOnClickListener { showAdd() }
    }

    private fun showAdd() {
        val view = activity.layoutInflater.inflate(R.layout.dialog_signature, null)
        val input = view.findViewById<TextInputEditText>(R.id.signature_input)
        val field = view.findViewById<TextInputLayout>(R.id.signature_input_layout)
        val dialog = MaterialAlertDialogBuilder(activity).setTitle(R.string.signature_add).setView(view)
            .setPositiveButton(R.string.signature_save, null).setNegativeButton(R.string.cancel, null).create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val result = store.save(input.text?.toString().orEmpty())
                if (result == SignatureSaveResult.SAVED) { dialog.dismiss(); onChanged() }
                else field.error = activity.getString(result.messageRes())
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
