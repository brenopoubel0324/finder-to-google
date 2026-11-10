package br.com.findertogoogle.data

import android.content.ComponentName
import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import br.com.findertogoogle.routing.FinderClientSignature
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.StringReader
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

data class SettingsBackup(val browserPackage: String?, val browserComponent: String?,
    val compatibility: Boolean, val diagnostics: Boolean, val rules: List<FinderSignature>)

/** Formato portátil de configurações. Nunca inclui permissões, consultas ou diagnóstico. */
object SettingsBackupCodec {
    const val MAX_BYTES = 16 * 1024
    private const val FORMAT = "finder-to-google-settings"
    fun encode(value: SettingsBackup): ByteArray {
        validate(value)
        return JSONObject().put("format", FORMAT).put("version", 1)
            .put("browserPackage", value.browserPackage ?: JSONObject.NULL)
            .put("browserComponent", value.browserComponent ?: JSONObject.NULL)
            .put("compatibility", value.compatibility).put("diagnostics", value.diagnostics)
            .put("rules", JSONArray().apply { value.rules.forEach {
                put(JSONObject().put("client", it.client).put("enabled", it.enabled))
            } }).toString(2).toByteArray(Charsets.UTF_8).also { require(it.size <= MAX_BYTES) }
    }
    fun read(stream: InputStream): SettingsBackup {
        val data = stream.readBytesBounded()
        val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(data)).toString()
        return decode(text)
    }
    private fun InputStream.readBytesBounded(): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(2048)
        while (true) {
            val n = read(buffer)
            if (n < 0) return output.toByteArray()
            require(output.size() + n <= MAX_BYTES)
            output.write(buffer, 0, n)
        }
    }
    fun decode(text: String): SettingsBackup {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        var format: String? = null; var version: String? = null
        var pkg: String? = null; var component: String? = null
        var compatibility: Boolean? = null; var diagnostics: Boolean? = null
        val rules = mutableListOf<FinderSignature>()
        JsonReader(StringReader(text)).use { r ->
            val keys = mutableSetOf<String>(); r.beginObject()
            while (r.hasNext()) {
                val key = r.nextName(); require(keys.add(key))
                when (key) {
                    "format" -> format = r.strictString()
                    "version" -> { require(r.peek() == JsonToken.NUMBER); version = r.nextString() }
                    "browserPackage" -> pkg = r.nullableString()
                    "browserComponent" -> component = r.nullableString()
                    "compatibility" -> compatibility = r.strictBoolean()
                    "diagnostics" -> diagnostics = r.strictBoolean()
                    "rules" -> {
                        r.beginArray()
                        while (r.hasNext()) {
                            require(rules.size < FinderSignatureStore.MAX_RULES)
                            var client: String? = null; var enabled: Boolean? = null
                            val ruleKeys = mutableSetOf<String>(); r.beginObject()
                            while (r.hasNext()) {
                                val ruleKey = r.nextName(); require(ruleKeys.add(ruleKey))
                                when (ruleKey) {
                                    "client" -> client = r.strictString()
                                    "enabled" -> enabled = r.strictBoolean()
                                    else -> error("Campo desconhecido")
                                }
                            }
                            r.endObject(); require(ruleKeys == setOf("client", "enabled"))
                            rules += FinderSignature(requireNotNull(client), requireNotNull(enabled))
                        }
                        r.endArray()
                    }
                    else -> error("Campo desconhecido")
                }
            }
            r.endObject(); require(r.peek() == JsonToken.END_DOCUMENT)
            require(keys == setOf("format", "version", "browserPackage", "browserComponent", "compatibility", "diagnostics", "rules"))
        }
        require(format == FORMAT && version == "1")
        return SettingsBackup(pkg, component, requireNotNull(compatibility), requireNotNull(diagnostics), rules).also(::validate)
    }
    fun validate(value: SettingsBackup) {
        require(value.browserPackage == null || (value.browserPackage != "br.com.findertogoogle" &&
            value.browserPackage.length <= 200 && Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+").matches(value.browserPackage)))
        value.browserComponent?.let {
            require(it.length <= 600)
            val component = requireNotNull(ComponentName.unflattenFromString(it))
            require(component.packageName == value.browserPackage && component.className.length <= 400 &&
                Regex("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+").matches(component.className))
        }
        require(value.rules.size <= FinderSignatureStore.MAX_RULES && value.rules.map { it.client }.distinct().size == value.rules.size)
        require(value.rules.all { FinderClientSignature.isValid(it.client) })
    }
    private fun JsonReader.strictString(): String { require(peek() == JsonToken.STRING); return nextString() }
    private fun JsonReader.nullableString(): String? = if (peek() == JsonToken.NULL) { nextNull(); null } else strictString()
    private fun JsonReader.strictBoolean(): Boolean { require(peek() == JsonToken.BOOLEAN); return nextBoolean() }
}

/** Journal curto: uma restauração válida interrompida é concluída antes de ler preferências. */
object SettingsRestore {
    @Synchronized fun recover(context: Context) {
        val journal = context.getSharedPreferences("settings_restore", Context.MODE_PRIVATE)
        val text = journal.getString("pending", null) ?: return
        applyPending(context, SettingsBackupCodec.decode(text))
    }
    @Synchronized fun restore(context: Context, value: SettingsBackup) {
        val encoded = SettingsBackupCodec.encode(value).toString(Charsets.UTF_8)
        check(context.getSharedPreferences("settings_restore", Context.MODE_PRIVATE).edit().putString("pending", encoded).commit())
        applyPending(context, value)
    }
    private fun applyPending(context: Context, value: SettingsBackup) {
        check(context.getSharedPreferences("finder_rules", Context.MODE_PRIVATE).edit()
            .putStringSet("registered", value.rules.map { it.client }.toSet())
            .putStringSet("disabled", value.rules.filterNot { it.enabled }.map { it.client }.toSet()).commit())
        check(context.getSharedPreferences("router_settings", Context.MODE_PRIVATE).edit()
            .putString("browser_package", value.browserPackage).putString("browser_component", value.browserComponent)
            .putBoolean("experimental_finder", value.compatibility).putBoolean("diagnostics_enabled", value.diagnostics)
            .remove("last_diagnostic").commit())
        check(context.getSharedPreferences("settings_restore", Context.MODE_PRIVATE).edit().remove("pending").commit())
    }
    fun capture(context: Context): SettingsBackup {
        val p = AppPreferences(context)
        return SettingsBackup(p.selectedBrowserPackage, p.preferredBrowserComponent?.flattenToString(),
            p.experimentalFinderDetection, p.diagnosticsEnabled, FinderSignatureStore(context).all())
    }
}
