package br.com.findertogoogle.routing

import android.content.ComponentName
import android.net.Uri
import br.com.findertogoogle.data.BrowserSelection

internal class MemorySelection : BrowserSelection {
    override var selectedBrowserPackage: String? = null
    override var preferredBrowserComponent: ComponentName? = null
    override fun selectBrowser(component: ComponentName) {
        selectedBrowserPackage = component.packageName
        preferredBrowserComponent = component
    }
}
internal class TestBrowsers(var targets: List<BrowserTarget>) : BrowserCatalog {
    var resolver: ((String, Uri, ComponentName?) -> BrowserTarget?)? = null
    override fun installedBrowsers() = targets.map { BrowserApp(it.component.packageName, it.label) }.distinctBy { it.packageName }
    override fun resolve(packageName: String, uri: Uri, preferred: ComponentName?): BrowserTarget? {
        resolver?.let { return it(packageName, uri, preferred) }
        return targets.firstOrNull { it.component.packageName == packageName }
    }
}
