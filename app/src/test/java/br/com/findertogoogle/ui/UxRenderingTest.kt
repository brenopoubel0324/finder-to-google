package br.com.findertogoogle.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import br.com.findertogoogle.R
import br.com.findertogoogle.routing.FinderCalibration
import com.google.android.material.button.MaterialButton
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w393dp-h852dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UxRenderingTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() {
        listOf("router_settings", "finder_rules").forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        FinderCalibration.process.cancel()
    }
    private fun layout(root: View, width: Int, height: Int = 852) {
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height); shadowOf(Looper.getMainLooper()).idle()
        // ActionMenu schedules one layout after deciding which items fit.
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        root.layout(0, 0, width, height); shadowOf(Looper.getMainLooper()).idle()
    }
    private fun checkText(view: View) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotEmpty()) {
            view.layout?.let { layout -> repeat(layout.lineCount) { assertEquals("Text clipped: ${view.text}", 0, layout.getEllipsisCount(it)) } }
            if (view is MaterialButton) assertTrue("Small tap target: ${view.text}", view.measuredHeight >= 48)
        }
        if (view is ViewGroup) repeat(view.childCount) { checkText(view.getChildAt(it)) }
    }
    @Suppress("DEPRECATION")
    @Test fun `fontes e larguras mantem texto e acoes legiveis`() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        val root = a.findViewById<View>(R.id.root)
        listOf(1f, 1.3f, 2f).forEach { scale ->
            a.resources.updateConfiguration(Configuration(a.resources.configuration).apply { fontScale = scale }, a.resources.displayMetrics)
            listOf(320, 360, 412, 600, 840).forEach { width ->
                SettingsPage.entries.forEach { page ->
                    a.openPage(page); layout(root, width)
                    assertTrue(a.findViewById<View>(R.id.page_content).measuredWidth <= 640)
                    checkText(a.findViewById(R.id.page_content))
                }
            }
        }
        assertNull(shadowOf(a).nextStartedActivity)
        c.pause().stop().destroy()
    }
    @Test fun `captura previa clara da interface renderizada`() { previews("day") }
    @Test @Config(qualifiers = "w393dp-h852dp-night-mdpi") fun `captura previa escura da interface renderizada`() { previews("night") }

    @Test @Config(qualifiers = "w840dp-h393dp-land-mdpi") fun `tablet em paisagem usa margem ampla e coluna limitada`() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        assertEquals(24, a.resources.getDimensionPixelSize(R.dimen.screen_gutter))
        val root = a.findViewById<View>(R.id.root)
        SettingsPage.entries.forEach { a.openPage(it); layout(root, 840, 393); assertTrue(a.findViewById<View>(R.id.page_content).measuredWidth <= 640); checkText(a.findViewById(R.id.page_content)) }
        c.pause().stop().destroy()
    }

    @Suppress("DEPRECATION")
    @Test fun `cadastro manual permite rolar com fonte grande e viewport de teclado`() {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        a.resources.updateConfiguration(Configuration(a.resources.configuration).apply { fontScale = 2f }, a.resources.displayMetrics)
        val form = a.layoutInflater.inflate(R.layout.dialog_signature, null) as android.widget.ScrollView
        layout(form, 300, 220)
        val input = form.findViewById<View>(R.id.signature_input)
        assertFalse(input.isSaveEnabled)
        form.smoothScrollTo(0, form.getChildAt(0).height)
        shadowOf(Looper.getMainLooper()).idle()
        form.scrollTo(0, form.getChildAt(0).height)
        assertTrue(form.getChildAt(0).measuredHeight > form.measuredHeight)
        assertTrue(form.scrollY > 0)
        c.pause().stop().destroy()
    }
    private fun previews(theme: String) {
        val c = Robolectric.buildActivity(SettingsActivity::class.java).setup(); val a = c.get()
        val root = a.findViewById<View>(R.id.root)
        val out = File("build/reports/ux-preview").apply { mkdirs() }
        fun save(name: String) {
            layout(root, 393)
            val image = Bitmap.createBitmap(393, 852, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            canvas.drawColor(a.getColor(R.color.app_surface))
            root.draw(canvas)
            File(out, "$theme-$name.png").outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            image.recycle()
        }
        save("home-start")
        installUiBrowser(context)
        shadowOf(context.getSystemService(RoleManager::class.java)).apply { addAvailableRole(RoleManager.ROLE_BROWSER); addHeldRole(RoleManager.ROLE_BROWSER) }
        c.pause().resume(); save("home-ready")
        a.openPage(SettingsPage.SETUP); save("setup-finder")
        a.openPage(SettingsPage.CALIBRATION); a.copyCalibrationTest(); save("calibration")
        a.openPage(SettingsPage.PATTERNS); save("patterns")
        assertNull(shadowOf(a).nextStartedActivity)
        FinderCalibration.process.cancel(); c.pause().stop().destroy()
    }
}
