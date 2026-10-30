package br.com.findertogoogle.data

import android.content.ComponentName
import android.content.Context

interface BrowserSelection {
    var selectedBrowserPackage: String?
    val preferredBrowserComponent: ComponentName?
    fun selectBrowser(component: ComponentName)
}

class AppPreferences(context: Context) : BrowserSelection {
    private val selfPackage = context.packageName
    private val prefs = context.getSharedPreferences("router_settings", Context.MODE_PRIVATE)
    private fun string(key: String): String? = try { prefs.getString(key, null) } catch (_: ClassCastException) { null }
    private fun validPackage(value: String?): String? = value?.takeIf {
        it != selfPackage && it.length <= 200 && PACKAGE_PATTERN.matches(it)
    }
    private fun legacyComponent(): ComponentName? = string("browser_component")
        ?.let(ComponentName::unflattenFromString)?.takeIf { validPackage(it.packageName) != null }

    override var selectedBrowserPackage: String?
        get() {
            validPackage(string("browser_package"))?.let { return it }
            // Migração puramente local: não apagar a escolha se o navegador estiver ausente.
            val legacy = legacyComponent()?.packageName ?: return null
            prefs.edit().putString("browser_package", legacy).apply()
            return legacy
        }
        set(value) {
            require(value == null || validPackage(value) != null)
            val edit = prefs.edit()
            if (value == null) edit.remove("browser_package").remove("browser_component")
            else {
                edit.putString("browser_package", value)
                if (legacyComponent()?.packageName != value) edit.remove("browser_component")
            }
            edit.apply()
        }

    override val preferredBrowserComponent: ComponentName?
        get() = legacyComponent()?.takeIf { it.packageName == selectedBrowserPackage }

    override fun selectBrowser(component: ComponentName) {
        require(validPackage(component.packageName) != null)
        prefs.edit().putString("browser_package", component.packageName)
            .putString("browser_component", component.flattenToString()).apply()
    }

    var experimentalFinderDetection: Boolean
        get() = prefs.getBoolean("experimental_finder", false)
        set(value) { prefs.edit().putBoolean("experimental_finder", value).apply() }

    var diagnosticsEnabled: Boolean
        get() = prefs.getBoolean("diagnostics_enabled", false)
        set(value) {
            val edit = prefs.edit().putBoolean("diagnostics_enabled", value)
            if (!value) edit.remove("last_diagnostic")
            edit.apply()
        }

    val lastDiagnostic: String? get() = string("last_diagnostic")
    /** Recebe apenas a projeção saneada produzida por DiagnosticsFormatter. */
    fun recordDiagnostic(report: String) {
        if (diagnosticsEnabled) prefs.edit().putString("last_diagnostic", report).apply()
    }
    fun clearDiagnostic() { prefs.edit().remove("last_diagnostic").apply() }

    companion object { private val PACKAGE_PATTERN = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+") }
}
