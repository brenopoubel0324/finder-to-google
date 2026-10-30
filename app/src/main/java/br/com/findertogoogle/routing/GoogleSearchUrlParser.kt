package br.com.findertogoogle.routing

import android.net.Uri

/** Sem I/O ou estado. A decodificação é feita uma única vez pelo próprio Android. */
object GoogleSearchUrlParser {
    const val MAX_QUERY_LENGTH = 4096
    val defaultFinderClients = setOf(
        "tablet-android-samsung-rvo1",
        "ms-android-samsung-a1-terr1-rvo1aqb",
    )
    private val trustedHosts = setOf("google.com", "www.google.com", "google.com.br", "www.google.com.br")

    fun isWebUri(uri: Uri?): Boolean = try {
        uri != null && uri.isHierarchical &&
            (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) &&
            !uri.host.isNullOrBlank()
    } catch (_: RuntimeException) { false }

    fun extractQuery(uri: Uri?): String? = try {
        if (!isWebUri(uri) || uri == null ||
            trustedHosts.none { it.equals(uri.host, ignoreCase = true) } ||
            uri.userInfo != null || uri.encodedPath != "/search" ||
            (uri.port != -1 && uri.port != if (uri.scheme.equals("https", true)) 443 else 80)
        ) null else uri.getQueryParameter("q")?.takeIf {
            it.length <= MAX_QUERY_LENGTH && it.isNotBlank()
        }
    } catch (_: RuntimeException) { null }

    fun finderClient(uri: Uri): String? = try {
        if (extractQuery(uri) == null) null else uri.getQueryParameter("client")?.takeIf(FinderClientSignature::isValid)
    } catch (_: RuntimeException) { null }

    fun hasFinderSignature(uri: Uri, clients: Set<String> = defaultFinderClients): Boolean = try {
        finderClient(uri)?.let { it in clients } == true
    } catch (_: RuntimeException) { false }
}
