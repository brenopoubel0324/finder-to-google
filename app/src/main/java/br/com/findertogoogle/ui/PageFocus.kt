package br.com.findertogoogle.ui

import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo

/** Conserva foco somente quando o mesmo controle continua visível após renderizar. */
internal object PageFocus {
    data class Saved(val id: Int, val tag: Any?, val accessibility: Boolean)
    fun capture(root: View): Saved? {
        if (root.isAccessibilityFocused) return Saved(root.id, root.tag, true)
        if (root.hasFocus() && root !is ViewGroup) return Saved(root.id, root.tag, false)
        if (root is ViewGroup) repeat(root.childCount) { capture(root.getChildAt(it))?.let { found -> return found } }
        return null
    }
    fun restore(root: View, saved: Saved?) {
        if (saved == null || saved.id == View.NO_ID) return
        fun match(view: View): View? {
            if (view.visibility != View.VISIBLE) return null
            if (view.id == saved.id && view.tag == saved.tag) return view
            if (view is ViewGroup) repeat(view.childCount) { match(view.getChildAt(it))?.let { found -> return found } }
            return null
        }
        match(root)?.let { view ->
            if (saved.accessibility) view.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null)
            else view.requestFocus()
        }
    }
}
