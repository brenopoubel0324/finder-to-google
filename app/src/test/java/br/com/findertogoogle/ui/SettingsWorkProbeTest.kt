package br.com.findertogoogle.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Looper
import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.R
import br.com.findertogoogle.routing.FinderCalibration
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowApplicationPackageManager
import org.robolectric.shadows.ShadowRoleManager
import java.io.File

@Implements(className = "android.app.ApplicationPackageManager", isInAndroidSdk = false)
class CountingPackageManager : ShadowApplicationPackageManager() {
    @Implementation
    override fun queryIntentActivities(intent: Intent, flags: Int): MutableList<ResolveInfo> {
        if (intent.action == Intent.ACTION_VIEW && intent.hasCategory(Intent.CATEGORY_BROWSABLE)) {
            if (intent.`package` == null) globalQueries++ else scopedQueries++
        }
        return super.queryIntentActivities(intent, flags)
    }
    companion object {
        var globalQueries = 0
        var scopedQueries = 0
        fun clearCounts() { globalQueries = 0; scopedQueries = 0 }
    }
}

@Implements(RoleManager::class)
class CountingRoleManager : ShadowRoleManager() {
    @Implementation override fun isRoleHeld(roleName: String): Boolean { heldQueries++; return super.isRoleHeld(roleName) }
    @Implementation override fun isRoleAvailable(roleName: String): Boolean { availableQueries++; return super.isRoleAvailable(roleName) }
    companion object {
        var heldQueries = 0
        var availableQueries = 0
        fun clearCounts() { heldQueries = 0; availableQueries = 0 }
    }
}

/** Mede trabalho no mesmo harness Robolectric; não é medição de energia/dispositivo. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [CountingPackageManager::class, CountingRoleManager::class])
class SettingsWorkProbeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun reset() {
        listOf("router_settings", "finder_rules").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        FinderCalibration.process.cancel()
        installUiBrowser(context)
        shadowOf(context.getSystemService(RoleManager::class.java)).apply {
            addAvailableRole(RoleManager.ROLE_BROWSER); addHeldRole(RoleManager.ROLE_BROWSER)
        }
        clearCounts()
    }
    private fun clearCounts() { CountingPackageManager.clearCounts(); CountingRoleManager.clearCounts() }
    private fun counts(rebuilds: Int) = "{\"global_pm\":${CountingPackageManager.globalQueries},\"scoped_pm\":${CountingPackageManager.scopedQueries},\"role_held\":${CountingRoleManager.heldQueries},\"role_available\":${CountingRoleManager.availableQueries},\"page_rebuilds\":$rebuilds}"
    @Test fun `conta consultas e recriacoes em cenarios fixos`() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        val initial = counts(-1) // A criação anterior à captura de Views não é contada aqui.
        assertEquals("Navegador de teste", a.findViewById<android.widget.TextView>(R.id.selected_browser).text.toString())
        a.openPage(SettingsPage.HELP)
        clearCounts()
        var rebuilds = 0
        repeat(20) {
            val previous = a.findViewById<ViewGroup>(R.id.page_content).getChildAt(0)
            a.toggleSupport()
            if (previous !== a.findViewById<ViewGroup>(R.id.page_content).getChildAt(0)) rebuilds++
        }
        val local = counts(rebuilds)
        clearCounts(); rebuilds = 0
        repeat(10) {
            val previous = a.findViewById<ViewGroup>(R.id.page_content).getChildAt(0)
            c.pause().resume()
            shadowOf(Looper.getMainLooper()).idle()
            if (previous !== a.findViewById<ViewGroup>(R.id.page_content).getChildAt(0)) rebuilds++
        }
        val resume = counts(rebuilds)
        clearCounts()
        c.pause().stop()
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofHours(1))
        val background = counts(0)
        c.destroy()
        File("build/reports/settings-work.json").apply {
            requireNotNull(parentFile).mkdirs()
            writeText("{\"runtime\":\"Robolectric API 36\",\"initial\":$initial,\"local_20\":$local,\"resume_10\":$resume,\"stopped_1h\":$background}\n")
        }
    }
}
