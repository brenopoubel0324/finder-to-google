package br.com.findertogoogle.routing

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import br.com.findertogoogle.data.BrowserSelection
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
    private var available = listOf(real, self)
    private val started = mutableListOf<Intent>()
    private val forwarder = BrowserForwarder(self.component.packageName, selected,
        BrowserCatalog { available }, ActivityStarter { started += it })
    private val uri = Uri.parse("https://example.com/p?a=1&b=%2526+%2B#fragmento")

    @Test fun `encaminha explicitamente preservando a URL inteira`() {
        selected.selectedBrowser = real.component
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
        assertEquals(listOf(real), forwarder.availableBrowsers())
        assertFalse(forwarder.select(self))
        selected.selectedBrowser = self.component
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE), forwarder.open(uri))
        assertTrue(started.isEmpty())
    }

    @Test fun `sem escolha exige recuperacao sem abertura implicita`() {
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.NOT_CONFIGURED), forwarder.open(uri))
        assertTrue(started.isEmpty())
    }

    @Test fun `desinstalacao ou desativacao nao troca para outro pacote sozinha`() {
        selected.selectedBrowser = ComponentName("org.removido", "org.removido.Main")
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.NO_LONGER_AVAILABLE), forwarder.open(uri))
        assertTrue(started.isEmpty())
    }

    @Test fun `atualizacao pode alterar activity dentro do mesmo pacote`() {
        selected.selectedBrowser = ComponentName(real.component.packageName, "org.browser.Antiga")
        assertEquals(BrowserResult.Opened, forwarder.open(uri))
        assertEquals(real.component, started.single().component)
        assertEquals(real.component, selected.selectedBrowser)
    }

    @Test fun `remocao entre listar e selecionar nao salva escolha invalida`() {
        available = emptyList()
        assertFalse(forwarder.select(real))
        assertNull(selected.selectedBrowser)
    }

    @Test fun `falha ao iniciar recupera sem tentar implicitamente`() {
        selected.selectedBrowser = real.component
        listOf(ActivityNotFoundException(), SecurityException(), IllegalArgumentException()).forEach { failure ->
            var attempts = 0
            val failing = BrowserForwarder(self.component.packageName, selected, BrowserCatalog { available }, ActivityStarter {
                attempts++
                assertEquals(real.component, it.component)
                throw failure
            })
            assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.LAUNCH_FAILED), failing.open(uri))
            assertEquals(1, attempts)
        }
    }

    @Test fun `esquema perigoso nao inicia navegador`() {
        selected.selectedBrowser = real.component
        assertEquals(BrowserResult.NeedsRecovery(BrowserFailure.INVALID_LINK), forwarder.open(Uri.parse("intent://example.com")))
        assertTrue(started.isEmpty())
    }
}

internal class MemorySelection : BrowserSelection { override var selectedBrowser: ComponentName? = null }
