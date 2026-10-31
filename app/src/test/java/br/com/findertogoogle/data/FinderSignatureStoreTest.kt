package br.com.findertogoogle.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.routing.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinderSignatureStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val raw get() = context.getSharedPreferences("finder_rules", Context.MODE_PRIVATE)
    @Before fun clear() { raw.edit().clear().commit() }

    @Test fun `primeiro acesso cadastra tablet e S25 sem alterar preferencias antigas`() {
        val prefs = AppPreferences(context).apply { experimentalFinderDetection = true; diagnosticsEnabled = true }
        assertEquals(GoogleSearchUrlParser.defaultFinderClients, FinderSignatureStore(context).enabledClients())
        assertTrue(prefs.experimentalFinderDetection)
        assertTrue(prefs.diagnosticsEnabled)
    }

    @Test fun `regra nova fica disponivel imediatamente e persiste sem recompilar`() {
        val store = FinderSignatureStore(context)
        val url = Uri.parse("https://google.com/search?q=teste&client=novo-finder-2027")
        val unknown = LinkOrigin(OriginKind.UNKNOWN)
        assertEquals(RoutingDecision.BROWSER_UNKNOWN_SIGNATURE, RoutingPolicy.decide(url, unknown, true, store.enabledClients()))
        assertEquals(SignatureSaveResult.SAVED, store.save("novo-finder-2027"))
        val recreated = FinderSignatureStore(context)
        assertEquals(RoutingDecision.GOOGLE_EXPERIMENTAL, RoutingPolicy.decide(url, unknown, true, recreated.enabledClients()))
        assertEquals(RoutingDecision.BROWSER_UNKNOWN_ORIGIN, RoutingPolicy.decide(url, unknown, false, recreated.enabledClients()))
        assertEquals(RoutingDecision.BROWSER_OTHER_APP, RoutingPolicy.decide(url, LinkOrigin(OriginKind.OTHER), true, recreated.enabledClients()))
        assertEquals(RoutingDecision.BROWSER_UNKNOWN_SIGNATURE, RoutingPolicy.decide(
            Uri.parse("https://google.com/search?q=teste&client=novo-finder-2027-extra"), unknown, true, recreated.enabledClients()))
    }

    @Test fun `desativar reativar e excluir sobrevivem a nova instancia`() {
        val store = FinderSignatureStore(context)
        val value = GoogleSearchUrlParser.defaultFinderClients.first()
        store.setEnabled(value, false)
        assertFalse(value in FinderSignatureStore(context).enabledClients())
        assertTrue(store.all().any { it.client == value && !it.enabled })
        store.setEnabled(value, true)
        assertTrue(value in FinderSignatureStore(context).enabledClients())
        store.remove(value)
        assertFalse(FinderSignatureStore(context).all().any { it.client == value })
    }

    @Test fun `excluir todos nao recadastra padroes ao reiniciar`() {
        val store = FinderSignatureStore(context)
        store.all().forEach { store.remove(it.client) }
        assertTrue(FinderSignatureStore(context).all().isEmpty())
    }

    @Test fun `recusa URL espacos controles expressoes e comprimento indevido`() {
        val store = FinderSignatureStore(context)
        val before = store.all()
        listOf("", "https://google.com/search?q=SEGREDO", "campo com espaco", "prefix*", ".inicio", "valor\n", "a".repeat(129), "ácento").forEach {
            assertEquals(it, SignatureSaveResult.INVALID, store.save(it))
        }
        assertEquals(before, store.all())
        assertFalse(raw.all.toString().contains("SEGREDO"))
        assertEquals(SignatureSaveResult.SAVED, store.save("a".repeat(128)))
    }

    @Test fun `limite inclui desativadas e salvar existente reativa sem duplicar`() {
        val store = FinderSignatureStore(context)
        repeat(FinderSignatureStore.MAX_RULES - store.all().size) { store.save("client-$it") }
        store.setEnabled("client-0", false)
        assertEquals(SignatureSaveResult.LIMIT_REACHED, store.save("excedente"))
        assertEquals(SignatureSaveResult.SAVED, store.save("client-0"))
        assertEquals(FinderSignatureStore.MAX_RULES, store.all().size)
        assertTrue("client-0" in store.enabledClients())
        store.remove("client-0")
        assertEquals(SignatureSaveResult.SAVED, store.save("excedente"))
    }

    @Test fun `preferencias corrompidas nao ativam regras nem ressuscitam padroes`() {
        raw.edit().putInt("registered", 4).commit()
        assertTrue(FinderSignatureStore(context).enabledClients().isEmpty())
        raw.edit().putStringSet("registered", setOf("valido", "https://google.com", "a".repeat(129)))
            .putStringSet("disabled", emptySet()).commit()
        assertEquals(setOf("valido"), FinderSignatureStore(context).enabledClients())
        raw.edit().putInt("disabled", 4).commit()
        assertTrue(FinderSignatureStore(context).enabledClients().isEmpty())
    }

    @Test fun `desfazer preserva estado ativo ou desativado sem sobrescrever outra alteracao`() {
        val store = FinderSignatureStore(context)
        listOf(true, false).forEach { enabled ->
            store.save("desfazer"); store.setEnabled("desfazer", enabled)
            val rule = store.all().first { it.client == "desfazer" }
            store.remove(rule.client)
            assertEquals(SignatureRestoreResult.RESTORED, store.restore(rule))
            assertEquals(enabled, FinderSignatureStore(context).all().first { it.client == rule.client }.enabled)
            store.setEnabled(rule.client, !enabled)
            assertEquals(SignatureRestoreResult.ALREADY_EXISTS, store.restore(rule))
            assertEquals(!enabled, store.all().first { it.client == rule.client }.enabled)
        }
    }
    @Test fun `desfazer respeita limite e recusa marcador invalido`() {
        val store = FinderSignatureStore(context)
        repeat(FinderSignatureStore.MAX_RULES - store.all().size) { store.save("limite-$it") }
        assertEquals(SignatureRestoreResult.LIMIT_REACHED, store.restore(FinderSignature("ausente", false)))
        assertEquals(SignatureRestoreResult.INVALID, store.restore(FinderSignature("https://url", true)))
        assertFalse(store.all().any { it.client == "ausente" })
    }
}
