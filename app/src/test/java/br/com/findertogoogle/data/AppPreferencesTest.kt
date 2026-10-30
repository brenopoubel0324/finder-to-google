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
        original.selectedBrowser = component
        original.experimentalFinderDetection = true
        val recreated = AppPreferences(context)
        assertEquals(component, recreated.selectedBrowser)
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
}
