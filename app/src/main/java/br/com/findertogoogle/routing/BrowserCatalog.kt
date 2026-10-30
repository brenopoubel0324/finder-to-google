package br.com.findertogoogle.routing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class BrowserTarget(val component: ComponentName, val label: String)

fun interface BrowserCatalog { fun installedBrowsers(): List<BrowserTarget> }

class AndroidBrowserCatalog(private val context: Context) : BrowserCatalog {
    @Suppress("DEPRECATION")
    override fun installedBrowsers(): List<BrowserTarget> {
        val pm = context.packageManager
        // A interseção exige suporte genérico tanto a HTTP quanto a HTTPS.
        fun query(scheme: String): Map<ComponentName, BrowserTarget> = try {
            pm.queryIntentActivities(
                Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com/"))
                    .addCategory(Intent.CATEGORY_BROWSABLE),
                PackageManager.MATCH_ALL or PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER,
            ).mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val filter = info.filter ?: return@mapNotNull null
                // handleAllWebDataURI é interno ao framework. Usar somente o IntentFilter público:
                // um navegador genérico não limita host, caminho ou MIME type.
                val generic = filter.hasDataScheme(scheme) && filter.countDataAuthorities() == 0 &&
                    filter.countDataPaths() == 0 && filter.countDataTypes() == 0
                if (!generic || !activity.exported || !activity.enabled ||
                    !activity.applicationInfo.enabled || activity.packageName == context.packageName ||
                    activity.permission?.let {
                        context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
                    } == true
                ) return@mapNotNull null
                val component = ComponentName(activity.packageName, activity.name)
                component to BrowserTarget(component, info.loadLabel(pm).toString())
            }.toMap()
        } catch (_: RuntimeException) { emptyMap() }

        val http = query("http")
        return query("https").filterKeys { it in http }.values
            .distinctBy { it.component }
            .sortedWith(compareBy<BrowserTarget> { it.label.lowercase() }
                .thenBy { it.component.flattenToString() })
    }
}
