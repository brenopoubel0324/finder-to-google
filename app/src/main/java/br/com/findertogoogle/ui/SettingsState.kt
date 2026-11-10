package br.com.findertogoogle.ui

import br.com.findertogoogle.data.FinderSignature

enum class SettingsPage { HOME, CONFIGURATION, HELP, SETUP, CALIBRATION, PATTERNS }
enum class SetupStep { BROWSER, ANDROID, FINDER }
enum class FinderTestAnswer { NONE, CONFIRMED, NOT_TESTED }
enum class HomeAction { CHOOSE_BROWSER, REPLACE_BROWSER, ACTIVATE, SYSTEM_SETTINGS, CONTINUE_CALIBRATION, TEST_FINDER }
data class VerificationContext(val browserPackage: String?, val browserAvailable: Boolean,
    val roleHeld: Boolean, val compatibility: Boolean, val activeClients: Set<String>)

internal data class SettingsRenderKey(val page: SettingsPage, val state: SettingsSnapshot,
    val step: SetupStep, val answer: FinderTestAnswer, val support: Boolean, val details: Boolean,
    val calibrationContext: Boolean, val calibrationCanceled: Boolean, val rules: List<FinderSignature>)

data class SettingsSnapshot(
    val browserLabel: String?, val hasBrowserChoice: Boolean,
    val roleAvailable: Boolean, val roleHeld: Boolean, val compatibility: Boolean,
    val pendingPhrase: String?, val enabledRules: Int, val totalRules: Int,
    val diagnosticsEnabled: Boolean, val diagnostic: String?,
) {
    val homeAction: HomeAction get() = when {
        browserLabel == null -> if (hasBrowserChoice) HomeAction.REPLACE_BROWSER else HomeAction.CHOOSE_BROWSER
        !roleHeld -> if (roleAvailable) HomeAction.ACTIVATE else HomeAction.SYSTEM_SETTINGS
        pendingPhrase != null -> HomeAction.CONTINUE_CALIBRATION
        else -> HomeAction.TEST_FINDER
    }
    val firstSetupStep: SetupStep get() = when {
        browserLabel == null -> SetupStep.BROWSER
        !roleHeld -> SetupStep.ANDROID
        else -> SetupStep.FINDER
    }
    fun validStep(requested: SetupStep): SetupStep = when {
        requested != SetupStep.BROWSER && browserLabel == null -> SetupStep.BROWSER
        requested == SetupStep.FINDER && !roleHeld -> SetupStep.ANDROID
        else -> requested
    }
}
