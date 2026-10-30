package br.com.findertogoogle.routing

import android.net.Uri

enum class RoutingDecision {
    GOOGLE_FINDER, GOOGLE_EXPERIMENTAL, BROWSER_NOT_SEARCH, BROWSER_OTHER_APP,
    BROWSER_UNKNOWN_ORIGIN, BROWSER_UNKNOWN_SIGNATURE, INVALID_LINK;

    val usesGoogle: Boolean get() = this == GOOGLE_FINDER || this == GOOGLE_EXPERIMENTAL
}

object RoutingPolicy {
    fun decide(uri: Uri?, origin: LinkOrigin, experimental: Boolean,
        clients: Set<String> = GoogleSearchUrlParser.defaultFinderClients): RoutingDecision {
        if (!GoogleSearchUrlParser.isWebUri(uri)) return RoutingDecision.INVALID_LINK
        if (GoogleSearchUrlParser.extractQuery(uri) == null) return RoutingDecision.BROWSER_NOT_SEARCH
        return when (origin.kind) {
            OriginKind.FINDER -> RoutingDecision.GOOGLE_FINDER
            OriginKind.OTHER -> RoutingDecision.BROWSER_OTHER_APP
            OriginKind.UNKNOWN -> when {
                !experimental -> RoutingDecision.BROWSER_UNKNOWN_ORIGIN
                uri != null && GoogleSearchUrlParser.hasFinderSignature(uri, clients) -> RoutingDecision.GOOGLE_EXPERIMENTAL
                else -> RoutingDecision.BROWSER_UNKNOWN_SIGNATURE
            }
        }
    }
}
