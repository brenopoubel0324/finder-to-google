package br.com.findertogoogle.routing

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
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

    @Test fun `catalogo exige filtros genericos em ambos esquemas e exclui activities desabilitadas`() {
        register("org.browser.real", generic = true)
        register("org.google.maps", generic = false)
        register(context.packageName, generic = true)
        register("org.browser.disabled", generic = true, enabled = false)
        register("org.browser.private", generic = true, exported = false)
        register("org.browser.httpsonly", generic = true, schemes = listOf("https"))
        assertEquals(listOf("org.browser.real"), AndroidBrowserCatalog(context).installedBrowsers().map { it.component.packageName })
    }

    private fun register(
        pkg: String, generic: Boolean, enabled: Boolean = true,
        exported: Boolean = true, schemes: List<String> = listOf("http", "https"),
    ) {
        val info = ResolveInfo().apply {
            filter = IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addCategory(Intent.CATEGORY_BROWSABLE)
                schemes.forEach { addDataScheme(it) }
                if (!generic) addDataAuthority("maps.google.com", null)
            }
            nonLocalizedLabel = pkg
            activityInfo = ActivityInfo().apply {
                packageName = pkg
                name = "$pkg.Main"
                this.enabled = enabled
                this.exported = exported
                applicationInfo = ApplicationInfo().apply { packageName = pkg; this.enabled = enabled }
            }
        }
        schemes.forEach {
            shadowOf(context.packageManager).addResolveInfoForIntent(
                Intent(Intent.ACTION_VIEW, Uri.parse("$it://example.com/")).addCategory(Intent.CATEGORY_BROWSABLE), info,
            )
        }
    }
}
