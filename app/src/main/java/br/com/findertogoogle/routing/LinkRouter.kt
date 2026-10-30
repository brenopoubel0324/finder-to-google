package br.com.findertogoogle.routing

import android.content.Intent
import android.net.Uri

sealed interface RouteOutcome {
    data object GoogleOpened : RouteOutcome
    data class BrowserOpened(val googleFailure: SearchFailure? = null) : RouteOutcome
    data class Recovery(val reason: BrowserFailure, val googleFailure: SearchFailure? = null) : RouteOutcome
    data object InvalidInput : RouteOutcome
}
data class RouteReport(val decision: RoutingDecision, val outcome: RouteOutcome)

class LinkRouter(private val search: SearchOpener, private val browser: BrowserForwarder) {
    fun route(action: String?, uri: Uri?, origin: LinkOrigin, experimental: Boolean,
        clients: Set<String> = GoogleSearchUrlParser.defaultFinderClients): RouteReport {
        if (action != Intent.ACTION_VIEW) return RouteReport(RoutingDecision.INVALID_LINK, RouteOutcome.InvalidInput)
        val decision = RoutingPolicy.decide(uri, origin, experimental, clients)
        if (decision == RoutingDecision.INVALID_LINK || uri == null) return RouteReport(decision, RouteOutcome.InvalidInput)
        var failure: SearchFailure? = null
        if (decision.usesGoogle) {
            when (val result = search.open(requireNotNull(GoogleSearchUrlParser.extractQuery(uri)))) {
                SearchLaunchResult.Launched -> return RouteReport(decision, RouteOutcome.GoogleOpened)
                is SearchLaunchResult.Failed -> failure = result.reason
            }
        }
        return RouteReport(decision, when (val result = browser.open(uri)) {
            BrowserResult.Opened -> RouteOutcome.BrowserOpened(failure)
            is BrowserResult.NeedsRecovery -> RouteOutcome.Recovery(result.reason, failure)
        })
    }
}
