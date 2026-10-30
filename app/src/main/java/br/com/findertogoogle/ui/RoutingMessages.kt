package br.com.findertogoogle.ui

import br.com.findertogoogle.R
import br.com.findertogoogle.routing.BrowserFailure
import br.com.findertogoogle.routing.SearchFailure

internal fun BrowserFailure.messageRes(): Int = when (this) {
    BrowserFailure.NOT_CONFIGURED -> R.string.recovery_not_configured
    BrowserFailure.NO_LONGER_AVAILABLE -> R.string.recovery_missing
    BrowserFailure.LAUNCH_FAILED -> R.string.recovery_failed
    BrowserFailure.INVALID_LINK -> R.string.invalid_link
    BrowserFailure.REENTRY_LIMIT -> R.string.recovery_reentry
}
internal fun SearchFailure.messageRes(): Int = when (this) {
    SearchFailure.UNAVAILABLE -> R.string.search_unavailable
    SearchFailure.ACCESS_DENIED -> R.string.search_denied
    SearchFailure.INVALID_REQUEST -> R.string.search_invalid
}
