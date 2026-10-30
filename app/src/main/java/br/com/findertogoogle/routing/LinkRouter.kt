package br.com.findertogoogle.routing

import android.content.Intent
import android.net.Uri

sealed interface RouteOutcome {
    data object GoogleOpened : RouteOutcome
    data class BrowserOpened(val googleFailed: Boolean) : RouteOutcome
    data class Recovery(val reason: BrowserFailure, val googleFailed: Boolean) : RouteOutcome
    data object InvalidInput : RouteOutcome
}

data class RouteReport(val decision: RoutingDecision, val outcome: RouteOutcome)

class LinkRouter(private val search: SearchOpener, private val browser: BrowserForwarder) {
    fun route(action: String?, uri: Uri?, origin: LinkOrigin, experimental: Boolean): RouteReport {
        if (action != Intent.ACTION_VIEW) {
            return RouteReport(RoutingDecision.INVALID_LINK, RouteOutcome.InvalidInput)
        }
        val decision = RoutingPolicy.decide(uri, origin, experimental)
        if (decision == RoutingDecision.INVALID_LINK || uri == null) {
            return RouteReport(decision, RouteOutcome.InvalidInput)
        }
        if (decision.usesGoogle && search.open(requireNotNull(GoogleSearchUrlParser.extractQuery(uri)))) {
            return RouteReport(decision, RouteOutcome.GoogleOpened)
        }
        return RouteReport(decision, when (val result = browser.open(uri)) {
            BrowserResult.Opened -> RouteOutcome.BrowserOpened(googleFailed = decision.usesGoogle)
            is BrowserResult.NeedsRecovery -> RouteOutcome.Recovery(result.reason, googleFailed = decision.usesGoogle)
        })
    }
}
