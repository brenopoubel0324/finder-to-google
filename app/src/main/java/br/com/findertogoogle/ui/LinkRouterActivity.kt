package br.com.findertogoogle.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import br.com.findertogoogle.R
import br.com.findertogoogle.data.AppPreferences
import br.com.findertogoogle.routing.*
import com.google.android.material.button.MaterialButton

/** launchMode padrão: cada abertura tem sua própria identidade, sem reutilizar chamador anterior. */
class LinkRouterActivity : AppCompatActivity() {
    private lateinit var prefs: AppPreferences
    private lateinit var browser: BrowserForwarder
    private lateinit var origin: LinkOrigin
    private var originalUri: Uri? = null
    private var decision = RoutingDecision.INVALID_LINK
    private var googleFailed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        prefs = AppPreferences(this)
        browser = BrowserForwarder(packageName, prefs, AndroidBrowserCatalog(this), ActivityStarter { startActivity(it) })
        origin = LinkOriginResolver.resolve(this)
        val report = try {
            originalUri = intent.data
            LinkRouter(SearchLauncher(this), browser).route(intent.action, originalUri, origin, prefs.experimentalFinderDetection)
        } catch (_: RuntimeException) {
            RouteReport(RoutingDecision.INVALID_LINK, RouteOutcome.InvalidInput)
        }
        decision = report.decision
        record(report)
        when (val outcome = report.outcome) {
            RouteOutcome.GoogleOpened -> finish()
            is RouteOutcome.BrowserOpened -> {
                if (outcome.googleFailed) Toast.makeText(this, R.string.google_failed, Toast.LENGTH_LONG).show()
                finish()
            }
            is RouteOutcome.Recovery -> {
                googleFailed = outcome.googleFailed
                showRecovery(outcome.reason)
            }
            RouteOutcome.InvalidInput -> showRecovery(BrowserFailure.INVALID_LINK)
        }
    }

    private fun record(report: RouteReport) {
        if (prefs.diagnosticsEnabled) prefs.recordDiagnostic(DiagnosticsFormatter.format(this, origin, report))
    }

    private fun showRecovery(reason: BrowserFailure) {
        setContentView(R.layout.activity_recovery)
        applySystemInsets()
        val message = getString(when (reason) {
            BrowserFailure.NOT_CONFIGURED -> R.string.recovery_not_configured
            BrowserFailure.NO_LONGER_AVAILABLE -> R.string.recovery_missing
            BrowserFailure.LAUNCH_FAILED -> R.string.recovery_failed
            BrowserFailure.INVALID_LINK -> R.string.invalid_link
        })
        findViewById<TextView>(R.id.recovery_message).text = if (googleFailed) {
            getString(R.string.google_failed) + "\n\n" + message
        } else message
        findViewById<MaterialButton>(R.id.choose_browser).apply {
            visibility = if (reason == BrowserFailure.INVALID_LINK) View.GONE else View.VISIBLE
            setOnClickListener { showBrowserPicker(browser) { retryBrowser() } }
        }
        findViewById<MaterialButton>(R.id.retry).apply {
            visibility = if (reason == BrowserFailure.INVALID_LINK) View.GONE else View.VISIBLE
            setOnClickListener { retryBrowser() }
        }
        findViewById<MaterialButton>(R.id.app_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.close).setOnClickListener { finish() }
    }

    private fun retryBrowser() {
        val uri = originalUri ?: return
        when (val result = browser.open(uri)) {
            BrowserResult.Opened -> {
                record(RouteReport(decision, RouteOutcome.BrowserOpened(googleFailed)))
                finish()
            }
            is BrowserResult.NeedsRecovery -> showRecovery(result.reason)
        }
    }
}
