package br.com.findertogoogle.ui

import android.content.Context
import br.com.findertogoogle.R
import br.com.findertogoogle.routing.*

object DiagnosticsFormatter {
    fun format(context: Context, origin: LinkOrigin, report: RouteReport): String = with(context) {
        // A assinatura não recebe Uri nem termo de busca, por construção.
        getString(R.string.diagnostic_format,
            getString(when (origin.kind) {
                OriginKind.FINDER -> R.string.origin_finder
                OriginKind.OTHER -> R.string.origin_other
                OriginKind.UNKNOWN -> R.string.origin_unknown
            }),
            origin.packageName ?: getString(R.string.unknown),
            getString(when (origin.evidence) {
                IdentityEvidence.SHARED_IDENTITY -> R.string.identity_shared
                IdentityEvidence.CALLING_PACKAGE -> R.string.identity_calling
                IdentityEvidence.NONE -> R.string.identity_none
            }),
            origin.referrerHint ?: getString(R.string.unknown),
            getString(when (report.decision) {
                RoutingDecision.GOOGLE_FINDER -> R.string.decision_finder
                RoutingDecision.GOOGLE_EXPERIMENTAL -> R.string.decision_experimental
                RoutingDecision.BROWSER_NOT_SEARCH -> R.string.decision_not_search
                RoutingDecision.BROWSER_OTHER_APP -> R.string.decision_other
                RoutingDecision.BROWSER_UNKNOWN_ORIGIN -> R.string.decision_unknown
                RoutingDecision.INVALID_LINK -> R.string.decision_invalid
            }),
            getString(when (val outcome = report.outcome) {
                RouteOutcome.GoogleOpened -> R.string.outcome_google
                is RouteOutcome.BrowserOpened -> if (outcome.googleFailed) R.string.outcome_fallback else R.string.outcome_browser
                is RouteOutcome.Recovery -> R.string.outcome_recovery
                RouteOutcome.InvalidInput -> R.string.outcome_invalid
            }),
        )
    }
}
