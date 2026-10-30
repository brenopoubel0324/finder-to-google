package br.com.findertogoogle.routing

import android.app.Activity
import android.os.Build

enum class OriginKind { FINDER, OTHER, UNKNOWN }
enum class IdentityEvidence { SHARED_IDENTITY, CALLING_PACKAGE, NONE }

data class LinkOrigin(
    val kind: OriginKind,
    val packageName: String? = null,
    val evidence: IdentityEvidence = IdentityEvidence.NONE,
    val referrerHint: String? = null,
)

object LinkOriginResolver {
    const val FINDER_PACKAGE = "com.samsung.android.app.galaxyfinder"
    private val packagePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")

    fun resolve(activity: Activity): LinkOrigin {
        val shared = if (Build.VERSION.SDK_INT >= 34) {
            try { activity.launchedFromPackage } catch (_: RuntimeException) { null }
        } else null
        val calling = try { activity.callingPackage } catch (_: RuntimeException) { null }
        // getReferrer pode vir de EXTRA_REFERRER e não comprova a identidade.
        val hint = try {
            activity.referrer?.takeIf { it.scheme == "android-app" }?.host
        } catch (_: RuntimeException) { null }
        return fromEvidence(shared, calling, hint)
    }

    internal fun fromEvidence(shared: String?, calling: String?, hint: String?): LinkOrigin {
        val known = safePackage(shared) ?: safePackage(calling)
        val evidence = when {
            safePackage(shared) != null -> IdentityEvidence.SHARED_IDENTITY
            safePackage(calling) != null -> IdentityEvidence.CALLING_PACKAGE
            else -> IdentityEvidence.NONE
        }
        return LinkOrigin(
            when (known) {
                null -> OriginKind.UNKNOWN
                FINDER_PACKAGE -> OriginKind.FINDER
                else -> OriginKind.OTHER
            }, known, evidence, safePackage(hint),
        )
    }

    private fun safePackage(value: String?): String? = value?.takeIf {
        it.length <= 200 && packagePattern.matches(it)
    }
}
