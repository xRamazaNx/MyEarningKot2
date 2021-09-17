package ru.developer.press.myearningkot.helpers.prefLayouts

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.ViewManager
import android.widget.LinearLayout
import android.widget.TextView
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import de.hdodenhof.circleimageview.CircleImageView
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.sdk27.coroutines.onClick
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.database.gson
import ru.developer.press.myearningkot.helpers.colorRes
import ru.developer.press.myearningkot.model.*
import java.util.*

class InputLayout private constructor(
    private val cell: Cell,
    private val card: Card,
    private val inputCallBack: InputCallBack
) {
    companion object {
        fun inflateInputLayout(
            context: Context,
            cell: Cell,
            card: Card,
            inputCallBack: InputCallBack
        ): LinearLayout {
            return InputLayout(cell, card, inputCallBack).inputView(context)
        }
    }

    private fun inputView(context: Context): LinearLayout {
        return context.linearLayout {
            val padding = dip(16)
            val height = dip(24)
            lparams(matchParent, wrapContent)
            minimumHeight = dip(45)
            setPadding(padding, dip(4), padding, dip(4))

            var textCell: TextView? = null
            when (cell.type) {
                ColumnType.TEXT,
                ColumnType.NUMBER,
                ColumnType.PHONE,
                ColumnType.DATE -> {
                    imageView(R.drawable.ic_backspace) {
                        imageTintList =
                            ColorStateList.valueOf(context.colorRes(R.color.colorRed))
                        onClick {
                            cell.clear()
                            textCell?.text = ""
                            inputCallBack.notifyCellChanged()
                        }
                    }.lparams(height, matchParent)
                }
                else -> {
                    imageView(R.drawable.ic_fullscreen)
                        .lparams((height * 1.2).toInt(), matchParent)
                        .onClick {
                            inputCallBack.openCellDialog()
                        }
                }
            }

            horizontalScrollView {
                setPadding(padding, 0, padding, 0)
                when (cell.type) {
                    ColumnType.TEXT,
                    ColumnType.NUMBER,
                    ColumnType.PHONE,
                    ColumnType.DATE -> {
                        textCell = textView(text()) {
                            textSize = 16f
                            gravity = Gravity.CENTER_VERTICAL
                            maxLines = 1
                            hint = context.getString(R.string.notData)
                            onClick {
                                inputCallBack.openCellDialog()
                            }
                        }.lparams(matchParent, matchParent, Gravity.START)
                    }
                    ColumnType.COLOR -> {
                        val currentColor = cell.sourceValue.toInt()
                        val colorList = mutableListOf<Int>()
                        colorList.addAll(ColorPickerDialog.MATERIAL_COLORS.toList())

                        if (colorList.contains(currentColor)) {
                            colorList.remove(currentColor)
                        }
                        colorList.add(0, currentColor)
                        linearLayout {
                            var oldCircleSelected: CircleImageView? = null
                            colorList.forEach { color ->
                                circleImageView(ColorDrawable(color)) {
                                    this.setPadding(dip(8), 0, dip(8), 0)
                                    borderColor = Color.BLACK
                                    borderWidth = dip(1)
                                    fun select() {
                                        oldCircleSelected?.let {
//                                            it.animate()
//                                                .scaleX(1f)
//                                                .scaleY(1f)
//                                                .setDuration(300)
//                                                .start()
                                            it.backgroundColor = Color.TRANSPARENT
                                        }
//                                        animate()
//                                            .scaleX(1.4f)
//                                            .scaleY(1.4f)
//                                            .setDuration(200)
//                                            .start()
                                        backgroundResource = R.drawable.background_for_color_in_input_layout
                                        oldCircleSelected = this
                                    }
                                    if (color == currentColor) {
                                        select()
                                    }
                                    onClick {
                                        select()
                                        cell.sourceValue = color.toString()
                                        inputCallBack.notifyCellChanged()
                                    }
                                }.lparams(dip(45), dip(45))
                            }
                        }.lparams(wrapContent, matchParent)
                    }
                    ColumnType.IMAGE -> {
                    }
                    else -> {
                    }
                }
            }.lparams(0, matchParent, 1f)

            imageView(R.drawable.ic_check) {
                onClick {
                    inputCallBack.close()
                }
            }.lparams((height * 1.2).toInt(), matchParent) {
                gravity = Gravity.END
            }
        }
    }

    private fun text(): String {
        var text = ""
        card.rows.forEach { row ->
            row.cellList.forEachIndexed { index, cell ->
                if (this.cell === cell) {
                    val column: Column = card.columns[index]
                    text = when (cell.type) {
                        ColumnType.TEXT -> cell.sourceValue
                        ColumnType.NUMBER -> {
                            column.let { numberColumn ->
                                if (numberColumn is NumberColumn) {
                                    text =
                                        if (numberColumn.inputType == InputTypeNumberColumn.FORMULA)
                                            numberColumn.formulaString(card, row)
                                        else
                                            cell.sourceValue
                                }
                            }
                            text
                        }
                        ColumnType.PHONE -> {
                            val typeValue: PhoneTypeValue =
                                gson.fromJson(cell.sourceValue, PhoneTypeValue::class.java)
                            val phoneTypePref = (column as PhoneColumn).pref()
                            typeValue.getPhoneInfo(phoneTypePref, false)
                        }
                        ColumnType.DATE -> {
                            if (cell.sourceValue.isNotEmpty()) {
                                val calendar = Calendar.getInstance()
                                calendar.timeInMillis = cell.sourceValue.toLong()
                                calendar.time.toString()
                            } else ""
                        }
                        else -> {
                            cell.displayValue
                        }
                    }
                }
            }
        }
        return text
    }

    interface InputCallBack {
        fun openCellDialog()
        fun notifyCellChanged()
        fun close()
    }
}

inline fun ViewManager.circleImageView(
    drawable: Drawable,
    init: CircleImageView.() -> Unit
): CircleImageView {
    return circleImageView(init).apply {
        setImageDrawable(drawable)
    }
}

inline fun ViewManager.circleImageView(init: (@AnkoViewDslMarker CircleImageView).() -> Unit): CircleImageView {
    return ankoView({ CircleImageView(it) }, theme = 0, init = init)
}