package br.com.findertogoogle.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
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
}
