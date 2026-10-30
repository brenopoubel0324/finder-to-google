package br.com.findertogoogle.routing

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import br.com.findertogoogle.data.BrowserSelection

enum class BrowserFailure { NOT_CONFIGURED, NO_LONGER_AVAILABLE, LAUNCH_FAILED, INVALID_LINK, REENTRY_LIMIT }
sealed interface BrowserResult {
    data object Opened : BrowserResult
    data class NeedsRecovery(val reason: BrowserFailure) : BrowserResult
}

class BrowserForwarder(
    private val selfPackage: String,
    private val selection: BrowserSelection,
    private val catalog: BrowserCatalog,
    private val starter: ActivityStarter,
    private val guard: ForwardingGuard = ForwardingGuard.process,
) {
    fun availableBrowsers(): List<BrowserApp> = catalog.installedBrowsers().filter { it.packageName != selfPackage }
    fun selectedTarget(): BrowserApp? = availableBrowsers().firstOrNull { it.packageName == selection.selectedBrowserPackage }
    fun hasSelection(): Boolean = selection.selectedBrowserPackage != null

    fun select(app: BrowserApp): Boolean {
        if (availableBrowsers().none { it.packageName == app.packageName }) return false
        val target = catalog.resolve(app.packageName, Uri.parse("https://example.com/")) ?: return false
        if (target.component.packageName != app.packageName || app.packageName == selfPackage) return false
        selection.selectBrowser(target.component)
        return true
    }

    /** Apenas callbacks locais de interação devem chamar retry; extras de entrada não o acionam. */
    fun retry(uri: Uri): BrowserResult {
        selection.selectedBrowserPackage?.let { guard.reset(uri, it) }
        return open(uri)
    }

    fun open(uri: Uri): BrowserResult {
        if (!GoogleSearchUrlParser.isWebUri(uri)) return BrowserResult.NeedsRecovery(BrowserFailure.INVALID_LINK)
        val pkg = selection.selectedBrowserPackage ?: return BrowserResult.NeedsRecovery(BrowserFailure.NOT_CONFIGURED)
        if (pkg == selfPackage) return BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE)
        val target = catalog.resolve(pkg, uri, selection.preferredBrowserComponent)
            ?: return BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE)
        if (target.component.packageName != pkg || target.component.packageName == selfPackage) {
            return BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE)
        }
        val ticket = guard.reserve(uri, pkg) ?: return BrowserResult.NeedsRecovery(BrowserFailure.REENTRY_LIMIT)
        return try {
            // Intent novo: não propagar extras, flags ou ClipData do chamador.
            starter.start(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).apply {
                component = target.component
            })
            selection.selectBrowser(target.component)
            BrowserResult.Opened
        } catch (_: ActivityNotFoundException) { failed(ticket)
        } catch (_: SecurityException) { failed(ticket)
        } catch (_: IllegalArgumentException) { failed(ticket) }
    }

    private fun failed(ticket: ForwardingGuard.Reservation): BrowserResult {
        guard.release(ticket)
        return BrowserResult.NeedsRecovery(BrowserFailure.LAUNCH_FAILED)
    }
}
