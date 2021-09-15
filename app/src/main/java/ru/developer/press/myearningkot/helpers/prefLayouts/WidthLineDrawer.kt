package ru.developer.press.myearningkot.helpers.prefLayouts


import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.forEach
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.helpers.getColorFromRes

class WidthLineDrawer(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    val positionList: MutableList<Float> = mutableListOf()
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(7f, 7f), 15f)
        color = context.getColorFromRes(R.color.textColorSecondary)
        strokeWidth = 3f
    }

    override fun onDraw(canvas: Canvas?) {

        positionList.forEach {
            canvas?.drawLine(
                it,
                0f,
                it,
                resources.displayMetrics.heightPixels.toFloat(),
                paint
            )

        }
    }

    fun updateLines(columnContainer: LinearLayout, offset: Int) {
        positionList.clear()
        columnContainer.forEach { title ->
            val rightPosition =
                title.x + title.width - offset
            positionList.add(
                rightPosition
            )
        }
        invalidate()
    }
}