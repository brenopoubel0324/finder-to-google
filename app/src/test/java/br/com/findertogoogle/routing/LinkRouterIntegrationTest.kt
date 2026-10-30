package br.com.findertogoogle.routing

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Integra componentes reais, substituindo somente catálogo de pacotes e startActivity. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LinkRouterIntegrationTest {
    private val real = BrowserTarget(ComponentName("org.vivaldi.browser", "org.vivaldi.browser.Main"), "Vivaldi")
    private val selection = MemorySelection().apply { selectBrowser(real.component) }
    private val intents = mutableListOf<Intent>()
    private var googleFailure: RuntimeException? = null
    private val catalog = TestBrowsers(listOf(real))
    private val starter = ActivityStarter {
        intents += it
        if (it.action == Intent.ACTION_WEB_SEARCH) googleFailure?.let { failure -> throw failure }
    }
    private val browser = BrowserForwarder("br.com.findertogoogle", selection, catalog, starter, ForwardingGuard { 0L })
    private val router = LinkRouter(SearchLauncher(starter), browser)
    private val finder = LinkOrigin(OriginKind.FINDER)

    @Test fun `Finder abre pesquisa nativa com pacote e query corretos`() {
        val report = route("https://www.google.com/search?client=tablet-android-samsung-rvo1&hl=pt&q=C%2B%2B+Android")
        assertEquals(RouteOutcome.GoogleOpened, report.outcome)
        val intent = intents.single()
        assertEquals(Intent.ACTION_WEB_SEARCH, intent.action)
        assertEquals(SearchLauncher.GOOGLE_PACKAGE, intent.`package`)
        assertEquals("C++ Android", intent.getStringExtra(SearchManager.QUERY))
        assertNull(intent.data)
    }

    @Test fun `Google ausente preserva URL original no fallback`() {
        googleFailure = ActivityNotFoundException()
        val url = "https://www.google.com/search?hl=pt&q=ol%C3%A1%20mundo&x=%2526#abc"
        assertEquals(RouteOutcome.BrowserOpened(SearchFailure.UNAVAILABLE), route(url).outcome)
        assertEquals(2, intents.size)
        assertEquals(url, intents.last().dataString)
        assertEquals(real.component, intents.last().component)
    }

    @Test fun `Google bloqueado tambem usa fallback`() {
        googleFailure = SecurityException()
        assertEquals(RouteOutcome.BrowserOpened(SearchFailure.ACCESS_DENIED), route("https://google.com/search?q=x").outcome)
        assertEquals(real.component, intents.last().component)
    }

    @Test fun `aceitacao links comuns falsos e sem q preserva URL exata`() {
        listOf("https://www.google.com/search?hl=pt", "https://www.google.com/maps",
            "https://www.google.com.evil.test/search?q=teste", "https://example.com/p?a=1&b=2",
            "https://google.com/search?q=" + "x".repeat(4097),
        ).forEach { url ->
            intents.clear()
            assertEquals(RouteOutcome.BrowserOpened(), route(url).outcome)
            assertEquals(real.component, intents.single().component)
            assertEquals(url, intents.single().dataString)
        }
    }

    @Test fun `outro app nao pesquisa mesmo com assinatura e experimental`() {
        val url = "https://google.com/search?q=teste&client=tablet-android-samsung-rvo1"
        val report = router.route(Intent.ACTION_VIEW, Uri.parse(url), LinkOrigin(OriginKind.OTHER), true)
        assertEquals(RouteOutcome.BrowserOpened(), report.outcome)
        assertEquals(real.component, intents.single().component)
    }

    @Test fun `origem desconhecida exige opt in para assinatura`() {
        val url = Uri.parse("https://google.com/search?q=teste&client=tablet-android-samsung-rvo1")
        assertEquals(RouteOutcome.BrowserOpened(), router.route(Intent.ACTION_VIEW, url, LinkOrigin(OriginKind.UNKNOWN), false).outcome)
        intents.clear()
        assertEquals(RouteOutcome.GoogleOpened, router.route(Intent.ACTION_VIEW, url, LinkOrigin(OriginKind.UNKNOWN), true).outcome)
        assertEquals(Intent.ACTION_WEB_SEARCH, intents.single().action)
    }

    @Test fun `sem nenhum destino mostra recuperacao`() {
        googleFailure = ActivityNotFoundException()
        catalog.targets = emptyList()
        val report = route("https://google.com/search?q=teste")
        assertEquals(RouteOutcome.Recovery(BrowserFailure.NO_LONGER_AVAILABLE, SearchFailure.UNAVAILABLE), report.outcome)
        assertEquals(1, intents.size)
    }

    @Test fun `entradas externas invalidas nao abrem nenhuma activity`() {
        listOf("file:///tmp/teste", "javascript:alert(1)", "intent://example.com").forEach {
            assertEquals(RouteOutcome.InvalidInput, route(it).outcome)
        }
        assertEquals(RouteOutcome.InvalidInput, router.route(Intent.ACTION_SEND, Uri.parse("https://google.com/search?q=x"), finder, false).outcome)
        assertEquals(RouteOutcome.InvalidInput, router.route(Intent.ACTION_VIEW, null, finder, false).outcome)
        assertTrue(intents.isEmpty())
    }

    private fun route(url: String) = router.route(Intent.ACTION_VIEW, Uri.parse(url), finder, false)
}
