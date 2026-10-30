package br.com.findertogoogle.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.data.AppPreferences
import br.com.findertogoogle.routing.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DiagnosticClipboardTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clipboard get() = context.getSystemService(ClipboardManager::class.java)
    @Before fun clear() {
        context.getSharedPreferences("router_settings", Context.MODE_PRIVATE).edit().clear().commit()
        clipboard.setPrimaryClip(ClipData.newPlainText("anterior", "conteúdo anterior"))
    }
    @Test fun `sem opt in nao altera clipboard e desligar remove registro`() {
        val prefs = AppPreferences(context)
        prefs.recordDiagnostic("não gravar")
        assertFalse(DiagnosticClipboard.copyIfEnabled(context, prefs))
        assertEquals("conteúdo anterior", clipboard.primaryClip?.getItemAt(0)?.text.toString())
        prefs.diagnosticsEnabled = true
        prefs.recordDiagnostic("metadados")
        assertTrue(DiagnosticClipboard.copyIfEnabled(context, prefs))
        assertTrue(clipboard.primaryClip?.getItemAt(0)?.text.toString().contains("metadados"))
        prefs.diagnosticsEnabled = false
        assertNull(prefs.lastDiagnostic)
        assertFalse(DiagnosticClipboard.copyIfEnabled(context, prefs))
    }
    @Test fun `todas falhas tipadas sao saneadas e sem dados de busca`() {
        val prefs = AppPreferences(context).apply { diagnosticsEnabled = true }
        SearchFailure.entries.forEach { failure ->
            val report = DiagnosticsFormatter.format(context,
                LinkOrigin(OriginKind.UNKNOWN, "https://query-canary.invalid", referrerHint = "https://url-canary.invalid/search?q=PRIVATE"),
                RouteReport(RoutingDecision.GOOGLE_EXPERIMENTAL, RouteOutcome.Recovery(BrowserFailure.LAUNCH_FAILED, failure)))
            prefs.recordDiagnostic(report)
            assertTrue(DiagnosticClipboard.copyIfEnabled(context, prefs))
            val text = clipboard.primaryClip?.getItemAt(0)?.text.toString()
            listOf("query-canary", "url-canary", "PRIVATE", "https://").forEach { assertFalse(text.contains(it)) }
            assertTrue(clipboard.primaryClip?.description?.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true)
        }
    }
}
