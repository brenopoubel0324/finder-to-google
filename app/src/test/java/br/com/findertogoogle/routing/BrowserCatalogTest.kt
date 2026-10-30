package br.com.findertogoogle.routing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrowserCatalogTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val shadowPm = shadowOf(context.packageManager)
    private val catalog = AndroidBrowserCatalog(context)

    private fun register(pkg: String, name: String = "Main", schemes: List<String> = listOf("http", "https"),
        generic: Boolean = true, enabled: Boolean = true, exported: Boolean = true): ComponentName {
        val component = ComponentName(pkg, "$pkg.$name")
        val info = shadowPm.addActivityIfNotPresent(component)
        info.enabled = enabled
        info.exported = exported
        info.applicationInfo = ApplicationInfo().apply { packageName = pkg; this.enabled = enabled }
        info.nonLocalizedLabel = pkg
        shadowPm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_VIEW).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            schemes.forEach { addDataScheme(it) }
            if (!generic) addDataAuthority("maps.google.com", null)
        })
        return component
    }

    @Test fun `exige genericos nos dois esquemas e exclui destinos invalidos`() {
        register("org.browser.real")
        register("org.google.maps", generic = false)
        register(context.packageName)
        register("org.browser.disabled", enabled = false)
        register("org.browser.private", exported = false)
        register("org.browser.httpsonly", schemes = listOf("https"))
        assertEquals(listOf("org.browser.real"), catalog.installedBrowsers().map { it.packageName })
    }

    @Test fun `atividades distintas por esquema continuam sendo um navegador e resolvem URI real`() {
        val http = register("org.browser.split", "Http", listOf("http"))
        val https = register("org.browser.split", "Https", listOf("https"))
        assertEquals(1, catalog.installedBrowsers().count { it.packageName == "org.browser.split" })
        assertEquals(http, catalog.resolve("org.browser.split", Uri.parse("http://example.com/p?a=1#x"), https)?.component)
        assertEquals(https, catalog.resolve("org.browser.split", Uri.parse("https://example.com/p?q=%2526"), http)?.component)
        assertNull(catalog.resolve(context.packageName, Uri.parse("https://example.com/")))
        assertNull(catalog.resolve("org.browser.split", Uri.parse("file:///tmp/arquivo")))
    }

    @Suppress("DEPRECATION")
    @Test fun `rotulo quebrado nao elimina os outros navegadores`() {
        val good = register("org.browser.good")
        val broken = register("org.browser.broken")
        val candidate = object : ResolveInfo() {
            override fun loadLabel(pm: PackageManager): CharSequence { throw IllegalStateException("Sem recursos") }
        }.apply {
            activityInfo = context.packageManager.getActivityInfo(broken, 0)
            filter = IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT); addCategory(Intent.CATEGORY_BROWSABLE)
                addDataScheme("http"); addDataScheme("https")
            }
        }
        listOf("http", "https").forEach { scheme ->
            shadowPm.addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com/"))
                .addCategory(Intent.CATEGORY_BROWSABLE), candidate)
        }
        assertTrue(catalog.installedBrowsers().any { it.packageName == good.packageName })
    }
}
