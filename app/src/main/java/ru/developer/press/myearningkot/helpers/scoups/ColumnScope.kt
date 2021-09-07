package ru.developer.press.myearningkot.helpers.scoups

import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.dip
import ru.developer.press.myearningkot.model.Column
import ru.developer.press.myearningkot.model.NumerationColumn

fun Column.inflateView(title: TextView) {
    var w = 1f
    if (this is NumerationColumn) {
        w = 0f
    }

    title.layoutParams =
        LinearLayout.LayoutParams(
            width,
            title.dip(35)
        ).apply {
            gravity = Gravity.CENTER
            weight = w
        }

    title.text = name
    titlePref.customize(title)
}