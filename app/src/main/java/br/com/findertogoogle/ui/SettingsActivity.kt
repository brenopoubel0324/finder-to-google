package br.com.findertogoogle.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.findertogoogle.R
import br.com.findertogoogle.data.AppPreferences
import br.com.findertogoogle.routing.*
import br.com.findertogoogle.system.BrowserRoleHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: AppPreferences
    private lateinit var browser: BrowserForwarder
    private lateinit var role: BrowserRoleHelper
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshStatus() // O código de resultado não comprova que o papel foi concedido.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        setContentView(R.layout.activity_settings)
        applySystemInsets()
        prefs = AppPreferences(this)
        browser = BrowserForwarder(packageName, prefs, AndroidBrowserCatalog(this), ActivityStarter { startActivity(it) })
        role = BrowserRoleHelper(this)
        findViewById<MaterialButton>(R.id.choose_browser).setOnClickListener {
            showBrowserPicker(browser) { refreshStatus() }
        }
        findViewById<MaterialButton>(R.id.activate).setOnClickListener { requestBrowserRole() }
        findViewById<MaterialButton>(R.id.test_search).setOnClickListener {
            if (!SearchLauncher(this).open(getString(R.string.test_query))) {
                Toast.makeText(this, R.string.google_test_failed, Toast.LENGTH_LONG).show()
                testBrowser(Uri.parse("https://www.google.com/search?q=teste%20Finder%20to%20Google"))
            }
        }
        findViewById<MaterialButton>(R.id.test_link).setOnClickListener {
            testBrowser(Uri.parse("https://example.com/?origem=finder-to-google#teste"))
        }
        findViewById<MaterialButton>(R.id.restore).setOnClickListener {
            MaterialAlertDialogBuilder(this).setTitle(R.string.restore_title).setMessage(R.string.restore_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.open_settings) { _, _ -> openDefaultApps() }.show()
        }
        findViewById<MaterialSwitch>(R.id.experimental).apply {
            isChecked = prefs.experimentalFinderDetection
            setOnCheckedChangeListener { _, enabled -> prefs.experimentalFinderDetection = enabled }
        }
        findViewById<MaterialSwitch>(R.id.diagnostics).apply {
            isChecked = prefs.diagnosticsEnabled
            setOnCheckedChangeListener { _, enabled -> prefs.diagnosticsEnabled = enabled; refreshDiagnostic() }
        }
        findViewById<MaterialButton>(R.id.clear_diagnostics).setOnClickListener {
            prefs.clearDiagnostic()
            refreshDiagnostic()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (!::role.isInitialized) return
        val held = role.isHeld()
        findViewById<TextView>(R.id.status).setText(if (held) R.string.status_active else R.string.status_inactive)
        findViewById<MaterialButton>(R.id.activate).isEnabled = !held
        val target = browser.selectedTarget()
        findViewById<TextView>(R.id.selected_browser).text = when {
            target != null -> getString(R.string.browser_selected, target.label, target.component.packageName)
            prefs.selectedBrowser != null -> getString(R.string.browser_missing)
            else -> getString(R.string.browser_none)
        }
        refreshDiagnostic()
    }

    private fun refreshDiagnostic() {
        findViewById<TextView>(R.id.diagnostic_report).text = if (prefs.diagnosticsEnabled) {
            prefs.lastDiagnostic ?: getString(R.string.diagnostics_empty)
        } else getString(R.string.diagnostics_off)
        findViewById<MaterialButton>(R.id.clear_diagnostics).isEnabled = prefs.lastDiagnostic != null
    }

    private fun requestBrowserRole() {
        if (browser.selectedTarget() == null) {
            Toast.makeText(this, R.string.select_before_activate, Toast.LENGTH_LONG).show()
            showBrowserPicker(browser) { refreshStatus() }
            return
        }
        MaterialAlertDialogBuilder(this).setTitle(R.string.activation_title).setMessage(R.string.activation_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.continue_action) { _, _ ->
                val request = role.requestIntent()
                if (request == null) {
                    Toast.makeText(this, R.string.role_unavailable, Toast.LENGTH_LONG).show()
                    openDefaultApps()
                } else try {
                    roleRequest.launch(request)
                } catch (_: ActivityNotFoundException) { systemScreenFailed()
                } catch (_: SecurityException) { systemScreenFailed() }
            }.show()
    }

    private fun openDefaultApps() {
        try { startActivity(role.defaultAppsIntent())
        } catch (_: ActivityNotFoundException) { systemScreenFailed()
        } catch (_: SecurityException) { systemScreenFailed() }
    }

    private fun systemScreenFailed() {
        Toast.makeText(this, R.string.system_open_failed, Toast.LENGTH_LONG).show()
    }

    private fun testBrowser(uri: Uri) {
        when (val result = browser.open(uri)) {
            BrowserResult.Opened -> Unit
            is BrowserResult.NeedsRecovery -> MaterialAlertDialogBuilder(this)
                .setMessage(when (result.reason) {
                    BrowserFailure.NOT_CONFIGURED -> R.string.recovery_not_configured
                    BrowserFailure.NO_LONGER_AVAILABLE -> R.string.recovery_missing
                    else -> R.string.recovery_failed
                }).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.choose_browser) { _, _ ->
                    showBrowserPicker(browser) { refreshStatus(); testBrowser(uri) }
                }.show()
        }
    }
}
