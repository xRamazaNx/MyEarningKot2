package ru.developer.press.myearningkot.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.list_item_change_layout.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColorResource
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

@SuppressLint("InflateParams")
fun Context.showItemChangeDialog(
    title: String,
    list: MutableList<String>,
    _selectItem: Int,
    firstElementText: String?,
    itemClickEvent: (Int) -> Unit
) {
    val builder = android.app.AlertDialog.Builder(this).create()
    builder.apply {
        val linear: LinearLayout =
            layoutInflater.inflate(R.layout.list_item_change_layout, null) as LinearLayout
        linear.titleList.text = title
        val addItemInListButton = linear.addItemInListButton
        if (firstElementText == null) {
            addItemInListButton.visibility = View.GONE
        } else
            addItemInListButton.setOnClickListener {
                itemClickEvent(-1)
                builder.dismiss()
            }
        val itemListContainer = linear.itemListContainer
        val dpsToPixels = dip(16)
        fun setSelectedItemDecor(textView: TextView) {
            textView.textColorResource = R.color.colorAccent
            textView.textSize = 16f
            textView.setTypeface(textView.typeface, Typeface.BOLD)
//            list.forEachIndexed { index, it ->
//                if (it != textView.text.toString()){
//                    val item = itemListContainer.getChildAt(index) as TextView
//                    item.textColorResource = R.color.textColorTertiary
//                    item.textSize = 14f
//                    item.setTypeface(textView.typeface, Typeface.NORMAL)
//                }
//            }
        }
        list.forEachIndexed { index, name ->
            val itemTextView = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dip(8)
                    marginEnd = dip(8)
                }
                text = name
                textColorResource = R.color.textColorPrimary

                setPadding(dpsToPixels, dpsToPixels, dpsToPixels, dpsToPixels)
                setOnClickListener {
                    itemClickEvent(index)
                    post {
                        setSelectedItemDecor(this)
                        dismiss()
                    }
                }

                addClickEffectBackground()
            }

            itemListContainer.addView(itemTextView)
            if (index == _selectItem) {
                setSelectedItemDecor(itemTextView)
            }
        }
        setView(linear)

    }.show()
}