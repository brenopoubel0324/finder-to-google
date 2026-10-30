package br.com.findertogoogle.routing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import br.com.findertogoogle.data.BrowserSelection

enum class BrowserFailure { NOT_CONFIGURED, NO_LONGER_AVAILABLE, LAUNCH_FAILED, INVALID_LINK }

sealed interface BrowserResult {
    data object Opened : BrowserResult
    data class NeedsRecovery(val reason: BrowserFailure) : BrowserResult
}

class BrowserForwarder(
    private val selfPackage: String,
    private val selection: BrowserSelection,
    private val catalog: BrowserCatalog,
    private val starter: ActivityStarter,
) {
    fun availableBrowsers(): List<BrowserTarget> = catalog.installedBrowsers()
        .filter { it.component.packageName != selfPackage }

    fun selectedTarget(): BrowserTarget? {
        val selected = selection.selectedBrowser ?: return null
        if (selected.packageName == selfPackage) return null
        val available = availableBrowsers()
        // Após uma atualização, a Activity pode mudar; manter o mesmo pacote autorizado.
        return available.firstOrNull { it.component == selected }
            ?: available.firstOrNull { it.component.packageName == selected.packageName }
    }

    fun select(target: BrowserTarget): Boolean {
        if (availableBrowsers().none { it.component == target.component }) return false
        selection.selectedBrowser = target.component
        return true
    }

    fun open(uri: Uri): BrowserResult {
        if (!GoogleSearchUrlParser.isWebUri(uri)) {
            return BrowserResult.NeedsRecovery(BrowserFailure.INVALID_LINK)
        }
        if (selection.selectedBrowser == null) {
            return BrowserResult.NeedsRecovery(BrowserFailure.NOT_CONFIGURED)
        }
        val target = selectedTarget()
            ?: return BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE)
        return try {
            // Intent novo: nunca propagar component, extras, ClipData ou flags do chamador.
            starter.start(Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                component = target.component
            })
            selection.selectedBrowser = target.component
            BrowserResult.Opened
        } catch (_: ActivityNotFoundException) {
            BrowserResult.NeedsRecovery(BrowserFailure.LAUNCH_FAILED)
        } catch (_: SecurityException) {
            BrowserResult.NeedsRecovery(BrowserFailure.LAUNCH_FAILED)
        } catch (_: IllegalArgumentException) {
            BrowserResult.NeedsRecovery(BrowserFailure.LAUNCH_FAILED)
        }
    }
}
