package br.com.findertogoogle.ui

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import br.com.findertogoogle.R
import br.com.findertogoogle.routing.BrowserApp
import br.com.findertogoogle.routing.BrowserForwarder
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun AppCompatActivity.prepareEdgeToEdge() { enableEdgeToEdge() }
fun AppCompatActivity.applySystemInsets() {
    val root = findViewById<View>(R.id.root)
    val left = root.paddingLeft; val top = root.paddingTop; val right = root.paddingRight; val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
        view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
        insets
    }
    ViewCompat.requestApplyInsets(root)
}
fun AppCompatActivity.showBrowserPicker(forwarder: BrowserForwarder, onSelected: () -> Unit) {
    val browsers = forwarder.availableBrowsers()
    val dialog = MaterialAlertDialogBuilder(this).setTitle(R.string.choose_browser)
    if (browsers.isEmpty()) { dialog.setMessage(R.string.no_browsers).setPositiveButton(android.R.string.ok, null).show(); return }
    val current = forwarder.selectedTarget()?.packageName
    val duplicateLabels = browsers.groupingBy { it.label }.eachCount().filterValues { it > 1 }.keys
    val adapter = object : ArrayAdapter<BrowserApp>(this, android.R.layout.select_dialog_singlechoice, browsers) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = browsers[position]
            return (super.getView(position, convertView, parent) as CheckedTextView).apply {
                text = if (item.label in duplicateLabels) "${item.label}\n${item.packageName}" else item.label
                isSingleLine = false
                val size = (32 * resources.displayMetrics.density).toInt()
                val icon = try { packageManager.getApplicationIcon(item.packageName) }
                    catch (_: Exception) { AppCompatResources.getDrawable(context, R.drawable.ic_browser) }
                icon?.setBounds(0, 0, size, size)
                setCompoundDrawablesRelative(icon, null, null, null)
                compoundDrawablePadding = (16 * resources.displayMetrics.density).toInt()
                minHeight = (64 * resources.displayMetrics.density).toInt()
            }
        }
    }
    dialog.setSingleChoiceItems(adapter, browsers.indexOfFirst { it.packageName == current }) { picker, index ->
        if (forwarder.select(browsers[index])) { picker.dismiss(); onSelected() }
        else { Toast.makeText(this, R.string.selection_changed, Toast.LENGTH_LONG).show(); picker.dismiss() }
    }.setNegativeButton(R.string.cancel, null).show()
}
