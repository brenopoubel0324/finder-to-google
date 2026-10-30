package br.com.findertogoogle.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.R
import br.com.findertogoogle.data.AppPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActivityRecoveryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun clearPreferences() {
        context.getSharedPreferences("router_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `link sem navegador mostra recuperacao sem perder URL nem abrir implicitamente`() {
        val url = "https://example.com/p?a=1&b=2#abc"
        val controller = Robolectric.buildActivity(LinkRouterActivity::class.java, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            .setup()
        val activity = controller.get()
        assertFalse(activity.isFinishing)
        assertEquals(activity.getString(R.string.recovery_not_configured), activity.findViewById<TextView>(R.id.recovery_message).text.toString())
        assertEquals(url, activity.intent.dataString)
        assertNull(shadowOf(activity).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `diagnostico nao guarda URL termo nem extras de origem falsificada`() {
        val prefs = AppPreferences(context).apply { diagnosticsEnabled = true }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com/search?q=SEGREDO_UNICO"))
            .putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://com.samsung.android.app.galaxyfinder"))
        val controller = Robolectric.buildActivity(LinkRouterActivity::class.java, intent).setup()
        val diagnostic = requireNotNull(prefs.lastDiagnostic)
        assertFalse(diagnostic.contains("SEGREDO_UNICO"))
        assertFalse(diagnostic.contains("https://"))
        assertTrue(diagnostic.contains(context.getString(R.string.origin_unknown)))
        assertNull(shadowOf(controller.get()).nextStartedActivity)
        controller.pause().stop().destroy()
    }

    @Test fun `tela de configuracao pode ser criada em Android 16`() {
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        assertNotNull(controller.get().findViewById<TextView>(R.id.status))
        controller.pause().stop().destroy()
    }

    @Test @Config(sdk = [29]) fun `Android 10 nao chama API de identidade do Android 14`() {
        val controller = Robolectric.buildActivity(LinkRouterActivity::class.java,
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/"))).setup()
        assertFalse(controller.get().isFinishing)
        assertNull(shadowOf(controller.get()).nextStartedActivity)
        controller.pause().stop().destroy()
    }
    @Test fun `recuperacao recriada nao dispara novamente pesquisa e link novo e independente`() {
        AppPreferences(context).experimentalFinderDetection = true
        val url = "https://google.com/search?q=teste&client=tablet-android-samsung-rvo1"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val state = Bundle().apply {
            putBoolean("router.recovering", true)
            putString("router.reason", "NO_LONGER_AVAILABLE")
            putString("router.decision", "GOOGLE_EXPERIMENTAL")
            putString("router.google_failure", "UNAVAILABLE")
        }
        val recovering = Robolectric.buildActivity(LinkRouterActivity::class.java, intent).create(state).start().resume().visible()
        assertFalse(recovering.get().isFinishing)
        assertNull(shadowOf(recovering.get()).nextStartedActivity)
        recovering.recreate()
        assertNull(shadowOf(recovering.get()).nextStartedActivity)
        assertEquals(url, recovering.get().intent.dataString)
        recovering.pause().stop().destroy()
        val fresh = Robolectric.buildActivity(LinkRouterActivity::class.java, Intent(intent)).setup()
        assertEquals(Intent.ACTION_WEB_SEARCH, shadowOf(fresh.get()).nextStartedActivity?.action)
        fresh.pause().stop().destroy()
    }

    @Test fun `configuracao atualizada preserva compatibilidade e avancado pode copiar diagnostico`() {
        val prefs = AppPreferences(context).apply {
            experimentalFinderDetection = true
            diagnosticsEnabled = true
            recordDiagnostic("Somente metadados")
        }
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        val activity = controller.get()
        assertTrue(activity.findViewById<MaterialSwitch>(R.id.experimental).isChecked)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.advanced_content).visibility)
        activity.findViewById<MaterialButton>(R.id.advanced_toggle).performClick()
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.advanced_content).visibility)
        assertTrue(activity.findViewById<MaterialButton>(R.id.copy_diagnostics).isEnabled)
        activity.findViewById<MaterialSwitch>(R.id.diagnostics).isChecked = false
        assertNull(prefs.lastDiagnostic)
        assertFalse(activity.findViewById<MaterialButton>(R.id.copy_diagnostics).isEnabled)
        assertTrue(prefs.experimentalFinderDetection)
        controller.pause().stop().destroy()
    }

    @Test fun `layout limita largura no tablet e nao ultrapassa largura do telefone`() {
        val controller = Robolectric.buildActivity(SettingsActivity::class.java).setup()
        val activity = controller.get()
        val view = (activity.findViewById<View>(R.id.root) as android.view.ViewGroup).getChildAt(0)
        assertTrue(view is MaxWidthLinearLayout)
        val density = activity.resources.displayMetrics.density
        listOf(320, 600, 840).forEach { width ->
            val pixels = (width * density).toInt()
            view.measure(View.MeasureSpec.makeMeasureSpec(pixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            assertTrue(view.measuredWidth <= pixels)
            assertTrue(view.measuredWidth <= (640 * density).toInt())
        }
        controller.pause().stop().destroy()
    }

}
