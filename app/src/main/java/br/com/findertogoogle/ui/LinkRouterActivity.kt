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
import br.com.findertogoogle.data.FinderSignatureStore
import br.com.findertogoogle.data.SignatureSaveResult
import br.com.findertogoogle.routing.*
import com.google.android.material.button.MaterialButton

/** launchMode padrão: cada abertura mantém sua própria identidade e recuperação. */
class LinkRouterActivity : AppCompatActivity() {
    private lateinit var prefs: AppPreferences
    private lateinit var browser: BrowserForwarder
    private lateinit var origin: LinkOrigin
    private var originalUri: Uri? = null
    private var decision = RoutingDecision.INVALID_LINK
    private var googleFailure: SearchFailure? = null
    private var recoveryReason: BrowserFailure? = null
    private var calibrationClient: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        prefs = AppPreferences(this)
        browser = BrowserForwarder(packageName, prefs, AndroidBrowserCatalog(this), ActivityStarter { startActivity(it) })
        origin = LinkOriginResolver.resolve(this)
        originalUri = try { intent.data } catch (_: RuntimeException) { null }
        if (savedInstanceState?.containsKey("router.origin_kind") == true) {
            origin = LinkOrigin(
                savedInstanceState.getString("router.origin_kind").enumOrNull<OriginKind>() ?: OriginKind.UNKNOWN,
                LinkOriginResolver.safePackage(savedInstanceState.getString("router.origin_package")),
                savedInstanceState.getString("router.evidence").enumOrNull<IdentityEvidence>() ?: IdentityEvidence.NONE,
                LinkOriginResolver.safePackage(savedInstanceState.getString("router.hint")),
            )
        }
        if (savedInstanceState?.getBoolean("router.recovering") == true) {
            decision = savedInstanceState.getString("router.decision").enumOrNull<RoutingDecision>() ?: RoutingDecision.INVALID_LINK
            googleFailure = savedInstanceState.getString("router.google_failure").enumOrNull<SearchFailure>()
            val valid = intent.action == Intent.ACTION_VIEW && GoogleSearchUrlParser.isWebUri(originalUri)
            showRecovery(if (valid) savedInstanceState.getString("router.reason").enumOrNull<BrowserFailure>()
                ?: BrowserFailure.LAUNCH_FAILED else BrowserFailure.INVALID_LINK)
            return
        }
        val restoredClient = savedInstanceState?.getString("router.calibration_client")
        if (restoredClient != null && FinderClientSignature.isValid(restoredClient) &&
            intent.action == Intent.ACTION_VIEW && origin.kind != OriginKind.OTHER &&
            originalUri?.let { GoogleSearchUrlParser.finderClient(it) } == restoredClient) {
            showCalibration(restoredClient)
            return
        }
        when (val capture = FinderCalibration.process.inspect(intent.action, originalUri, origin)) {
            is CalibrationCapture.Candidate -> { showCalibration(capture.client); return }
            CalibrationCapture.Unsupported -> Toast.makeText(this, R.string.calibration_unsupported, Toast.LENGTH_LONG).show()
            CalibrationCapture.Ignore -> Unit
        }
        routeOriginal()
    }

    private fun routeOriginal() {
        calibrationClient = null
        val report = try {
            LinkRouter(SearchLauncher(this), browser).route(intent.action, originalUri, origin,
                prefs.experimentalFinderDetection, FinderSignatureStore(this).enabledClients())
        } catch (_: RuntimeException) { RouteReport(RoutingDecision.INVALID_LINK, RouteOutcome.InvalidInput) }
        decision = report.decision
        record(report)
        when (val outcome = report.outcome) {
            RouteOutcome.GoogleOpened -> finish()
            is RouteOutcome.BrowserOpened -> {
                if (outcome.googleFailure != null) Toast.makeText(this, R.string.google_failed, Toast.LENGTH_LONG).show()
                finish()
            }
            is RouteOutcome.Recovery -> {
                googleFailure = outcome.googleFailure
                showRecovery(outcome.reason)
            }
            RouteOutcome.InvalidInput -> showRecovery(BrowserFailure.INVALID_LINK)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (recoveryReason == null && calibrationClient == null) return
        outState.putString("router.calibration_client", calibrationClient)
        outState.putBoolean("router.recovering", recoveryReason != null)
        outState.putString("router.reason", recoveryReason?.name)
        outState.putString("router.decision", decision.name)
        outState.putString("router.google_failure", googleFailure?.name)
        outState.putString("router.origin_kind", origin.kind.name)
        outState.putString("router.origin_package", origin.packageName)
        outState.putString("router.evidence", origin.evidence.name)
        outState.putString("router.hint", origin.referrerHint)
        // A URI permanece apenas no Intent já recebido, sem nova cópia no estado salvo.
    }

    private fun record(report: RouteReport) {
        if (prefs.diagnosticsEnabled) prefs.recordDiagnostic(DiagnosticsFormatter.format(this, origin, report))
    }

    private fun showCalibration(client: String) {
        calibrationClient = client
        setContentView(R.layout.activity_calibration)
        applySystemInsets()
        findViewById<TextView>(R.id.calibration_client).text = client
        findViewById<MaterialButton>(R.id.confirm_signature).setOnClickListener {
            val result = FinderSignatureStore(this).save(client)
            if (result == SignatureSaveResult.SAVED) {
                // Confirmação local explícita; Intent/extras nunca ativam compatibilidade.
                prefs.experimentalFinderDetection = true
                routeOriginal()
            } else findViewById<TextView>(R.id.calibration_message).setText(result.messageRes())
        }
        findViewById<MaterialButton>(R.id.skip_signature).setOnClickListener { routeOriginal() }
    }

    private fun showRecovery(reason: BrowserFailure) {
        recoveryReason = reason
        setContentView(R.layout.activity_recovery)
        applySystemInsets()
        val detail = getString(reason.messageRes())
        findViewById<TextView>(R.id.recovery_message).text = googleFailure?.let {
            getString(it.messageRes()) + "\n\n" + detail
        } ?: detail
        findViewById<MaterialButton>(R.id.choose_browser).apply {
            visibility = if (reason == BrowserFailure.INVALID_LINK) View.GONE else View.VISIBLE
            setOnClickListener { showBrowserPicker(browser) { retryBrowser() } }
        }
        findViewById<MaterialButton>(R.id.retry).apply {
            visibility = if (reason == BrowserFailure.INVALID_LINK) View.GONE else View.VISIBLE
            setOnClickListener { retryBrowser() }
        }
        findViewById<MaterialButton>(R.id.app_settings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<MaterialButton>(R.id.close).setOnClickListener { finish() }
    }

    private fun retryBrowser() {
        val uri = originalUri ?: return
        when (val result = browser.retry(uri)) {
            BrowserResult.Opened -> {
                record(RouteReport(decision, RouteOutcome.BrowserOpened(googleFailure)))
                finish()
            }
            is BrowserResult.NeedsRecovery -> {
                record(RouteReport(decision, RouteOutcome.Recovery(result.reason, googleFailure)))
                showRecovery(result.reason)
            }
        }
    }
    private inline fun <reified T : Enum<T>> String?.enumOrNull(): T? = this?.let {
        try { enumValueOf<T>(it) } catch (_: IllegalArgumentException) { null }
    }
}
