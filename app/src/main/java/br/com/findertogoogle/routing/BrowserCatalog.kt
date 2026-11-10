package br.com.findertogoogle.routing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri

data class BrowserApp(val packageName: String, val label: String)
data class BrowserTarget(val component: ComponentName, val label: String)

interface BrowserCatalog {
    fun installedBrowsers(): List<BrowserApp>
    fun findBrowser(packageName: String): BrowserApp? = installedBrowsers().firstOrNull { it.packageName == packageName }
    fun resolve(packageName: String, uri: Uri, preferred: ComponentName? = null): BrowserTarget?
}

class AndroidBrowserCatalog(private val context: Context) : BrowserCatalog {
    private val pm = context.packageManager

    @Suppress("DEPRECATION")
    private fun query(uri: Uri, packageName: String? = null): List<ResolveInfo> = try {
        pm.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE).setPackage(packageName),
            PackageManager.MATCH_ALL or PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER)
    } catch (_: RuntimeException) { emptyList() }

    private fun eligible(info: ResolveInfo): ComponentName? = try {
        val activity = info.activityInfo
        if (activity == null || !activity.exported || !activity.enabled ||
            activity.applicationInfo?.enabled != true || activity.packageName == context.packageName ||
            activity.permission?.let { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED } == true
        ) null else ComponentName(activity.packageName, activity.name)
    } catch (_: RuntimeException) { null }

    private fun generic(scheme: String, packageName: String? = null): Map<ComponentName, BrowserTarget> =
        query(Uri.parse("$scheme://example.com/"), packageName).mapNotNull { info ->
            try {
                val component = eligible(info) ?: return@mapNotNull null
                if (packageName != null && component.packageName != packageName) return@mapNotNull null
                val filter = info.filter ?: return@mapNotNull null
                if (!filter.hasDataScheme(scheme) || filter.countDataAuthorities() != 0 ||
                    filter.countDataPaths() != 0 || filter.countDataTypes() != 0 ||
                    filter.countDataSchemeSpecificParts() != 0
                ) return@mapNotNull null
                val label = try { info.loadLabel(pm).toString().ifBlank { component.packageName } }
                    catch (_: RuntimeException) { component.packageName }
                component to BrowserTarget(component, label)
            } catch (_: RuntimeException) { null } // Um candidato defeituoso não apaga os demais.
        }.toMap()

    override fun installedBrowsers(): List<BrowserApp> {
        val httpPackages = generic("http").keys.map { it.packageName }.toSet()
        return generic("https").values.filter { it.component.packageName in httpPackages }
            .distinctBy { it.component.packageName }.map { BrowserApp(it.component.packageName, it.label) }
            .sortedWith(compareBy<BrowserApp> { it.label.lowercase() }.thenBy { it.packageName })
    }

    override fun findBrowser(packageName: String): BrowserApp? {
        if (packageName == context.packageName || generic("http", packageName).isEmpty()) return null
        val target = generic("https", packageName).values.firstOrNull() ?: return null
        return BrowserApp(packageName, target.label)
    }

    override fun resolve(packageName: String, uri: Uri, preferred: ComponentName?): BrowserTarget? {
        if (packageName == context.packageName || !GoogleSearchUrlParser.isWebUri(uri)) return null
        val http = generic("http", packageName)
        val https = generic("https", packageName)
        if (http.isEmpty() || https.isEmpty()) return null
        val genericHandlers = if (uri.scheme.equals("https", true)) https else http
        // A resolução real está limitada ao pacote escolhido; filtros específicos adicionais
        // não desqualificam uma Activity que também possui um filtro genérico.
        val matches = query(uri, packageName).mapNotNull { eligible(it) }
            .filter { it.packageName == packageName && it in genericHandlers }
        val chosen = preferred?.takeIf { it in matches } ?: matches.firstOrNull() ?: return null
        return genericHandlers[chosen]
    }
}
