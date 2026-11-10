package br.com.findertogoogle.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import br.com.findertogoogle.BuildConfig
import br.com.findertogoogle.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

interface SettingsActions {
    fun homePrimary()
    fun openPage(page: SettingsPage)
    fun chooseBrowser()
    fun nextSetup()
    fun previousSetup()
    fun setCompatibility(enabled: Boolean)
    fun answerFinder(answer: FinderTestAnswer)
    fun finderProblem()
    fun copyCalibrationTest()
    fun cancelCalibration()
    fun openDefaultApps()
    fun testGoogle()
    fun testBrowser()
    fun setDiagnostics(enabled: Boolean)
    fun copyDiagnostic()
    fun clearDiagnostic()
    fun toggleSupport()
    fun toggleDiagnosticDetails()
    fun exportSettings()
    fun importSettings()
}

/** Renderização sem acesso a Intent, URI ou escrita de preferências. */
class SettingsPages(private val activity: AppCompatActivity, private val host: ViewGroup, private val actions: SettingsActions) {
    fun render(page: SettingsPage, state: SettingsSnapshot, step: SetupStep, answer: FinderTestAnswer,
        support: Boolean, details: Boolean, calibrationContext: Boolean, calibrationCanceled: Boolean): View {
        host.removeAllViews()
        val layout = when (page) {
            SettingsPage.HOME -> R.layout.page_home
            SettingsPage.CONFIGURATION -> R.layout.page_configuration
            SettingsPage.HELP -> R.layout.page_help
            SettingsPage.SETUP -> R.layout.page_setup
            SettingsPage.CALIBRATION -> R.layout.page_calibration
            SettingsPage.PATTERNS -> R.layout.page_patterns
        }
        val root = activity.layoutInflater.inflate(layout, host, false)
        host.addView(root)
        when (page) {
            SettingsPage.HOME -> {
                root.text(R.id.status, activity.getString(if (state.roleHeld) R.string.status_active else R.string.status_inactive))
                val (message, label) = when (state.homeAction) {
                    HomeAction.CHOOSE_BROWSER -> R.string.home_choose to R.string.configure
                    HomeAction.REPLACE_BROWSER -> R.string.home_replace to R.string.replace_browser
                    HomeAction.ACTIVATE -> R.string.home_activate to R.string.configure
                    HomeAction.SYSTEM_SETTINGS -> R.string.home_unavailable to R.string.open_system_settings
                    HomeAction.CONTINUE_CALIBRATION -> R.string.home_pending to R.string.continue_calibration
                    HomeAction.TEST_FINDER -> R.string.home_ready to R.string.setup_finder_title
                }
                root.text(R.id.readiness, activity.getString(message))
                root.findViewById<MaterialButton>(R.id.home_primary).setText(label)
                root.click(R.id.home_primary, actions::homePrimary)
                root.text(R.id.selected_browser, browserLabel(state))
                root.click(R.id.change_browser, actions::chooseBrowser)
                root.show(R.id.change_browser, state.browserLabel != null)
            }
            SettingsPage.CONFIGURATION -> {
                root.text(R.id.selected_browser, browserLabel(state))
                root.click(R.id.choose_browser, actions::chooseBrowser)
                root.mode(state)
                root.ruleCount(state)
                root.click(R.id.manage_signatures) { actions.openPage(SettingsPage.PATTERNS) }
                root.click(R.id.configure_guide) { actions.openPage(SettingsPage.SETUP) }
                root.click(R.id.restore, actions::openDefaultApps)
                root.click(R.id.export_settings, actions::exportSettings)
                root.click(R.id.import_settings, actions::importSettings)
            }
            SettingsPage.HELP -> {
                root.click(R.id.finder_problem, actions::finderProblem)
                root.click(R.id.test_search, actions::testGoogle)
                root.click(R.id.test_link, actions::testBrowser)
                root.findViewById<MaterialButton>(R.id.advanced_toggle).setText(if (support) R.string.advanced_hide else R.string.advanced_show)
                root.click(R.id.advanced_toggle, actions::toggleSupport)
                root.show(R.id.advanced_content, support)
                root.findViewById<MaterialSwitch>(R.id.diagnostics).apply {
                    isSaveEnabled = false
                    isSaveFromParentEnabled = false
                    isChecked = state.diagnosticsEnabled
                    setOnCheckedChangeListener { _, checked -> actions.setDiagnostics(checked) }
                }
                val available = state.diagnosticsEnabled && state.diagnostic != null
                root.text(R.id.diagnostic_summary, activity.getString(when {
                    !state.diagnosticsEnabled -> R.string.diagnostics_off
                    available -> R.string.diagnostic_available
                    else -> R.string.diagnostics_empty
                }))
                root.text(R.id.diagnostic_report, if (state.diagnosticsEnabled) state.diagnostic.orEmpty() else "")
                root.show(R.id.diagnostic_report, available && details)
                root.findViewById<MaterialButton>(R.id.copy_diagnostics).isEnabled = available
                root.findViewById<MaterialButton>(R.id.clear_diagnostics).isEnabled = available
                root.findViewById<MaterialButton>(R.id.diagnostic_details_toggle).apply {
                    isEnabled = available
                    setText(if (details) R.string.diagnostic_details_hide else R.string.diagnostic_details)
                }
                root.click(R.id.copy_diagnostics, actions::copyDiagnostic)
                root.click(R.id.clear_diagnostics, actions::clearDiagnostic)
                root.click(R.id.diagnostic_details_toggle, actions::toggleDiagnosticDetails)
            }
            SettingsPage.SETUP -> {
                root.text(R.id.setup_progress, activity.getString(R.string.setup_progress, step.ordinal + 1))
                root.show(R.id.step_browser, step == SetupStep.BROWSER)
                root.show(R.id.step_android, step == SetupStep.ANDROID)
                root.show(R.id.step_finder, step == SetupStep.FINDER)
                root.text(R.id.selected_browser, browserLabel(state))
                root.click(R.id.change_browser, actions::chooseBrowser)
                root.show(R.id.change_browser, state.browserLabel != null)
                root.text(R.id.role_status, activity.getString(if (state.roleHeld) R.string.status_active else R.string.status_inactive))
                root.mode(state)
                root.show(R.id.setup_continue, step != SetupStep.FINDER)
                root.findViewById<MaterialButton>(R.id.setup_continue).setText(when (step) {
                    SetupStep.BROWSER -> if (state.browserLabel == null) R.string.choose_browser else R.string.continue_action
                    SetupStep.ANDROID -> if (state.roleHeld) R.string.continue_action else if (state.roleAvailable) R.string.activate_plain else R.string.open_system_settings
                    SetupStep.FINDER -> R.string.continue_action
                })
                root.click(R.id.setup_continue, actions::nextSetup)
                root.show(R.id.setup_previous, step != SetupStep.BROWSER)
                root.click(R.id.setup_previous, actions::previousSetup)
                root.click(R.id.answer_google) { actions.answerFinder(FinderTestAnswer.CONFIRMED) }
                root.click(R.id.answer_browser, actions::finderProblem)
                root.click(R.id.answer_not_tested) { actions.answerFinder(FinderTestAnswer.NOT_TESTED) }
                root.text(R.id.setup_feedback, when (answer) {
                    FinderTestAnswer.NONE -> ""
                    FinderTestAnswer.CONFIRMED -> activity.getString(R.string.answer_confirmed)
                    FinderTestAnswer.NOT_TESTED -> activity.getString(R.string.answer_later)
                })
            }
            SettingsPage.CALIBRATION -> {
                val pending = state.pendingPhrase != null
                root.text(R.id.calibration_step, activity.getString(if (pending) R.string.calibration_step_waiting else R.string.calibration_step_ready))
                root.text(R.id.calibration_help, activity.getString(when {
                    pending -> R.string.calibration_pending_help
                    calibrationCanceled -> R.string.calibration_canceled
                    calibrationContext -> R.string.calibration_ended
                    else -> R.string.calibration_prepare
                }))
                root.text(R.id.calibration_status, state.pendingPhrase.orEmpty())
                root.show(R.id.calibration_status, pending)
                root.show(R.id.learn_signature, !pending)
                root.findViewById<MaterialButton>(R.id.learn_signature).setText(if (calibrationContext) R.string.calibration_restart else R.string.calibration_start)
                root.show(R.id.copy_calibration, pending)
                root.show(R.id.cancel_calibration, pending)
                root.show(R.id.test_finder_after_calibration, !pending && calibrationContext)
                root.click(R.id.learn_signature, actions::copyCalibrationTest)
                root.click(R.id.copy_calibration, actions::copyCalibrationTest)
                root.click(R.id.cancel_calibration, actions::cancelCalibration)
                root.click(R.id.test_finder_after_calibration) { actions.openPage(SettingsPage.SETUP) }
            }
            SettingsPage.PATTERNS -> root.ruleCount(state)
        }
        val footer = TextView(activity).apply {
            setTextAppearance(R.style.UiCaption)
            text = activity.getString(R.string.version_label, BuildConfig.VERSION_NAME)
            setPadding(0, (24 * resources.displayMetrics.density).toInt(), 0, 0)
        }
        host.addView(footer)
        return root
    }

    private fun browserLabel(state: SettingsSnapshot) = state.browserLabel ?: activity.getString(if (state.hasBrowserChoice) R.string.browser_missing else R.string.browser_none)
    private fun View.ruleCount(state: SettingsSnapshot) = text(R.id.signatures_status, activity.getString(R.string.signatures_count, state.enabledRules, state.totalRules))
    private fun View.mode(state: SettingsSnapshot) {
        findViewById<MaterialSwitch>(R.id.experimental).apply {
            // Impede também restaurar checked obsoleto vindo de um Bundle legado.
            isSaveEnabled = false
            isSaveFromParentEnabled = false
            isChecked = state.compatibility
            setOnCheckedChangeListener { _, checked -> actions.setCompatibility(checked) }
        }
    }
    private fun View.text(id: Int, value: CharSequence) { findViewById<TextView>(id).text = value }
    private fun View.click(id: Int, action: () -> Unit) { findViewById<View>(id).setOnClickListener { action() } }
    private fun View.show(id: Int, visible: Boolean) { findViewById<View>(id).visibility = if (visible) View.VISIBLE else View.GONE }
}
