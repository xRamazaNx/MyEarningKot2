package ru.developer.press.myearningkot.helpers.prefLayouts

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.helpers.colorRes


class MySwitch : SwitchCompat {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
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
            thumbColor = context.colorRes(R.color.colorControlEnabled)
            trackColor = thumbColor
        } else {
            thumbColor = context.colorRes(R.color.colorControlNormal)
            trackColor = context.colorRes(R.color.textColorSecondary)
        }
        try {
            post {
                thumbDrawable.colorFilter =
                    PorterDuffColorFilter(thumbColor, PorterDuff.Mode.MULTIPLY)
                trackDrawable.colorFilter =
                    PorterDuffColorFilter(trackColor, PorterDuff.Mode.MULTIPLY)

            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }
}