package ru.developer.press.myearningkot.helpers.prefLayouts

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.helpers.getColorFromRes


class MySwitch : SwitchCompat {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        changeColor(checked)
    }

    private fun changeColor(isChecked: Boolean) {
        val thumbColor: Int
        val trackColor: Int
        if (isChecked) {
            thumbColor = context.getColorFromRes(R.color.colorControlEnabled)
            trackColor = thumbColor
        } else {
            thumbColor = context.getColorFromRes(R.color.colorControlNormal)
            trackColor = context.getColorFromRes(R.color.textColorSecondary)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                thumbDrawable.colorFilter = PorterDuffColorFilter(thumbColor, PorterDuff.Mode.MULTIPLY)
                trackDrawable.colorFilter = PorterDuffColorFilter(trackColor, PorterDuff.Mode.MULTIPLY)
            } else {
                thumbDrawable.setColorFilter(thumbColor, PorterDuff.Mode.MULTIPLY)
                trackDrawable.setColorFilter(trackColor, PorterDuff.Mode.MULTIPLY)
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }
}