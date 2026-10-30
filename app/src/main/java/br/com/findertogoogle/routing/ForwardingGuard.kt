package br.com.findertogoogle.routing

import android.net.Uri
import android.os.SystemClock
import java.security.MessageDigest

/** Proteção efêmera de fallback; não registra URL/consulta nem executa limpeza em background. */
class ForwardingGuard(private val clock: () -> Long = { SystemClock.elapsedRealtime() }) {
    class Reservation internal constructor(internal val key: String, internal val generation: Long)
    private data class Entry(val generation: Long, val started: Long, var attempts: Int)
    private val entries = LinkedHashMap<String, Entry>()
    private var generation = 0L

    private fun key(uri: Uri, pkg: String): String = MessageDigest.getInstance("SHA-256")
        .digest((pkg + '\u0000' + uri.toString()).toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    @Synchronized fun reserve(uri: Uri, pkg: String): Reservation? {
        val now = clock()
        entries.entries.removeAll { now - it.value.started >= WINDOW_MS || now < it.value.started }
        val key = key(uri, pkg)
        val entry = entries[key] ?: run {
            if (entries.size >= CAPACITY) entries.remove(entries.keys.first())
            Entry(++generation, now, 0).also { entries[key] = it }
        }
        if (entry.attempts >= MAX_ATTEMPTS) return null
        entry.attempts++
        return Reservation(key, entry.generation)
    }

    @Synchronized fun release(reservation: Reservation) {
        val entry = entries[reservation.key] ?: return
        if (entry.generation != reservation.generation) return
        if (--entry.attempts == 0) entries.remove(reservation.key)
    }

    @Synchronized fun reset(uri: Uri, pkg: String) { entries.remove(key(uri, pkg)) }
    internal val trackedCount: Int @Synchronized get() = entries.size

    companion object {
        const val WINDOW_MS = 2000L
        const val MAX_ATTEMPTS = 3
        const val CAPACITY = 16
        val process = ForwardingGuard()
    }
}
