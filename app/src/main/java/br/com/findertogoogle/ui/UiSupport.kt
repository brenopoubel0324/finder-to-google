package br.com.findertogoogle.ui

import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.findertogoogle.R
import br.com.findertogoogle.routing.BrowserForwarder
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun AppCompatActivity.prepareEdgeToEdge() { enableEdgeToEdge() }

fun AppCompatActivity.applySystemInsets() {
    val root = findViewById<View>(R.id.root)
    val left = root.paddingLeft
    val top = root.paddingTop
    val right = root.paddingRight
    val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(root)
}

fun AppCompatActivity.showBrowserPicker(forwarder: BrowserForwarder, onSelected: () -> Unit) {
    val browsers = forwarder.availableBrowsers()
    val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.choose_browser)
    if (browsers.isEmpty()) {
        dialog.setMessage(R.string.no_browsers).setPositiveButton(android.R.string.ok, null).show()
        return
    }
    val current = forwarder.selectedTarget()?.component
    dialog.setSingleChoiceItems(
        browsers.map { "${it.label}\n${it.component.packageName}" }.toTypedArray(),
        browsers.indexOfFirst { it.component == current },
    ) { picker, index ->
        if (forwarder.select(browsers[index])) {
            picker.dismiss()
            onSelected()
        } else {
            Toast.makeText(this, R.string.selection_changed, Toast.LENGTH_LONG).show()
            picker.dismiss()
        }
    }.setNegativeButton(R.string.cancel, null).show()
}
