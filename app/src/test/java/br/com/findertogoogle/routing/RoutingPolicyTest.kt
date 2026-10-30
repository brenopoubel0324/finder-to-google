package br.com.findertogoogle.routing

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoutingPolicyTest {
    private val search = Uri.parse("https://www.google.com/search?q=teste&client=tablet-android-samsung-rvo1")

    @Test fun `somente Finder reconhecido usa Google por padrao`() {
        assertEquals(RoutingDecision.GOOGLE_FINDER, decide(OriginKind.FINDER, false))
        assertEquals(RoutingDecision.BROWSER_OTHER_APP, decide(OriginKind.OTHER, false))
        assertEquals(RoutingDecision.BROWSER_UNKNOWN_ORIGIN, decide(OriginKind.UNKNOWN, false))
    }

    @Test fun `experimental so afeta desconhecidos com assinatura exata`() {
        assertEquals(RoutingDecision.GOOGLE_EXPERIMENTAL, decide(OriginKind.UNKNOWN, true))
        assertEquals(RoutingDecision.GOOGLE_FINDER, decide(OriginKind.FINDER, true))
        assertEquals(RoutingDecision.BROWSER_OTHER_APP, decide(OriginKind.OTHER, true))
        assertEquals(RoutingDecision.BROWSER_UNKNOWN_ORIGIN, RoutingPolicy.decide(
            Uri.parse("https://google.com/search?q=teste"), LinkOrigin(OriginKind.UNKNOWN), true,
        ))
    }

    @Test fun `links comuns vao ao navegador independentemente da origem`() {
        OriginKind.entries.forEach {
            assertEquals(RoutingDecision.BROWSER_NOT_SEARCH, RoutingPolicy.decide(
                Uri.parse("https://www.google.com/maps"), LinkOrigin(it), true,
            ))
        }
        assertEquals(RoutingDecision.INVALID_LINK, RoutingPolicy.decide(Uri.parse("javascript:1"), LinkOrigin(OriginKind.FINDER), true))
    }

    @Test fun `referrer sozinho nunca comprova Finder`() {
        val spoofed = LinkOriginResolver.fromEvidence(null, null, LinkOriginResolver.FINDER_PACKAGE)
        assertEquals(OriginKind.UNKNOWN, spoofed.kind)
        assertEquals(LinkOriginResolver.FINDER_PACKAGE, spoofed.referrerHint)
        assertEquals(RoutingDecision.BROWSER_UNKNOWN_ORIGIN, RoutingPolicy.decide(search, spoofed, false))
    }

    @Test fun `identidade do sistema prevalece sobre referrer e chamador secundario`() {
        val origin = LinkOriginResolver.fromEvidence("org.outro.app", LinkOriginResolver.FINDER_PACKAGE, LinkOriginResolver.FINDER_PACKAGE)
        assertEquals(OriginKind.OTHER, origin.kind)
        assertEquals(IdentityEvidence.SHARED_IDENTITY, origin.evidence)
        assertEquals(RoutingDecision.BROWSER_OTHER_APP, RoutingPolicy.decide(search, origin, true))
    }

    @Test fun `callingPackage autentico tambem identifica Finder`() {
        val origin = LinkOriginResolver.fromEvidence(null, LinkOriginResolver.FINDER_PACKAGE, null)
        assertEquals(OriginKind.FINDER, origin.kind)
        assertEquals(IdentityEvidence.CALLING_PACKAGE, origin.evidence)
    }

    @Test fun `diagnostico ignora pacote malformado e hint arbitrario`() {
        val origin = LinkOriginResolver.fromEvidence("nao e um pacote", null, "https://google.com/search?q=segredo")
        assertNull(origin.packageName)
        assertNull(origin.referrerHint)
    }

    private fun decide(kind: OriginKind, experimental: Boolean) = RoutingPolicy.decide(search, LinkOrigin(kind), experimental)
}
