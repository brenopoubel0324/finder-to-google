package br.com.findertogoogle.data

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clear() {
        context.getSharedPreferences("router_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `configuracao persiste sem mudar papel do Android`() {
        val original = AppPreferences(context)
        val component = ComponentName("org.browser", "org.browser.Main")
        original.selectBrowser(component)
        original.experimentalFinderDetection = true
        val recreated = AppPreferences(context)
        assertEquals(component, recreated.preferredBrowserComponent)
        assertTrue(recreated.experimentalFinderDetection)
    }

    @Test fun `padroes conservadores e diagnostico exige opt in`() {
        val prefs = AppPreferences(context)
        assertFalse(prefs.experimentalFinderDetection)
        assertFalse(prefs.diagnosticsEnabled)
        prefs.recordDiagnostic("Origem desconhecida")
        assertNull(prefs.lastDiagnostic)
        prefs.diagnosticsEnabled = true
        prefs.recordDiagnostic("Navegador aberto")
        assertEquals("Navegador aberto", prefs.lastDiagnostic)
        prefs.recordDiagnostic("Google App aberto")
        assertEquals("Google App aberto", prefs.lastDiagnostic)
        prefs.diagnosticsEnabled = false
        assertNull(AppPreferences(context).lastDiagnostic)
    }
    @Test fun `migra componente ausente do aparelho preservando flags e diagnostico`() {
        val raw = context.getSharedPreferences("router_settings", Context.MODE_PRIVATE)
        raw.edit().putString("browser_component", "org.removido/org.removido.Old")
            .putBoolean("experimental_finder", true).putBoolean("diagnostics_enabled", true)
            .putString("last_diagnostic", "Registro anterior").commit()
        repeat(2) {
            val prefs = AppPreferences(context)
            assertEquals("org.removido", prefs.selectedBrowserPackage)
            assertTrue(prefs.experimentalFinderDetection)
            assertTrue(prefs.diagnosticsEnabled)
            assertEquals("Registro anterior", prefs.lastDiagnostic)
        }
        assertEquals("org.removido", raw.getString("browser_package", null))
        assertEquals("org.removido/org.removido.Old", raw.getString("browser_component", null))
    }

    @Test fun `pacote novo tem precedencia mesmo se desinstalado`() {
        context.getSharedPreferences("router_settings", Context.MODE_PRIVATE).edit()
            .putString("browser_package", "org.novo").putString("browser_component", "org.antigo/org.antigo.Main").commit()
        val prefs = AppPreferences(context)
        assertEquals("org.novo", prefs.selectedBrowserPackage)
        assertNull(prefs.preferredBrowserComponent)
    }

    @Test fun `valor invalido recupera legado e proprio pacote nao vira destino`() {
        val raw = context.getSharedPreferences("router_settings", Context.MODE_PRIVATE)
        raw.edit().putString("browser_package", "invalido?").putString("browser_component", "org.legado/org.legado.Main").commit()
        assertEquals("org.legado", AppPreferences(context).selectedBrowserPackage)
        raw.edit().clear().putString("browser_package", context.packageName)
            .putString("browser_component", context.packageName + "/.ui.LinkRouterActivity").commit()
        assertNull(AppPreferences(context).selectedBrowserPackage)
    }

    @Test fun `escolha nova grava pacote e componente coerentes`() {
        val prefs = AppPreferences(context)
        prefs.selectBrowser(ComponentName("org.primeiro", "org.primeiro.Http"))
        prefs.selectBrowser(ComponentName("org.segundo", "org.segundo.Https"))
        val recreated = AppPreferences(context)
        assertEquals("org.segundo", recreated.selectedBrowserPackage)
        assertEquals(ComponentName("org.segundo", "org.segundo.Https"), recreated.preferredBrowserComponent)
        assertFalse(recreated.experimentalFinderDetection)
    }

}
