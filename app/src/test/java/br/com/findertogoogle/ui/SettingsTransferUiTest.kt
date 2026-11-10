package br.com.findertogoogle.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.R
import br.com.findertogoogle.data.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsTransferUiTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uri = Uri.parse("content://test.documents/backup.json")
    @Before fun clear() { clearPreferences() }
    private fun clearPreferences() {
        listOf("router_settings", "finder_rules", "settings_restore").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
    private fun waitUntil(condition: () -> Boolean) {
        val until = System.nanoTime() + 5_000_000_000L
        while (System.nanoTime() < until) {
            shadowOf(Looper.getMainLooper()).idle()
            if (condition()) return
            Thread.sleep(10) // Somente espera limitada do teste, não existe polling no APK.
        }
        fail("Transferência não terminou")
    }
    @Test fun `exporta verifica e importa apos simular nova instalacao sem mudar nada antes de confirmar`() {
        AppPreferences(context).apply {
            selectBrowser(ComponentName("org.browser.ausente", "org.browser.ausente.Main"))
            experimentalFinderDetection = true; diagnosticsEnabled = true; recordDiagnostic("registro-que-nao-exporta")
        }
        val rules = FinderSignatureStore(context)
        rules.all().forEach { rules.remove(it.client) }
        rules.save("meu-padrao"); rules.setEnabled("meu-padrao", false)
        val expected = SettingsRestore.capture(context)
        val output = ByteArrayOutputStream()
        shadowOf(context.contentResolver).registerOutputStream(uri, output)
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { output.toByteArray().inputStream() }
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.exportSettings()
        val request = requireNotNull(shadowOf(a).nextStartedActivityForResult)
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, request.intent.action)
        assertEquals(Intent.ACTION_CREATE_DOCUMENT, requireNotNull(shadowOf(a).nextStartedActivity).action)
        a.activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_OK, Intent().setData(uri))
        waitUntil { ShadowToast.getTextOfLatestToast() == a.getString(R.string.backup_exported) }
        assertEquals(expected, SettingsBackupCodec.read(output.toByteArray().inputStream()))
        assertFalse(output.toString(Charsets.UTF_8.name()).contains("registro-que-nao-exporta"))
        c.pause().stop().destroy()
        clearPreferences() // Simula instalação release nova, mantendo o arquivo fora do app.
        val fresh = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val b = fresh.get()
        val before = SettingsRestore.capture(context)
        b.importSettings()
        val open = requireNotNull(shadowOf(b).nextStartedActivityForResult)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, open.intent.action)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, requireNotNull(shadowOf(b).nextStartedActivity).action)
        b.activityResultRegistry.dispatchResult(open.requestCode, Activity.RESULT_OK, Intent().setData(uri))
        waitUntil { ShadowDialog.getLatestDialog()?.isShowing == true }
        assertEquals(before, SettingsRestore.capture(context))
        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(expected, SettingsRestore.capture(context))
        assertNull(AppPreferences(context).lastDiagnostic)
        assertNull(shadowOf(b).nextStartedActivity)
        fresh.pause().stop().destroy()
    }
    @Test fun `cancelar preview preserva preferencias e nao ativa compatibilidade`() {
        val data = SettingsBackup(null, null, true, true, emptyList())
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { SettingsBackupCodec.encode(data).inputStream() }
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        val before = SettingsRestore.capture(context)
        a.importSettings(); val request = requireNotNull(shadowOf(a).nextStartedActivityForResult)
        a.activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_OK, Intent().setData(uri))
        waitUntil { ShadowDialog.getLatestDialog()?.isShowing == true }
        (ShadowDialog.getLatestDialog() as AlertDialog).getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(before, SettingsRestore.capture(context))
        c.pause().stop().destroy()
    }
    @Test fun `exportacao que nao pode ser lida de volta nao declara sucesso`() {
        shadowOf(context.contentResolver).registerOutputStream(uri, ByteArrayOutputStream())
        shadowOf(context.contentResolver).registerInputStreamSupplier(uri) { "{}".byteInputStream() }
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.exportSettings(); val request = requireNotNull(shadowOf(a).nextStartedActivityForResult)
        a.activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_OK, Intent().setData(uri))
        waitUntil { ShadowToast.getTextOfLatestToast() == a.getString(R.string.backup_failed) }
        c.pause().stop().destroy()
    }
}
