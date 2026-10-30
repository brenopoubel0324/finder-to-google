package br.com.findertogoogle.ui

import android.content.Context
import br.com.findertogoogle.R
import br.com.findertogoogle.routing.*

object DiagnosticsFormatter {
    fun format(context: Context, origin: LinkOrigin, report: RouteReport): String = with(context) {
        // Por construção, não recebe Intent, URI, termo ou exceção.
        val outcome = when (val result = report.outcome) {
            RouteOutcome.GoogleOpened -> getString(R.string.outcome_google)
            is RouteOutcome.BrowserOpened -> if (result.googleFailure == null) getString(R.string.outcome_browser)
                else getString(R.string.outcome_fallback) + "\n" + getString(result.googleFailure.messageRes())
            is RouteOutcome.Recovery -> getString(R.string.outcome_recovery) + "\n" + getString(result.reason.messageRes()) +
                (result.googleFailure?.let { "\n" + getString(it.messageRes()) } ?: "")
            RouteOutcome.InvalidInput -> getString(R.string.outcome_invalid)
        }
        getString(R.string.diagnostic_format,
            getString(when (origin.kind) {
                OriginKind.FINDER -> R.string.origin_finder
                OriginKind.OTHER -> R.string.origin_other
                OriginKind.UNKNOWN -> R.string.origin_unknown
            }),
            LinkOriginResolver.safePackage(origin.packageName) ?: getString(R.string.unknown),
            getString(when (origin.evidence) {
                IdentityEvidence.SHARED_IDENTITY -> R.string.identity_shared
                IdentityEvidence.CALLING_PACKAGE -> R.string.identity_calling
                IdentityEvidence.NONE -> R.string.identity_none
            }),
            LinkOriginResolver.safePackage(origin.referrerHint) ?: getString(R.string.unknown),
            getString(when (report.decision) {
                RoutingDecision.GOOGLE_FINDER -> R.string.decision_finder
                RoutingDecision.GOOGLE_EXPERIMENTAL -> R.string.decision_experimental
                RoutingDecision.BROWSER_NOT_SEARCH -> R.string.decision_not_search
                RoutingDecision.BROWSER_OTHER_APP -> R.string.decision_other
                RoutingDecision.BROWSER_UNKNOWN_ORIGIN -> R.string.decision_unknown
                RoutingDecision.BROWSER_UNKNOWN_SIGNATURE -> R.string.decision_unknown_signature
                RoutingDecision.INVALID_LINK -> R.string.decision_invalid
            }), outcome,
        )
    }
}
