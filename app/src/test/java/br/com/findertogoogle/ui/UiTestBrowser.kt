package br.com.findertogoogle.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import br.com.findertogoogle.data.AppPreferences
import org.robolectric.Shadows.shadowOf

fun installUiBrowser(context: Context, selected: Boolean = true): ComponentName {
    val component = ComponentName("org.ui.browser", "org.ui.browser.Main")
    val pm = shadowOf(context.packageManager)
    pm.addActivityIfNotPresent(component).apply {
        enabled = true; exported = true
        applicationInfo = ApplicationInfo().apply { packageName = component.packageName; enabled = true }
        nonLocalizedLabel = "Navegador de teste"
    }
    pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_VIEW).apply {
        addCategory(Intent.CATEGORY_DEFAULT); addCategory(Intent.CATEGORY_BROWSABLE)
        addDataScheme("http"); addDataScheme("https")
    })
    if (selected) AppPreferences(context).selectBrowser(component)
    return component
}
