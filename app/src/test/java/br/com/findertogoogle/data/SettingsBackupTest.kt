package br.com.findertogoogle.data

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsBackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sample = SettingsBackup("org.browser.test", "org.browser.test/.Main", true, true,
        listOf(FinderSignature("personalizado", false), FinderSignature("tablet-android-samsung-rvo1", true)))
    @Before fun clear() {
        listOf("router_settings", "finder_rules", "settings_restore").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
    private fun text(value: SettingsBackup = sample) = SettingsBackupCodec.encode(value).toString(Charsets.UTF_8)
    private fun rejects(value: String) {
        assertTrue(runCatching { SettingsBackupCodec.decode(value) }.isFailure)
    }
    @Test fun `exportacao importa os mesmos dados inclusive regras desativadas`() {
        assertEquals(sample, SettingsBackupCodec.read(text().byteInputStream()))
        val prefs = AppPreferences(context).apply {
            selectBrowser(ComponentName("org.browser.test", "org.browser.test.Main"))
            experimentalFinderDetection = true; diagnosticsEnabled = true
            recordDiagnostic("ultimo-registro-privado")
        }
        val capture = SettingsRestore.capture(context)
        val encoded = text(capture)
        assertFalse(encoded.contains("ultimo-registro-privado"))
        assertFalse(encoded.contains("last_diagnostic"))
        assertEquals(prefs.selectedBrowserPackage, SettingsBackupCodec.read(encoded.byteInputStream()).browserPackage)
    }
    @Test fun `restaura em instalacao nova sem navegador instalado e nao repovoa removidas`() {
        SettingsRestore.restore(context, sample)
        val restored = SettingsRestore.capture(context)
        assertEquals(sample.browserPackage, restored.browserPackage)
        assertEquals(sample.rules.sortedBy { it.client }, restored.rules)
        assertTrue(restored.compatibility); assertTrue(restored.diagnostics)
        assertNull(AppPreferences(context).lastDiagnostic)
        assertEquals(sample.rules.sortedBy { it.client }, FinderSignatureStore(context).all())
        SettingsRestore.restore(context, sample.copy(rules = emptyList(), browserPackage = null, browserComponent = null, compatibility = false, diagnostics = false))
        assertTrue(FinderSignatureStore(context).all().isEmpty())
        assertNull(AppPreferences(context).selectedBrowserPackage)
        assertFalse(AppPreferences(context).experimentalFinderDetection)
    }
    @Test fun `formato invalido nao modifica preferencias existentes`() {
        SettingsRestore.restore(context, sample)
        val before = SettingsRestore.capture(context)
        listOf("{}", text().replace("\"version\": 1", "\"version\": 2"),
            text().replace("\"compatibility\": true", "\"compatibility\": \"true\""),
            text().dropLast(1), text() + "{}", "[1,2,3]").forEach(::rejects)
        assertEquals(before, SettingsRestore.capture(context))
    }
    @Test fun `recusa duplicatas desconhecidos permissoes e campos de diagnostico`() {
        rejects(text().replaceFirst("{", "{\"version\":1,"))
        for (field in listOf("last_diagnostic", "roleHeld", "command", "query", "url")) {
            rejects(JSONObject(text()).put(field, "nao-importar").toString())
        }
        rejects(JSONObject(text()).put("rules", JSONArray().put(JSONObject().put("client", "igual").put("enabled", true))
            .put(JSONObject().put("client", "igual").put("enabled", false))).toString())
        rejects(text().replaceFirst("\"client\":", "\"enabled\":true,\"client\":"))
    }
    @Test fun `limita tamanho UTF8 regras e pacote componente`() {
        assertTrue(runCatching { SettingsBackupCodec.read(ByteArray(SettingsBackupCodec.MAX_BYTES + 1).inputStream()) }.isFailure)
        assertTrue(runCatching { SettingsBackupCodec.read(byteArrayOf(0xc3.toByte(), 0x28).inputStream()) }.isFailure)
        for (value in listOf(sample.copy(browserPackage = "br.com.findertogoogle"),
            sample.copy(browserComponent = "outro.pacote/.Main"),
            sample.copy(browserComponent = "org.browser.test/org..Invalid"),
            sample.copy(rules = listOf(FinderSignature("https://nao-e-padrao", true))),
            sample.copy(rules = (0..32).map { FinderSignature("regra$it", true) }))) {
            assertTrue(runCatching { SettingsBackupCodec.encode(value) }.isFailure)
        }
        assertEquals(32, SettingsBackupCodec.read(text(sample.copy(rules = (0..31).map { FinderSignature("r$it", it % 2 == 0) })).byteInputStream()).rules.size)
    }
    @Test fun `journal conclui restauracao interrompida antes de ler preferencias`() {
        val prefs = AppPreferences(context).apply { diagnosticsEnabled = true; recordDiagnostic("antigo") }
        context.getSharedPreferences("settings_restore", Context.MODE_PRIVATE).edit().putString("pending", text()).commit()
        context.getSharedPreferences("finder_rules", Context.MODE_PRIVATE).edit().putStringSet("registered", emptySet()).commit()
        // Simula nova entrada no processo após interrupção entre os dois arquivos.
        val newPrefs = AppPreferences(context)
        assertEquals(sample.browserPackage, newPrefs.selectedBrowserPackage)
        assertEquals(sample.rules.sortedBy { it.client }, FinderSignatureStore(context).all())
        assertTrue(newPrefs.experimentalFinderDetection)
        assertNull(prefs.lastDiagnostic)
        assertFalse(context.getSharedPreferences("settings_restore", Context.MODE_PRIVATE).contains("pending"))
    }
}
