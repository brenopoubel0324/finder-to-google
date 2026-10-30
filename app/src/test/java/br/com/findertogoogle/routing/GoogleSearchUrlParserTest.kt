package br.com.findertogoogle.routing

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleSearchUrlParserTest {
    @Test fun `extrai pesquisa Finder confirmada`() {
        assertEquals("teste", parse("https://www.google.com/search?client=tablet-android-samsung-rvo1&hl=pt&q=teste"))
    }

    @Test fun `preserva Unicode emoji espacos e simbolos sem duplo decode`() {
        val cases = mapOf(
            "ol%C3%A1%20mundo" to "olá mundo",
            "C%2B%2B+Android" to "C++ Android",
            "a%26b%3Dc%23d" to "a&b=c#d",
            "%2526%252B%2520" to "%26%2B%20",
            "%F0%9F%98%80+caf%C3%A9" to "😀 café",
            "%2B+%2B" to "+ +",
        )
        cases.forEach { (encoded, expected) ->
            assertEquals(encoded, expected, parse("https://www.google.com/search?hl=pt&q=$encoded&x=1#fragmento"))
        }
    }

    @Test fun `aceita somente hosts exatos com comparacao insensivel a caixa`() {
        listOf("google.com", "www.google.com", "google.com.br", "www.google.com.br", "WWW.GOOGLE.COM").forEach {
            assertEquals(it, "teste", parse("https://$it/search?q=teste"))
        }
        listOf("www.google.com.evil.test", "evilgoogle.com", "google.com.", "google.co.uk", "www.googlé.com").forEach {
            assertNull(it, parse("https://$it/search?q=teste"))
        }
    }

    @Test fun `recusa esquemas caminhos e termos inadequados`() {
        listOf(
            "javascript:alert(1)", "file:///search?q=teste", "intent://google.com/search?q=teste",
            "ftp://google.com/search?q=teste", "https://google.com/maps?q=teste",
            "https://google.com/url?q=teste", "https://google.com/aclk?q=teste",
            "https://google.com/search/?q=teste", "https://google.com/SEARCH?q=teste",
            "https://google.com/%73earch?q=teste", "https://google.com/search?hl=pt",
            "https://google.com/search?q=", "https://google.com/search?q=+++",
            "https://google.com/search?q=%20%09", "https://google.com/search?Q=teste",
            "https://google.com:444/search?q=teste", "https://user@google.com/search?q=teste",
            "https://google.com@evil.test/search?q=teste", "https:/search?q=teste", "",
        ).forEach { assertNull(it, parse(it)) }
        assertNull(GoogleSearchUrlParser.extractQuery(null))
    }

    @Test fun `limite se aplica ao termo decodificado`() {
        assertEquals(4096, parse("https://google.com/search?q=" + "a".repeat(4096))?.length)
        assertNull(parse("https://google.com/search?q=" + "a".repeat(4097)))
        assertEquals("+".repeat(4096), parse("https://google.com/search?q=" + "%2B".repeat(4096)))
    }

    @Test fun `parametro duplicado segue exatamente getQueryParameter do Android`() {
        assertEquals("primeiro", parse("https://google.com/search?q=primeiro&q=segundo"))
        assertNull(parse("https://google.com/search?q=&q=segundo"))
    }

    @Test fun `assinatura experimental exige client confirmado e pesquisa valida`() {
        assertTrue(signature("https://google.com/search?q=teste&client=tablet-android-samsung-rvo1"))
        assertFalse(signature("https://google.com/search?q=teste&client=outro"))
        assertFalse(signature("https://google.com/maps?q=teste&client=tablet-android-samsung-rvo1"))
        assertFalse(signature("https://google.com.evil.test/search?q=teste&client=tablet-android-samsung-rvo1"))
    }

    @Test fun `entrada malformada nao causa crash`() {
        listOf("https://%/search?q=%", "https://[invalid/search?q=%FF", "http://", "https://google.com:bad/search?q=x").forEach {
            GoogleSearchUrlParser.extractQuery(Uri.parse(it))
        }
    }

    @Test fun `reconhece assinatura exata da URL enviada pelo S25`() {
        val url = "https://www.google.com/search?client=ms-android-samsung-a1-terr1-rvo1aqb&hl=pt&q=teste"
        assertEquals("teste", parse(url))
        assertTrue(signature(url))
    }

    @Test fun `assinatura do S25 nao permite variantes nem dispensa busca valida`() {
        val client = "ms-android-samsung-a1-terr1-rvo1aqb"
        listOf(
            "https://google.com/search?q=teste&client=${client}extra",
            "https://google.com/search?q=teste&client=extra$client",
            "https://google.com/search?q=teste&client=MS-ANDROID-SAMSUNG-A1-TERR1-RVO1AQB",
            "https://google.com/search?q=teste&client=ms-android-samsung",
            "https://google.com/search?q=teste&client=outro&client=$client",
            "https://google.com.evil.test/search?q=teste&client=$client",
            "https://google.com/maps?q=teste&client=$client",
            "https://google.com/search?q=+++&client=$client",
            "https://user@google.com/search?q=teste&client=$client",
        ).forEach { assertFalse(it, signature(it)) }
    }

    private fun parse(url: String) = GoogleSearchUrlParser.extractQuery(Uri.parse(url))
    private fun signature(url: String) = GoogleSearchUrlParser.hasFinderSignature(Uri.parse(url))
}
