package ru.developer.press.myearningkot.helpers

import android.content.DialogInterface
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import org.jetbrains.anko.backgroundColor
import ru.developer.press.myearningkot.R

fun AlertDialog.setAlertButtonColors(
    colorPositiveRes: Int = R.color.colorAccent,
    colorNegativeRes: Int = R.color.colorAccent,
    colorNeutralRes: Int = R.color.colorAccent
) {
    getButton(DialogInterface.BUTTON_POSITIVE).apply {
        backgroundColor = Color.TRANSPARENT
        setTextColor(context.colorRes(colorPositiveRes))
    }
    getButton(DialogInterface.BUTTON_NEGATIVE).apply {
        backgroundColor = Color.TRANSPARENT
        setTextColor(context.colorRes(colorNegativeRes))
    }
    getButton(DialogInterface.BUTTON_NEUTRAL).apply {
        backgroundColor = Color.TRANSPARENT
        setTextColor(context.colorRes(colorNeutralRes))
    }
}

fun android.app.AlertDialog.setAlertButtonColors(
    colorPositiveRes: Int = R.color.colorAccent,
    colorNegativeRes: Int = R.color.colorAccent,
    colorNeutralRes: Int = R.color.colorAccent
) {
    getButton(DialogInterface.BUTTON_POSITIVE).apply {
        backgroundColor = Color.TRANSPARENT
        setTextColor(context.colorRes(colorPositiveRes))
    }
    getButton(DialogInterface.BUTTON_NEGATIVE).apply {
        backgroundColor = Color.TRANSPARENT
        setTextColor(context.colorRes(colorNegativeRes))
    }
    getButton(DialogInterface.BUTTON_NEUTRAL).apply {
        backgroundColor = Color.TRANSPARENT
        setTextColor(context.colorRes(colorNeutralRes))
    }
}

fun AlertDialog.setAlertButtonColorsAfterShown(
    colorPositiveRes: Int = R.color.colorAccent,
    colorNegativeRes: Int = R.color.colorAccent,
    colorNeutralRes: Int = R.color.colorAccent
) {
    setOnShowListener {
        setAlertButtonColors(colorPositiveRes, colorNegativeRes, colorNeutralRes)
    }
}


fun android.app.AlertDialog.setAlertButtonColorsAfterShown(
    colorPositiveRes: Int = R.color.colorAccent,
    colorNegativeRes: Int = R.color.colorAccent,
    colorNeutralRes: Int = R.color.colorAccent
) {
    setOnShowListener {
        setAlertButtonColors(colorPositiveRes, colorNegativeRes, colorNeutralRes)
    }
}