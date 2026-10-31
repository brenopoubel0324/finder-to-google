package br.com.findertogoogle.ui

import android.app.SearchManager
import android.app.role.RoleManager
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.R
import br.com.findertogoogle.data.AppPreferences
import br.com.findertogoogle.data.FinderSignatureStore
import br.com.findertogoogle.routing.FinderCalibration
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CalibrationUiTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val phrase = "teste finder 12345678"
    private val client = "novo-samsung-local-2027"
    @Before fun clear() {
        listOf("router_settings", "finder_rules").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
        FinderCalibration.process.cancel()
    }
    @After fun cancel() { FinderCalibration.process.cancel() }
    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }
    private fun dialog(): AlertDialog { idle(); return ShadowDialog.getLatestDialog() as AlertDialog }
    private fun link(query: String = phrase) = Intent(Intent.ACTION_VIEW,
        Uri.parse("https://google.com/search?client=$client&q=" + Uri.encode(query)))
    private fun capture() = run {
        FinderCalibration.process.begin(phrase)
        Robolectric.buildActivity(LinkRouterActivity::class.java, link()).setup()
    }

    @Test fun `aprendizado exige papel e nao muda clipboard sem iniciar`() {
        val settings = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        settings.get().openPage(SettingsPage.CALIBRATION)
        settings.get().findViewById<MaterialButton>(R.id.learn_signature).performClick()
        assertNull(FinderCalibration.process.current())
        assertNull(context.getSystemService(ClipboardManager::class.java).primaryClip)
        settings.pause().stop().destroy()
    }

    @Test fun `botao explicito copia desafio e inicia sessao sem ligar compatibilidade`() {
        val role = shadowOf(context.getSystemService(RoleManager::class.java))
        role.addAvailableRole(RoleManager.ROLE_BROWSER); role.addHeldRole(RoleManager.ROLE_BROWSER)
        installUiBrowser(context)
        val settings = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        settings.get().openPage(SettingsPage.CALIBRATION)
        assertNull(FinderCalibration.process.current())
        settings.get().findViewById<MaterialButton>(R.id.learn_signature).performClick()
        idle()
        val pending = requireNotNull(FinderCalibration.process.current())
        assertEquals(pending.phrase, context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text.toString())
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        assertTrue(settings.get().findViewById<TextView>(R.id.calibration_status).text.contains(pending.phrase))
        settings.get().findViewById<MaterialButton>(R.id.cancel_calibration).performClick()
        assertNull(FinderCalibration.process.current())
        settings.pause().stop().destroy()
    }

    @Test fun `captura so salva com confirmacao e nova regra roteia sem outro build`() {
        val controller = capture(); val activity = controller.get()
        assertEquals(client, activity.findViewById<TextView>(R.id.calibration_client).text.toString())
        assertFalse(client in FinderSignatureStore(context).enabledClients())
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        assertNull(shadowOf(activity).nextStartedActivity)
        activity.findViewById<MaterialButton>(R.id.confirm_signature).performClick()
        assertTrue(client in FinderSignatureStore(context).enabledClients())
        assertTrue(AppPreferences(context).experimentalFinderDetection)
        val sent = requireNotNull(shadowOf(activity).nextStartedActivity)
        assertEquals(Intent.ACTION_WEB_SEARCH, sent.action)
        assertEquals(phrase, sent.getStringExtra(SearchManager.QUERY))
        assertNull(sent.data)
        controller.pause().stop().destroy()
        val fresh = Robolectric.buildActivity(LinkRouterActivity::class.java, link("teste normal")).setup()
        assertEquals(Intent.ACTION_WEB_SEARCH, shadowOf(fresh.get()).nextStartedActivity?.action)
        fresh.pause().stop().destroy()
        FinderSignatureStore(context).setEnabled(client, false)
        val disabled = Robolectric.buildActivity(LinkRouterActivity::class.java, link("teste normal")).setup()
        assertNull(shadowOf(disabled.get()).nextStartedActivity)
        assertNotNull(disabled.get().findViewById<View>(R.id.recovery_message))
        disabled.pause().stop().destroy()
    }

    @Test fun `continuar sem salvar nao altera regra nem modo`() {
        val controller = capture()
        controller.get().findViewById<MaterialButton>(R.id.skip_signature).performClick()
        assertFalse(client in FinderSignatureStore(context).enabledClients())
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        assertNull(shadowOf(controller.get()).nextStartedActivity)
        assertNotNull(controller.get().findViewById<View>(R.id.recovery_message))
        controller.pause().stop().destroy()
    }

    @Test fun `recriacao preserva confirmacao sem pesquisar nem salvar URL ou desafio adicional`() {
        val controller = capture()
        val state = Bundle(); controller.saveInstanceState(state)
        assertEquals(client, state.getString("router.calibration_client"))
        state.keySet().filter { it.startsWith("router.") }.forEach {
            assertFalse(state.get(it).toString().contains("https://"))
            assertFalse(state.get(it).toString().contains(phrase))
        }
        controller.recreate()
        assertEquals(client, controller.get().findViewById<TextView>(R.id.calibration_client).text.toString())
        assertNull(shadowOf(controller.get()).nextStartedActivity)
        assertFalse(client in FinderSignatureStore(context).enabledClients())
        listOf("finder_rules", "router_settings").forEach {
            val raw = context.getSharedPreferences(it, Context.MODE_PRIVATE).all.toString()
            assertFalse(raw.contains(phrase)); assertFalse(raw.contains("https://"))
        }
        controller.get().findViewById<MaterialButton>(R.id.confirm_signature).performClick()
        assertEquals(Intent.ACTION_WEB_SEARCH, shadowOf(controller.get()).nextStartedActivity?.action)
        controller.pause().stop().destroy()
    }

    @Test fun `extras externos nao iniciam aprendizado nem confirmam uma regra`() {
        val controller = Robolectric.buildActivity(LinkRouterActivity::class.java,
            link().putExtra("router.calibration_client", client).putExtra("learn", true).putExtra("confirm", true)).setup()
        assertFalse(client in FinderSignatureStore(context).enabledClients())
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        assertNull(shadowOf(controller.get()).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `cadastro manual valida e gerenciador desativa e exclui`() {
        val settings = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        val store = FinderSignatureStore(context)
        settings.get().openPage(SettingsPage.PATTERNS)
        settings.get().findViewById<MaterialButton>(R.id.add_signature).performClick()
        val add = dialog(); val input = add.findViewById<TextInputEditText>(R.id.signature_input)!!
        assertFalse(input.isSaveEnabled)
        input.setText("https://google.com/search?q=NAO_SALVAR")
        add.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        assertTrue(add.isShowing)
        assertNotNull(add.findViewById<TextInputLayout>(R.id.signature_input_layout)?.error)
        input.setText(client)
        add.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        assertTrue(client in store.enabledClients())
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        fun row(): View {
            val list = settings.get().findViewById<android.widget.LinearLayout>(R.id.patterns_list)
            return (0 until list.childCount).map { list.getChildAt(it) }.first {
                it.findViewById<TextView>(R.id.pattern_client).text.toString() == client
            }
        }
        row().findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.pattern_toggle).isChecked = false
        assertFalse(client in store.enabledClients())
        row().findViewById<MaterialButton>(R.id.pattern_delete).performClick()
        assertFalse(store.all().any { it.client == client })
        assertFalse(context.getSharedPreferences("finder_rules", Context.MODE_PRIVATE).all.toString().contains("NAO_SALVAR"))
        settings.pause().stop().destroy()
    }

    @Test fun `lista cheia mantem confirmacao sem ligar modo nem abrir Google`() {
        val store = FinderSignatureStore(context)
        repeat(FinderSignatureStore.MAX_RULES - store.all().size) { store.save("client-$it") }
        val controller = capture()
        controller.get().findViewById<MaterialButton>(R.id.confirm_signature).performClick()
        assertEquals(context.getString(R.string.signature_limit), controller.get().findViewById<TextView>(R.id.calibration_message).text.toString())
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        assertNull(shadowOf(controller.get()).nextStartedActivity)
        assertFalse(client in store.enabledClients())
        controller.pause().stop().destroy()
    }

    @Test fun `modo alterado depois de apresentar confirmacao exige novo consentimento`() {
        val prefs = AppPreferences(context).apply { experimentalFinderDetection = true }
        val controller = capture(); val activity = controller.get()
        assertEquals(context.getString(R.string.confirm_pattern_only), activity.findViewById<MaterialButton>(R.id.confirm_signature).text.toString())
        prefs.experimentalFinderDetection = false
        activity.findViewById<MaterialButton>(R.id.confirm_signature).performClick()
        assertFalse(prefs.experimentalFinderDetection)
        assertFalse(client in FinderSignatureStore(context).enabledClients())
        assertNull(shadowOf(activity).nextStartedActivity)
        assertEquals(context.getString(R.string.calibration_confirm_save), activity.findViewById<MaterialButton>(R.id.confirm_signature).text.toString())
        activity.findViewById<MaterialButton>(R.id.confirm_signature).performClick()
        assertTrue(prefs.experimentalFinderDetection)
        assertEquals(Intent.ACTION_WEB_SEARCH, shadowOf(activity).nextStartedActivity?.action)
        controller.pause().stop().destroy()
    }
    @Test fun `retorno a confirmacao atualiza explicacao e nao pesquisa sozinho`() {
        val prefs = AppPreferences(context).apply { experimentalFinderDetection = true }
        val controller = capture()
        controller.pause(); prefs.experimentalFinderDetection = false; controller.resume()
        assertEquals(context.getString(R.string.calibration_confirm_save), controller.get().findViewById<MaterialButton>(R.id.confirm_signature).text.toString())
        assertNull(shadowOf(controller.get()).nextStartedActivity)
        assertFalse(prefs.experimentalFinderDetection)
        controller.pause().stop().destroy()
    }
}
