package br.com.findertogoogle.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import br.com.findertogoogle.R
import br.com.findertogoogle.data.AppPreferences
import br.com.findertogoogle.data.FinderSignatureStore
import br.com.findertogoogle.routing.*
import br.com.findertogoogle.system.BrowserRoleHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity(), SettingsActions {
    private lateinit var prefs: AppPreferences
    private lateinit var browser: BrowserForwarder
    private lateinit var role: BrowserRoleHelper
    private lateinit var signatures: FinderSignatureStore
    private lateinit var pages: SettingsPages
    private var page = SettingsPage.HOME
    private var step = SetupStep.BROWSER
    private var answer = FinderTestAnswer.NONE
    private var supportExpanded = false
    private var diagnosticExpanded = false
    private var calibrationContext = false
    private var calibrationCanceled = false
    private val history = mutableListOf<SettingsPage>()
    private val positions = mutableMapOf<SettingsPage, Int>()
    private var verificationContext: VerificationContext? = null
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (role.isHeld() && page == SettingsPage.SETUP) step = SetupStep.FINDER
        else if (!role.isHeld()) toast(R.string.role_canceled)
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareEdgeToEdge()
        setContentView(R.layout.activity_settings)
        applySystemInsets()
        prefs = AppPreferences(this)
        signatures = FinderSignatureStore(this)
        browser = BrowserForwarder(packageName, prefs, AndroidBrowserCatalog(this), ActivityStarter { startActivity(it) })
        role = BrowserRoleHelper(this)
        pages = SettingsPages(this, findViewById<ViewGroup>(R.id.page_content), this)
        savedInstanceState?.let { state ->
            page = state.getString("ui.page").enumOrNull<SettingsPage>() ?: SettingsPage.HOME
            step = state.getString("ui.step").enumOrNull<SetupStep>() ?: SetupStep.BROWSER
            // Human feedback is deliberately session-only, not a persisted success guarantee.
            supportExpanded = state.getBoolean("ui.support")
            diagnosticExpanded = state.getBoolean("ui.details")
            calibrationContext = state.getBoolean("ui.calibration_context")
            calibrationCanceled = state.getBoolean("ui.calibration_canceled")
            state.getStringArrayList("ui.history")?.mapNotNull { it.enumOrNull<SettingsPage>() }?.let(history::addAll)
            SettingsPage.entries.forEach { positions[it] = state.getInt("ui.scroll.${it.name}") }
        }
        findViewById<MaterialToolbar>(R.id.settings_toolbar).apply {
            setNavigationOnClickListener { goBack() }
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.nav_settings -> { openPage(SettingsPage.CONFIGURATION); true }
                    R.id.nav_help -> { openPage(SettingsPage.HELP); true }
                    else -> false
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { goBack() }
        })
        render()
    }

    override fun onResume() { super.onResume(); if (::pages.isInitialized) render() }
    override fun onPause() { if (::pages.isInitialized) savePosition(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        savePosition()
        super.onSaveInstanceState(outState)
        outState.putString("ui.page", page.name); outState.putString("ui.step", step.name)
        outState.putBoolean("ui.support", supportExpanded); outState.putBoolean("ui.details", diagnosticExpanded)
        outState.putBoolean("ui.calibration_context", calibrationContext); outState.putBoolean("ui.calibration_canceled", calibrationCanceled)
        outState.putStringArrayList("ui.history", ArrayList(history.map { it.name }))
        positions.forEach { (p, y) -> outState.putInt("ui.scroll.${p.name}", y) }
    }

    private fun snapshot(): SettingsSnapshot {
        val rules = signatures.all()
        return SettingsSnapshot(browser.selectedTarget()?.label, browser.hasSelection(), role.isAvailable(), role.isHeld(),
            prefs.experimentalFinderDetection, FinderCalibration.process.current()?.phrase, rules.count { it.enabled }, rules.size,
            prefs.diagnosticsEnabled, prefs.lastDiagnostic)
    }
    private fun savePosition() { positions[page] = findViewById<ScrollView>(R.id.page_scroll).scrollY }
    private fun render() {
        val host = findViewById<ViewGroup>(R.id.page_content)
        val focus = PageFocus.capture(host)
        val state = snapshot()
        val context = VerificationContext(prefs.selectedBrowserPackage, state.browserLabel != null,
            state.roleHeld, state.compatibility, signatures.enabledClients())
        if (verificationContext != null && verificationContext != context) answer = FinderTestAnswer.NONE
        verificationContext = context
        step = state.validStep(step)
        val body = pages.render(page, state, step, answer, supportExpanded, diagnosticExpanded, calibrationContext, calibrationCanceled)
        findViewById<MaterialToolbar>(R.id.settings_toolbar).apply {
            navigationIcon = if (page == SettingsPage.HOME) null else AppCompatResources.getDrawable(this@SettingsActivity, R.drawable.ic_back)
            setNavigationContentDescription(R.string.navigate_back)
            menu.findItem(R.id.nav_settings).isVisible = page != SettingsPage.CONFIGURATION
            menu.findItem(R.id.nav_help).isVisible = page != SettingsPage.HELP
        }
        if (page == SettingsPage.PATTERNS) SignatureManager(this, signatures) { refresh() }.bind(body)
        PageFocus.restore(host, focus)
        val activePage = page
        findViewById<ScrollView>(R.id.page_scroll).post {
            if (page == activePage) findViewById<ScrollView>(R.id.page_scroll).scrollTo(0, positions[activePage] ?: 0)
        }
    }
    private fun refresh() { savePosition(); render() }
    override fun openPage(page: SettingsPage) {
        savePosition()
        if (this.page != page) {
            val previous = history.lastIndexOf(page)
            if (previous >= 0) while (history.size > previous) history.removeAt(history.lastIndex)
            else history += this.page
            this.page = page
        }
        if (page == SettingsPage.SETUP) { step = snapshot().firstSetupStep; answer = FinderTestAnswer.NONE }
        render()
    }
    private fun goBack() {
        if (page == SettingsPage.SETUP && step != SetupStep.BROWSER) { previousSetup(); return }
        savePosition()
        if (history.isEmpty()) { finish(); return }
        page = history.removeAt(history.lastIndex)
        render()
    }
    override fun homePrimary() {
        when (snapshot().homeAction) {
            HomeAction.SYSTEM_SETTINGS -> openDefaultApps()
            HomeAction.CONTINUE_CALIBRATION -> openPage(SettingsPage.CALIBRATION)
            else -> openPage(SettingsPage.SETUP)
        }
    }
    override fun chooseBrowser() { showBrowserPicker(browser) {
        if (page == SettingsPage.SETUP) step = if (role.isHeld()) SetupStep.FINDER else SetupStep.ANDROID
        refresh()
    } }
    override fun nextSetup() {
        val current = snapshot(); step = current.validStep(step)
        when (step) {
            SetupStep.BROWSER -> if (current.browserLabel == null) chooseBrowser() else { step = if (current.roleHeld) SetupStep.FINDER else SetupStep.ANDROID; render() }
            SetupStep.ANDROID -> if (current.roleHeld) { step = SetupStep.FINDER; render() } else requestRole()
            SetupStep.FINDER -> Unit
        }
    }
    override fun previousSetup() { step = if (step == SetupStep.FINDER) SetupStep.ANDROID else SetupStep.BROWSER; positions[SettingsPage.SETUP] = 0; render() }
    private fun requestRole() {
        if (browser.selectedTarget() == null) { step = SetupStep.BROWSER; render(); return }
        val intent = role.requestIntent()
        if (intent == null) { openDefaultApps(); return }
        try { roleRequest.launch(intent) } catch (_: ActivityNotFoundException) { toast(R.string.system_open_failed) }
        catch (_: SecurityException) { toast(R.string.system_open_failed) }
    }
    override fun setCompatibility(enabled: Boolean) { prefs.experimentalFinderDetection = enabled; refresh() }
    override fun answerFinder(answer: FinderTestAnswer) { this.answer = answer; refresh() }
    override fun finderProblem() {
        val state = snapshot()
        when {
            state.browserLabel == null || !state.roleHeld -> openPage(SettingsPage.SETUP)
            !state.compatibility -> openPage(SettingsPage.CONFIGURATION)
            else -> openPage(SettingsPage.CALIBRATION)
        }
    }
    override fun copyCalibrationTest() {
        if (browser.selectedTarget() == null || !role.isHeld()) { toast(R.string.calibration_need_role); openPage(SettingsPage.SETUP); return }
        val session = FinderCalibration.process.current() ?: FinderCalibration.process.begin()
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(getString(R.string.calibration_test_label), session.phrase))
        calibrationContext = true; calibrationCanceled = false
        refresh()
    }
    override fun cancelCalibration() { FinderCalibration.process.cancel(); calibrationContext = true; calibrationCanceled = true; refresh() }
    override fun openDefaultApps() {
        try { startActivity(role.defaultAppsIntent()) } catch (_: ActivityNotFoundException) { toast(R.string.system_open_failed) }
        catch (_: SecurityException) { toast(R.string.system_open_failed) }
    }
    override fun testGoogle() {
        when (val result = SearchLauncher(this).open(getString(R.string.test_query))) {
            SearchLaunchResult.Launched -> Unit
            is SearchLaunchResult.Failed -> toast(result.reason.messageRes())
        }
    }
    override fun testBrowser() {
        when (val result = browser.retry(Uri.parse("https://example.com/?origem=finder-to-google#teste"))) {
            BrowserResult.Opened -> Unit
            is BrowserResult.NeedsRecovery -> MaterialAlertDialogBuilder(this).setMessage(result.reason.messageRes())
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.choose_browser) { _, _ -> chooseBrowser() }.show()
        }
    }
    override fun setDiagnostics(enabled: Boolean) { prefs.diagnosticsEnabled = enabled; refresh() }
    override fun copyDiagnostic() { if (DiagnosticClipboard.copyIfEnabled(this, prefs)) toast(R.string.diagnostics_copied) }
    override fun clearDiagnostic() { prefs.clearDiagnostic(); refresh() }
    override fun toggleSupport() { supportExpanded = !supportExpanded; refresh() }
    override fun toggleDiagnosticDetails() { diagnosticExpanded = !diagnosticExpanded; refresh() }
    private fun toast(message: Int) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    private inline fun <reified T : Enum<T>> String?.enumOrNull(): T? = this?.let { try { enumValueOf<T>(it) } catch (_: IllegalArgumentException) { null } }
}
