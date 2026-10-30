package br.com.findertogoogle.routing

import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinderCalibrationTest {
    private var now = 100L
    private val session = FinderCalibration { now }
    private val phrase = "teste finder 12345678"
    private val unknown = LinkOrigin(OriginKind.UNKNOWN)
    private fun url(client: String = "novo-samsung-2027", query: String = phrase) = Uri.parse(
        "https://google.com/search?client=$client&q=" + Uri.encode(query))
    private fun inspect(uri: Uri = url(), origin: LinkOrigin = unknown) = session.inspect(Intent.ACTION_VIEW, uri, origin)

    @Test fun `nao aprende sem inicio local nem por apenas extras ou URL`() {
        assertEquals(CalibrationCapture.Ignore, inspect())
        assertNull(session.current())
    }
    @Test fun `frase alheia e links comuns nao consomem sessao`() {
        session.begin(phrase)
        assertEquals(CalibrationCapture.Ignore, inspect(url(query = "pesquisa real privada")))
        assertEquals(CalibrationCapture.Ignore, inspect(Uri.parse("https://example.com/?q=" + Uri.encode(phrase))))
        assertEquals(phrase, session.current()?.phrase)
        assertEquals(CalibrationCapture.Candidate("novo-samsung-2027"), inspect())
    }
    @Test fun `captura unica devolve somente client sem persistir termo`() {
        session.begin(phrase)
        val capture = inspect() as CalibrationCapture.Candidate
        assertEquals("novo-samsung-2027", capture.client)
        assertFalse(capture.toString().contains(phrase))
        assertNull(session.current())
        assertEquals(CalibrationCapture.Ignore, inspect())
    }
    @Test fun `outro app verificavel nao pode ser aprendido mesmo com frase correta`() {
        session.begin(phrase)
        assertEquals(CalibrationCapture.Ignore, inspect(origin = LinkOrigin(OriginKind.OTHER)))
        assertNotNull(session.current())
        assertTrue(inspect(origin = LinkOrigin(OriginKind.FINDER)) is CalibrationCapture.Candidate)
    }
    @Test fun `assinatura ausente invalida ou ambigua nao vira regra`() {
        listOf(url(client = "valor%20invalido"),
            Uri.parse("https://google.com/search?q=" + Uri.encode(phrase)),
            Uri.parse(url().toString() + "&client=segundo"), url(client = "a".repeat(129)),
        ).forEach {
            session.begin(phrase)
            assertEquals(it.toString(), CalibrationCapture.Unsupported, inspect(it))
            assertNull(session.current())
        }
    }
    @Test fun `host caminho e action inadequados nao interceptam teste`() {
        session.begin(phrase)
        listOf("https://google.com.evil.test/search", "https://google.com/maps", "https://user@google.com/search").forEach {
            assertEquals(CalibrationCapture.Ignore, inspect(Uri.parse(it + "?client=novo&q=" + Uri.encode(phrase))))
        }
        assertEquals(CalibrationCapture.Ignore, session.inspect(Intent.ACTION_SEND, url(), unknown))
        assertNotNull(session.current())
    }
    @Test fun `prazo expira sem timer e cancela se relogio voltar`() {
        session.begin(phrase)
        now += FinderCalibration.VALIDITY_MS - 1
        assertNotNull(session.current())
        now++
        assertEquals(CalibrationCapture.Ignore, inspect())
        session.begin(phrase); now--
        assertNull(session.current())
    }
    @Test fun `cancelar substituir ou perder processo nao deixa aprendizado ativo`() {
        session.begin(phrase); session.cancel()
        assertNull(session.current())
        session.begin(phrase); session.begin("teste finder abcdef12")
        assertEquals(CalibrationCapture.Ignore, inspect())
        assertEquals(CalibrationCapture.Ignore, FinderCalibration { now }.inspect(Intent.ACTION_VIEW, url(), unknown))
        assertEquals(CalibrationCapture.Candidate("novo-samsung-2027"), inspect(url(query = "teste finder abcdef12")))
    }
}
