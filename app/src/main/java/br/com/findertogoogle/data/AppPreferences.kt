package br.com.findertogoogle.data

import android.content.ComponentName
import android.content.Context

interface BrowserSelection {
    var selectedBrowser: ComponentName?
}

class AppPreferences(context: Context) : BrowserSelection {
    private val prefs = context.getSharedPreferences("router_settings", Context.MODE_PRIVATE)

    override var selectedBrowser: ComponentName?
        get() = prefs.getString("browser_component", null)?.let(ComponentName::unflattenFromString)
        set(value) { prefs.edit().putString("browser_component", value?.flattenToString()).apply() }

    var experimentalFinderDetection: Boolean
        get() = prefs.getBoolean("experimental_finder", false)
        set(value) { prefs.edit().putBoolean("experimental_finder", value).apply() }

    var diagnosticsEnabled: Boolean
        get() = prefs.getBoolean("diagnostics_enabled", false)
        set(value) {
            prefs.edit().putBoolean("diagnostics_enabled", value).apply()
            if (!value) clearDiagnostic()
        }

    val lastDiagnostic: String? get() = prefs.getString("last_diagnostic", null)

    /** Somente metadados produzidos pelo app. Nunca passar URL, query ou exceção aqui. */
    fun recordDiagnostic(report: String) {
        if (diagnosticsEnabled) prefs.edit().putString("last_diagnostic", report).apply()
    }

    fun clearDiagnostic() { prefs.edit().remove("last_diagnostic").apply() }
}
