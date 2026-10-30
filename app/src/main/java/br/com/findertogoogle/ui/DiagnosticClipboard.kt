package br.com.findertogoogle.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import br.com.findertogoogle.BuildConfig
import br.com.findertogoogle.R
import br.com.findertogoogle.data.AppPreferences

object DiagnosticClipboard {
    fun copyIfEnabled(context: Context, prefs: AppPreferences): Boolean {
        if (!prefs.diagnosticsEnabled) return false
        val report = prefs.lastDiagnostic ?: return false
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        val text = context.getString(R.string.version_label, BuildConfig.VERSION_NAME) + "\n\n" + report
        val clip = ClipData.newPlainText(context.getString(R.string.diagnostics_title), text)
        // API 24+ aceita extras; Android 13+ reconhece esta flag e oculta a prévia.
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        clipboard.setPrimaryClip(clip)
        return true
    }
}
