package br.com.findertogoogle.data

import android.content.Context
import br.com.findertogoogle.routing.FinderClientSignature
import br.com.findertogoogle.routing.GoogleSearchUrlParser

data class FinderSignature(val client: String, val enabled: Boolean)
enum class SignatureSaveResult { SAVED, INVALID, LIMIT_REACHED }

/** Regras locais, sem consulta/URL. Não altera as preferências existentes do roteador. */
class FinderSignatureStore(context: Context) {
    private val prefs = context.getSharedPreferences("finder_rules", Context.MODE_PRIVATE)

    init {
        synchronized(lock) {
            if (!prefs.contains("registered")) prefs.edit()
                .putStringSet("registered", GoogleSearchUrlParser.defaultFinderClients)
                .putStringSet("disabled", emptySet()).apply()
        }
    }

    fun all(): List<FinderSignature> = synchronized(lock) {
        try {
            val registered = prefs.getStringSet("registered", emptySet()).orEmpty()
            val disabled = prefs.getStringSet("disabled", emptySet()).orEmpty()
            registered.filter(FinderClientSignature::isValid).sorted().take(MAX_RULES)
                .map { FinderSignature(it, it !in disabled) }
        } catch (_: ClassCastException) { emptyList() }
    }

    fun enabledClients(): Set<String> = all().filter { it.enabled }.map { it.client }.toSet()

    fun save(client: String): SignatureSaveResult = synchronized(lock) {
        if (!FinderClientSignature.isValid(client)) return SignatureSaveResult.INVALID
        val current = all()
        if (current.none { it.client == client } && current.size >= MAX_RULES) return SignatureSaveResult.LIMIT_REACHED
        write(current.filterNot { it.client == client } + FinderSignature(client, true))
        SignatureSaveResult.SAVED
    }

    fun setEnabled(client: String, enabled: Boolean) = synchronized(lock) {
        write(all().map { if (it.client == client) it.copy(enabled = enabled) else it })
    }

    fun remove(client: String) = synchronized(lock) { write(all().filterNot { it.client == client }) }

    private fun write(rules: List<FinderSignature>) {
        prefs.edit().putStringSet("registered", rules.map { it.client }.toSet())
            .putStringSet("disabled", rules.filterNot { it.enabled }.map { it.client }.toSet()).apply()
    }

    companion object {
        const val MAX_RULES = 32
        private val lock = Any()
    }
}
