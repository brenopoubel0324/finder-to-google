package br.com.findertogoogle.routing

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ForwardingGuardTest {
    private var now = 0L
    private val guard = ForwardingGuard { now }
    private val uri = Uri.parse("https://example.com/p?q=teste")

    @Test fun `limita repeticao mas permite dois cliques legitimos`() {
        repeat(3) { assertNotNull(guard.reserve(uri, "org.browser")) }
        assertNull(guard.reserve(uri, "org.browser"))
        assertNotNull(guard.reserve(uri, "org.outro"))
        assertNotNull(guard.reserve(Uri.parse("https://example.com/outro"), "org.browser"))
    }
    @Test fun `janela expira sem timer e falha nao consome cota`() {
        repeat(6) { guard.release(requireNotNull(guard.reserve(uri, "org.browser"))) }
        repeat(3) { assertNotNull(guard.reserve(uri, "org.browser")) }
        now = 2000
        assertNotNull(guard.reserve(uri, "org.browser"))
    }
    @Test fun `capacidade e reset sao limitados e ticket antigo nao afeta janela nova`() {
        repeat(40) { guard.reserve(Uri.parse("https://example.com/$it"), "org.browser") }
        assertEquals(16, guard.trackedCount)
        val old = requireNotNull(guard.reserve(uri, "org.browser"))
        guard.reset(uri, "org.browser")
        repeat(3) { assertNotNull(guard.reserve(uri, "org.browser")) }
        guard.release(old)
        assertNull(guard.reserve(uri, "org.browser"))
    }
}
