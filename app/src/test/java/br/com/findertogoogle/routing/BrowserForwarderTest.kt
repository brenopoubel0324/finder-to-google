package br.com.findertogoogle.routing

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrowserForwarderTest {
    private val selected = MemorySelection()
    private val real = BrowserTarget(ComponentName("org.browser", "org.browser.Main"), "Navegador")
    private val self = BrowserTarget(ComponentName("br.com.findertogoogle", "br.com.findertogoogle.Router"), "Roteador")
    private val catalog = TestBrowsers(listOf(real, self))
    private val started = mutableListOf<Intent>()
    private val forwarder = BrowserForwarder(self.component.packageName, selected,
        catalog, ActivityStarter { started += it }, ForwardingGuard { 0L })
    private val uri = Uri.parse("https://example.com/p?a=1&b=%2526+%2B#fragmento")

    @Test fun `encaminha explicitamente preservando a URL inteira`() {
        selected.selectBrowser(real.component)
        assertEquals(BrowserResult.Opened, forwarder.open(uri))
        val intent = started.single()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(real.component, intent.component)
        assertEquals(uri.toString(), intent.dataString)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertNull(intent.extras)
        assertNull(intent.clipData)
    }

    @Test fun `nunca lista seleciona ou abre o proprio roteador`() {
        assertEquals(listOf(BrowserApp(real.component.packageName, real.label)), forwarder.availableBrowsers())
        assertFalse(forwarder.select(BrowserApp(self.component.packageName, self.label)))
        selected.selectBrowser(self.component)
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE), forwarder.open(uri))
        assertTrue(started.isEmpty())
    }

    @Test fun `sem escolha exige recuperacao sem abertura implicita`() {
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.NOT_CONFIGURED), forwarder.open(uri))
        assertTrue(started.isEmpty())
    }

    @Test fun `desinstalacao ou desativacao nao troca para outro pacote sozinha`() {
        selected.selectBrowser(ComponentName("org.removido", "org.removido.Main"))
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE), forwarder.open(uri))
        assertTrue(started.isEmpty())
    }

    @Test fun `atualizacao pode alterar activity dentro do mesmo pacote`() {
        selected.selectBrowser(ComponentName(real.component.packageName, "org.browser.Antiga"))
        assertEquals(BrowserResult.Opened, forwarder.open(uri))
        assertEquals(real.component, started.single().component)
        assertEquals(real.component, selected.preferredBrowserComponent)
    }

    @Test fun `remocao entre listar e selecionar nao salva escolha invalida`() {
        catalog.targets = emptyList()
        assertFalse(forwarder.select(BrowserApp(real.component.packageName, real.label)))
        assertNull(selected.preferredBrowserComponent)
    }

    @Test fun `falha ao iniciar recupera sem tentar implicitamente`() {
        selected.selectBrowser(real.component)
        listOf(ActivityNotFoundException(), SecurityException(), IllegalArgumentException()).forEach { failure ->
            var attempts = 0
            val failing = BrowserForwarder(self.component.packageName, selected, catalog, ActivityStarter {
                attempts++
                assertEquals(real.component, it.component)
                throw failure
            })
            assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.LAUNCH_FAILED), failing.open(uri))
            assertEquals(1, attempts)
        }
    }

    @Test fun `esquema perigoso nao inicia navegador`() {
        selected.selectBrowser(real.component)
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.INVALID_LINK), forwarder.open(Uri.parse("intent://example.com")))
        assertTrue(started.isEmpty())
    }
    @Test fun `quarta reentrada recupera e retry manual inicia nova janela`() {
        selected.selectBrowser(real.component)
        repeat(3) { assertEquals(BrowserResult.Opened, forwarder.open(uri)) }
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.REENTRY_LIMIT), forwarder.open(uri))
        assertEquals(3, started.size)
        assertEquals(BrowserResult.Opened, forwarder.retry(uri))
    }

    @Test fun `resolucao respeita uri e rejeita catalogo que devolve outro pacote`() {
        selected.selectBrowser(real.component)
        val http = real.copy(component = ComponentName(real.component.packageName, "org.browser.Http"))
        catalog.resolver = { _, link, _ -> if (link.scheme == "http") http else real }
        assertEquals(BrowserResult.Opened, forwarder.open(Uri.parse("http://example.com/a")))
        assertEquals(http.component, started.last().component)
        catalog.resolver = { _, _, _ -> self }
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE), forwarder.open(uri))
    }

}
