package br.com.findertogoogle.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.findertogoogle.BuildConfig
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
    private var advancedExpanded = false
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { refreshStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        setContentView(R.layout.activity_settings)
        applySystemInsets()
        prefs = AppPreferences(this)
        browser = BrowserForwarder(packageName, prefs, AndroidBrowserCatalog(this), ActivityStarter { startActivity(it) })
        role = BrowserRoleHelper(this)
        findViewById<MaterialButton>(R.id.choose_browser).setOnClickListener { showBrowserPicker(browser) { refreshStatus() } }
        findViewById<MaterialButton>(R.id.activate).setOnClickListener { requestBrowserRole() }
        findViewById<MaterialButton>(R.id.test_search).setOnClickListener {
            when (val result = SearchLauncher(this).open(getString(R.string.test_query))) {
                SearchLaunchResult.Launched -> Unit
                is SearchLaunchResult.Failed -> {
                    Toast.makeText(this, result.reason.messageRes(), Toast.LENGTH_LONG).show()
                    testBrowser(Uri.parse("https://www.google.com/search?q=teste%20Finder%20to%20Google"))
                }
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
            setOnCheckedChangeListener { _, enabled -> prefs.experimentalFinderDetection = enabled; refreshStatus() }
        }
        findViewById<MaterialSwitch>(R.id.diagnostics).apply {
            isChecked = prefs.diagnosticsEnabled
            setOnCheckedChangeListener { _, enabled -> prefs.diagnosticsEnabled = enabled; refreshDiagnostic() }
        }
        findViewById<MaterialButton>(R.id.copy_diagnostics).setOnClickListener {
            if (DiagnosticClipboard.copyIfEnabled(this, prefs)) Toast.makeText(this, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.clear_diagnostics).setOnClickListener { prefs.clearDiagnostic(); refreshDiagnostic() }
        findViewById<MaterialButton>(R.id.advanced_toggle).setOnClickListener { setAdvanced(!advancedExpanded) }
        setAdvanced(savedInstanceState?.getBoolean("advanced_expanded") ?: false)
        findViewById<TextView>(R.id.version).text = getString(R.string.version_label, BuildConfig.VERSION_NAME)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("advanced_expanded", advancedExpanded)
    }
    override fun onResume() { super.onResume(); refreshStatus() }

    private fun setAdvanced(expanded: Boolean) {
        advancedExpanded = expanded
        findViewById<View>(R.id.advanced_content).visibility = if (expanded) View.VISIBLE else View.GONE
        findViewById<MaterialButton>(R.id.advanced_toggle).setText(if (expanded) R.string.advanced_hide else R.string.advanced_show)
    }

    private fun refreshStatus() {
        if (!::role.isInitialized) return
        val held = role.isHeld()
        val target = browser.selectedTarget()
        findViewById<TextView>(R.id.status).setText(if (held) R.string.status_active else R.string.status_inactive)
        findViewById<TextView>(R.id.readiness).setText(when {
            target == null -> R.string.readiness_need_browser
            !held -> R.string.readiness_inactive
            else -> R.string.readiness_ready
        })
        findViewById<TextView>(R.id.mode_status).setText(if (prefs.experimentalFinderDetection) R.string.mode_compatibility else R.string.mode_standard)
        findViewById<MaterialButton>(R.id.activate).isEnabled = !held
        findViewById<TextView>(R.id.selected_browser).text = target?.label
            ?: getString(if (browser.hasSelection()) R.string.browser_missing else R.string.browser_none)
        findViewById<MaterialSwitch>(R.id.experimental).apply {
            if (isChecked != prefs.experimentalFinderDetection) isChecked = prefs.experimentalFinderDetection
        }
        findViewById<MaterialSwitch>(R.id.diagnostics).apply {
            if (isChecked != prefs.diagnosticsEnabled) isChecked = prefs.diagnosticsEnabled
        }
        refreshDiagnostic()
    }

    private fun refreshDiagnostic() {
        findViewById<TextView>(R.id.diagnostic_report).text = if (prefs.diagnosticsEnabled) prefs.lastDiagnostic
            ?: getString(R.string.diagnostics_empty) else getString(R.string.diagnostics_off)
        val available = prefs.diagnosticsEnabled && prefs.lastDiagnostic != null
        findViewById<MaterialButton>(R.id.copy_diagnostics).isEnabled = available
        findViewById<MaterialButton>(R.id.clear_diagnostics).isEnabled = available
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
                } else try { roleRequest.launch(request)
                } catch (_: ActivityNotFoundException) { systemScreenFailed()
                } catch (_: SecurityException) { systemScreenFailed() }
            }.show()
    }
    private fun openDefaultApps() {
        try { startActivity(role.defaultAppsIntent())
        } catch (_: ActivityNotFoundException) { systemScreenFailed()
        } catch (_: SecurityException) { systemScreenFailed() }
    }
    private fun systemScreenFailed() { Toast.makeText(this, R.string.system_open_failed, Toast.LENGTH_LONG).show() }

    private fun testBrowser(uri: Uri) {
        when (val result = browser.retry(uri)) {
            BrowserResult.Opened -> Unit
            is BrowserResult.NeedsRecovery -> MaterialAlertDialogBuilder(this).setMessage(result.reason.messageRes())
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.choose_browser) { _, _ ->
                    showBrowserPicker(browser) { refreshStatus(); testBrowser(uri) }
                }.show()
        }
    }
}
