package br.com.findertogoogle.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.findertogoogle.R
import br.com.findertogoogle.data.SettingsBackupCodec
import br.com.findertogoogle.data.SettingsRestore
import br.com.findertogoogle.routing.FinderCalibration
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** I/O somente após escolha explícita no seletor Android. Sem permissões ou tarefa residente. */
class SettingsTransferUi(private val activity: AppCompatActivity, private val changed: () -> Unit) {
    private var busy = false
    private val create = activity.registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) exportTo(uri)
    }
    private val open = activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFrom(uri)
    }
    fun export() = launch { create.launch("Finder-to-Google-configuracoes.json") }
    fun import() = launch { open.launch(arrayOf("*/*")) }
    private fun launch(action: () -> Unit) {
        if (busy) { toast(R.string.backup_busy); return }
        try { action() } catch (_: Exception) { toast(R.string.backup_picker_failed) }
    }
    private fun exportTo(uri: Uri) {
        if (busy) { toast(R.string.backup_busy); return }
        val snapshot = try { SettingsRestore.capture(activity) } catch (_: Exception) {
            toast(R.string.backup_failed); return
        }
        io {
            require(uri.scheme == "content")
            val bytes = SettingsBackupCodec.encode(snapshot)
            requireNotNull(activity.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
            val verified = requireNotNull(activity.contentResolver.openInputStream(uri)).use(SettingsBackupCodec::read)
            check(verified == snapshot)
            val complete: () -> Unit = { toast(R.string.backup_exported) }
            complete
        }
    }
    private fun importFrom(uri: Uri) = io {
        require(uri.scheme == "content")
        val snapshot = requireNotNull(activity.contentResolver.openInputStream(uri)).use(SettingsBackupCodec::read)
        val message = activity.getString(R.string.backup_preview,
            snapshot.browserPackage ?: activity.getString(R.string.backup_no_browser),
            activity.getString(if (snapshot.compatibility) R.string.backup_on else R.string.backup_off),
            activity.getString(if (snapshot.diagnostics) R.string.backup_on else R.string.backup_off),
            snapshot.rules.size, snapshot.rules.count { !it.enabled })
        val complete: () -> Unit = {
            MaterialAlertDialogBuilder(activity).setTitle(R.string.backup_import_title).setMessage(message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.backup_replace) { _, _ ->
                    try {
                        FinderCalibration.process.cancel()
                        SettingsRestore.restore(activity, snapshot)
                        changed()
                        toast(R.string.backup_imported)
                    } catch (_: Exception) { toast(R.string.backup_restore_failed) }
                }.show()
        }
        complete
    }
    private fun io(work: () -> (() -> Unit)) {
        if (busy) { toast(R.string.backup_busy); return }
        busy = true
        Thread({
            val result: () -> Unit = try { work() } catch (_: Exception) { { toast(R.string.backup_failed) } }
            activity.runOnUiThread {
                busy = false
                if (!activity.isDestroyed && !activity.isFinishing) result()
            }
        }, "settings-transfer").start()
    }
    private fun toast(message: Int) = Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
}
