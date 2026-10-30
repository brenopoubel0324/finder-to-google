package br.com.findertogoogle

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.findertogoogle.routing.AndroidBrowserCatalog
import br.com.findertogoogle.routing.GoogleSearchUrlParser
import br.com.findertogoogle.ui.SettingsActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Executar em Android 16 real/emulador. Não altera papel padrão nem desabilita outros apps. */
@RunWith(AndroidJUnit4::class)
class DeviceContractTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun uriRealDoAndroidDecodificaUmaVez() {
        assertEquals("C++ Android & %26 😀", GoogleSearchUrlParser.extractQuery(
            Uri.parse("https://www.google.com/search?q=C%2B%2B+Android+%26+%2526+%F0%9F%98%80"),
        ))
    }

    @Suppress("DEPRECATION")
    @Test fun filtroGenericoElegivelParaNavegador() {
        listOf("http", "https").forEach { scheme ->
            val matches = context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com/"))
                    .addCategory(Intent.CATEGORY_BROWSABLE),
                PackageManager.MATCH_ALL or PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER,
            )
            assertTrue("Filtro genérico $scheme ausente", matches.any {
                it.activityInfo.packageName == context.packageName &&
                    it.filter?.hasDataScheme(scheme) == true && it.filter.countDataAuthorities() == 0
            })
        }
        assertTrue(context.getSystemService(RoleManager::class.java).isRoleAvailable(RoleManager.ROLE_BROWSER))
    }

    @Test fun catalogoNuncaIncluiRoteador() {
        assertTrue(AndroidBrowserCatalog(context).installedBrowsers().none { it.packageName == context.packageName })
    }

    @Test fun telaExibeEstadoRealDoSistema() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val held = activity.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_BROWSER)
                val expected = activity.getString(if (held) R.string.status_active else R.string.status_inactive)
                assertEquals(expected, activity.findViewById<TextView>(R.id.status).text.toString())
            }
        }
    }
}
