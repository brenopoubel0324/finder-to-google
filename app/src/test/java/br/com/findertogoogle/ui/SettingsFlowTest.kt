package br.com.findertogoogle.ui

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.R
import br.com.findertogoogle.data.AppPreferences
import br.com.findertogoogle.data.FinderSignatureStore
import br.com.findertogoogle.routing.FinderCalibration
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
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
class SettingsFlowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }
    @Before fun clear() {
        listOf("router_settings", "finder_rules").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        FinderCalibration.process.cancel()
    }
    private fun held() {
        val r = shadowOf(context.getSystemService(RoleManager::class.java))
        r.addAvailableRole(RoleManager.ROLE_BROWSER); r.addHeldRole(RoleManager.ROLE_BROWSER)
    }
    @Test fun `inicio nao pesquisa e guia reage a escolha concessao cancelamento e retorno`() {
        installUiBrowser(context, false)
        shadowOf(context.getSystemService(RoleManager::class.java)).addAvailableRole(RoleManager.ROLE_BROWSER)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        assertNull(shadowOf(a).nextStartedActivity)
        a.findViewById<MaterialButton>(R.id.home_primary).performClick()
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.step_browser).visibility)
        a.findViewById<MaterialButton>(R.id.setup_continue).performClick(); idle()
        (ShadowDialog.getLatestDialog() as AlertDialog).listView.performItemClick(View(context), 0, 0L); idle()
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.step_android).visibility)
        a.findViewById<MaterialButton>(R.id.setup_continue).performClick(); idle()
        val request = requireNotNull(shadowOf(a).nextStartedActivityForResult)
        assertEquals("android.app.role.action.REQUEST_ROLE", shadowOf(a).nextStartedActivity?.action)
        a.activityResultRegistry.dispatchResult(request.requestCode, Activity.RESULT_CANCELED, null)
        idle()
        assertFalse(context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_BROWSER))
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.step_android).visibility)
        held(); a.findViewById<MaterialButton>(R.id.setup_continue).performClick()
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.step_finder).visibility)
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        a.findViewById<MaterialButton>(R.id.answer_google).performClick()
        assertEquals(context.getString(R.string.answer_confirmed), a.findViewById<TextView>(R.id.setup_feedback).text.toString())
        assertNull(shadowOf(a).nextStartedActivity)
        c.pause().stop().destroy()
    }
    @Test fun `atualizacao nao força guia nem muda preferencias e alterar contexto invalida confirmacao`() {
        installUiBrowser(context); held()
        val prefs = AppPreferences(context).apply { experimentalFinderDetection = true; diagnosticsEnabled = true; recordDiagnostic("Metadados") }
        val rules = FinderSignatureStore(context); rules.save("custom-preservado"); rules.setEnabled("custom-preservado", false)
        val previous = rules.all()
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        assertNotNull(a.findViewById<View>(R.id.home_primary))
        assertNull(a.findViewById<View>(R.id.setup_continue))
        assertEquals(previous, rules.all()); assertTrue(prefs.experimentalFinderDetection); assertTrue(prefs.diagnosticsEnabled)
        a.homePrimary(); a.answerFinder(FinderTestAnswer.CONFIRMED)
        a.findViewById<MaterialSwitch>(R.id.experimental).isChecked = false
        assertEquals("", a.findViewById<TextView>(R.id.setup_feedback).text.toString())
        a.answerFinder(FinderTestAnswer.CONFIRMED)
        c.pause(); shadowOf(context.getSystemService(RoleManager::class.java)).removeHeldRole(RoleManager.ROLE_BROWSER); c.resume()
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.step_android).visibility)
        assertEquals("", a.findViewById<TextView>(R.id.setup_feedback).text.toString())
        assertEquals("Metadados", prefs.lastDiagnostic)
        c.pause().stop().destroy()
    }
    @Test fun `navegacao e papel retirado sao recalculados sem abrir links`() {
        installUiBrowser(context); held()
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.openPage(SettingsPage.HELP); a.openPage(SettingsPage.CONFIGURATION)
        a.onBackPressedDispatcher.onBackPressed()
        assertNotNull(a.findViewById<View>(R.id.finder_problem))
        a.onBackPressedDispatcher.onBackPressed()
        assertNotNull(a.findViewById<View>(R.id.home_primary))
        c.pause(); shadowOf(context.getSystemService(RoleManager::class.java)).removeHeldRole(RoleManager.ROLE_BROWSER); c.resume()
        assertEquals(context.getString(R.string.status_inactive), a.findViewById<TextView>(R.id.status).text.toString())
        assertNull(shadowOf(a).nextStartedActivity)
        c.pause().stop().destroy()
    }
    @Test fun `sessao perdida nao restaura desafio nem arma aprendizado ao recriar tela`() {
        installUiBrowser(context); held()
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.openPage(SettingsPage.CALIBRATION); a.copyCalibrationTest()
        val phrase = requireNotNull(FinderCalibration.process.current()).phrase
        val bundle = Bundle(); c.saveInstanceState(bundle)
        bundle.keySet().filter { it.startsWith("ui.") }.forEach { assertFalse(bundle.get(it).toString().contains(phrase)) }
        FinderCalibration.process.cancel(); c.recreate()
        assertEquals(context.getString(R.string.calibration_ended), c.get().findViewById<TextView>(R.id.calibration_help).text.toString())
        assertEquals("", c.get().findViewById<TextView>(R.id.calibration_status).text.toString())
        assertNull(FinderCalibration.process.current())
        c.pause().stop().destroy()
    }
    @Test fun `rolagem e pagina sobrevivem ida a outro app e recriacao`() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.openPage(SettingsPage.HELP); a.toggleSupport(); a.setDiagnostics(true)
        AppPreferences(context).recordDiagnostic("Metadados\n".repeat(80)); a.toggleDiagnosticDetails()
        val root = a.findViewById<View>(R.id.root)
        fun layout() { root.measure(View.MeasureSpec.makeMeasureSpec(360, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY)); root.layout(0, 0, 360, 640); idle() }
        layout(); a.findViewById<ScrollView>(R.id.page_scroll).scrollTo(0, 200)
        val before = a.findViewById<ScrollView>(R.id.page_scroll).scrollY
        assertTrue(before > 0)
        c.pause().resume(); layout()
        assertEquals(before, a.findViewById<ScrollView>(R.id.page_scroll).scrollY)
        c.recreate(); idle()
        assertNotNull(c.get().findViewById<View>(R.id.finder_problem))
        c.pause().stop().destroy()
    }
    @Test @Config(sdk = [29]) fun `novas areas podem ser usadas no Android 10`() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        SettingsPage.entries.forEach { c.get().openPage(it) }
        assertFalse(AppPreferences(context).experimentalFinderDetection)
        assertNull(shadowOf(c.get()).nextStartedActivity)
        c.pause().stop().destroy()
    }

    @Test fun `desfazer pela interface restaura padrao desativado`() {
        val store = FinderSignatureStore(context)
        val client = store.all().first().client
        store.setEnabled(client, false)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.openPage(SettingsPage.PATTERNS)
        val list = a.findViewById<android.widget.LinearLayout>(R.id.patterns_list)
        val row = (0 until list.childCount).map { list.getChildAt(it) }.first { it.findViewById<TextView>(R.id.pattern_client).text.toString() == client }
        row.findViewById<MaterialButton>(R.id.pattern_delete).performClick(); idle()
        assertFalse(store.all().any { it.client == client })
        val undo = requireNotNull(a.findViewById<View>(com.google.android.material.R.id.snackbar_action))
        undo.performClick(); idle()
        assertTrue(store.all().any { it.client == client && !it.enabled })
        c.pause().stop().destroy()
    }

    @Test fun `alternar areas existentes nao acumula paginas repetidas`() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        repeat(4) { a.openPage(SettingsPage.CONFIGURATION); a.openPage(SettingsPage.HELP) }
        a.openPage(SettingsPage.CONFIGURATION)
        a.onBackPressedDispatcher.onBackPressed()
        assertNotNull(a.findViewById<View>(R.id.home_primary))
        c.pause().stop().destroy()
    }

    @Test fun `estado legado do switch nao reverte preferencia alterada enquanto tela estava fechada`() {
        val prefs = AppPreferences(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); c.get().openPage(SettingsPage.CONFIGURATION)
        c.get().findViewById<MaterialSwitch>(R.id.experimental).apply {
            isSaveEnabled = true; isSaveFromParentEnabled = true // Simula Bundle da versão anterior.
        }
        val state = Bundle(); c.saveInstanceState(state); c.pause().stop().destroy()
        prefs.experimentalFinderDetection = true
        val restored = Robolectric.buildActivity(SettingsActivity::class.java).create(state).start().restoreInstanceState(state).resume().visible()
        assertTrue(prefs.experimentalFinderDetection)
        assertTrue(restored.get().findViewById<MaterialSwitch>(R.id.experimental).isChecked)
        assertFalse(restored.get().findViewById<MaterialSwitch>(R.id.experimental).isSaveFromParentEnabled)
        restored.pause().stop().destroy()
    }
    @Test fun `regras mistas nao recebem mesmo checked do estado de view legado`() {
        val store = FinderSignatureStore(context)
        val first = store.all().first().client
        store.setEnabled(first, false)
        val expected = store.all()
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); c.get().openPage(SettingsPage.PATTERNS)
        val rows = c.get().findViewById<android.widget.LinearLayout>(R.id.patterns_list)
        repeat(rows.childCount) { rows.getChildAt(it).findViewById<MaterialSwitch>(R.id.pattern_toggle).apply { isSaveEnabled = true; isSaveFromParentEnabled = true } }
        val state = Bundle(); c.saveInstanceState(state); c.pause().stop().destroy()
        val restored = Robolectric.buildActivity(SettingsActivity::class.java).create(state).start().restoreInstanceState(state).resume().visible()
        assertEquals(expected, store.all())
        val newRows = restored.get().findViewById<android.widget.LinearLayout>(R.id.patterns_list)
        repeat(newRows.childCount) { assertFalse(newRows.getChildAt(it).findViewById<MaterialSwitch>(R.id.pattern_toggle).isSaveFromParentEnabled) }
        restored.pause().stop().destroy()
    }
}
