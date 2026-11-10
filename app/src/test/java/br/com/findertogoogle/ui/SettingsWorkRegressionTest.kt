package br.com.findertogoogle.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.R
import br.com.findertogoogle.routing.FinderCalibration
import br.com.findertogoogle.data.FinderSignatureStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [CountingPackageManager::class, CountingRoleManager::class])
class SettingsWorkRegressionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() {
        listOf("router_settings", "finder_rules").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        FinderCalibration.process.cancel()
        shadowOf(context.getSystemService(RoleManager::class.java)).apply {
            addAvailableRole(RoleManager.ROLE_BROWSER); addHeldRole(RoleManager.ROLE_BROWSER)
        }
    }
    private fun clearCounts() { CountingPackageManager.clearCounts(); CountingRoleManager.clearCounts() }
    private fun idle() { shadowOf(Looper.getMainLooper()).idle() }
    private fun SettingsActivity.body(): View = findViewById<ViewGroup>(R.id.page_content).getChildAt(0)
    private fun packageChanged(pkg: String) {
        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_CHANGED, Uri.parse("package:$pkg")))
        idle()
    }
    @Test fun `acoes locais nao consultam sistema e estado igual preserva Views`() {
        installUiBrowser(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.openPage(SettingsPage.HELP)
        clearCounts()
        repeat(20) { a.toggleSupport() }
        val previous = a.body()
        a.setDiagnostics(false)
        assertSame(previous, a.body())
        assertEquals(0, CountingPackageManager.globalQueries + CountingPackageManager.scopedQueries)
        assertEquals(0, CountingRoleManager.heldQueries + CountingRoleManager.availableQueries)
        c.pause().stop().destroy()
    }
    @Test fun `retomar revalida estado real sem recriar pagina igual`() {
        installUiBrowser(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        val previous = a.body(); clearCounts()
        c.pause().resume(); idle()
        assertSame(previous, a.body())
        assertEquals(0, CountingPackageManager.globalQueries)
        assertEquals(2, CountingPackageManager.scopedQueries)
        assertEquals(1, CountingRoleManager.heldQueries)
        shadowOf(context.getSystemService(RoleManager::class.java)).removeHeldRole(RoleManager.ROLE_BROWSER)
        c.pause().resume()
        assertEquals(a.getString(R.string.status_inactive), a.findViewById<TextView>(R.id.status).text.toString())
        c.pause().stop().destroy()
    }
    @Test fun `evento do pacote escolhido invalida cache mas pacote alheio nao faz varredura`() {
        val component = installUiBrowser(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        val previous = a.body(); clearCounts()
        packageChanged("org.unrelated.app")
        assertSame(previous, a.body())
        assertEquals(0, CountingPackageManager.globalQueries + CountingPackageManager.scopedQueries)
        shadowOf(context.packageManager).removeActivity(component)
        packageChanged(component.packageName)
        assertEquals(a.getString(R.string.browser_missing), a.findViewById<TextView>(R.id.selected_browser).text.toString())
        assertEquals(0, CountingPackageManager.globalQueries)
        c.pause().stop().destroy()
    }
    @Test fun `background nao observa eventos e retomada recupera alteracoes perdidas`() {
        val component = installUiBrowser(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        c.pause().stop(); clearCounts()
        shadowOf(context.packageManager).removeActivity(component)
        packageChanged(component.packageName)
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofHours(1))
        assertEquals(0, CountingPackageManager.globalQueries + CountingPackageManager.scopedQueries)
        assertEquals(0, CountingRoleManager.heldQueries + CountingRoleManager.availableQueries)
        c.restart().start().resume(); idle()
        assertEquals(a.getString(R.string.browser_missing), a.findViewById<TextView>(R.id.selected_browser).text.toString())
        c.pause().stop().destroy()
    }
    @Test fun `acao com papel revogado nao usa autorizacao em cache`() {
        installUiBrowser(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        shadowOf(context.getSystemService(RoleManager::class.java)).removeHeldRole(RoleManager.ROLE_BROWSER)
        a.copyCalibrationTest()
        assertNull(FinderCalibration.process.current())
        assertEquals(View.VISIBLE, a.findViewById<View>(R.id.step_android).visibility)
        c.pause().stop().destroy()
    }
    @Test fun `link nao usa disponibilidade da apresentacao para autorizar destino removido`() {
        val component = installUiBrowser(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        assertEquals("Navegador de teste", a.findViewById<TextView>(R.id.selected_browser).text.toString())
        shadowOf(context.packageManager).removeActivity(component)
        // Sem evento/retomada: a apresentação ainda está aquecida. A ação é ao vivo.
        a.testBrowser()
        assertNull(shadowOf(a).nextStartedActivity)
        assertTrue(org.robolectric.shadows.ShadowDialog.getLatestDialog().isShowing)
        c.pause().stop().destroy()
    }
    @Test fun `troca de regra com mesma contagem atualiza lista`() {
        val store = FinderSignatureStore(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.openPage(SettingsPage.PATTERNS)
        val previous = a.body()
        store.remove(store.all().first().client); store.save("padrao-substituto")
        c.pause().resume()
        assertNotSame(previous, a.body())
        val list = a.findViewById<android.widget.LinearLayout>(R.id.patterns_list)
        assertTrue((0 until list.childCount).map { list.getChildAt(it).findViewById<TextView>(R.id.pattern_client).text.toString() }.contains("padrao-substituto"))
        c.pause().stop().destroy()
    }
    @Test fun `nova Activity apos perda de processo nao restaura cache ou autorizacao antiga`() {
        val component = installUiBrowser(context)
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        val saved = Bundle(); c.saveInstanceState(saved); c.pause().stop().destroy()
        shadowOf(context.packageManager).removeActivity(component)
        shadowOf(context.getSystemService(RoleManager::class.java)).removeHeldRole(RoleManager.ROLE_BROWSER)
        FinderCalibration.process.cancel()
        val restored = Robolectric.buildActivity(SettingsActivity::class.java).create(saved).start().restoreInstanceState(saved).resume().visible()
        assertEquals(restored.get().getString(R.string.status_inactive), restored.get().findViewById<TextView>(R.id.status).text.toString())
        assertEquals(restored.get().getString(R.string.browser_missing), restored.get().findViewById<TextView>(R.id.selected_browser).text.toString())
        assertNull(shadowOf(restored.get()).nextStartedActivity)
        restored.pause().stop().destroy()
    }
}
