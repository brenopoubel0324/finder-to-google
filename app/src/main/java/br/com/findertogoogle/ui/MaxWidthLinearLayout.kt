package br.com.findertogoogle.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import br.com.findertogoogle.R

/** Mantém a leitura confortável em tablet sem comprimir celulares. */
class MaxWidthLinearLayout(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maximum = resources.getDimensionPixelSize(R.dimen.content_max_width)
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val size = MeasureSpec.getSize(widthMeasureSpec)
        val width = if (mode == MeasureSpec.UNSPECIFIED) maximum else minOf(maximum, size)
        val constrained = MeasureSpec.makeMeasureSpec(width, if (mode == MeasureSpec.EXACTLY) mode else MeasureSpec.AT_MOST)
        super.onMeasure(constrained, heightMeasureSpec)
    }
}
