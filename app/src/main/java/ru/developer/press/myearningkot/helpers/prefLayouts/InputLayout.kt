package ru.developer.press.myearningkot.helpers.prefLayouts

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.database.gson
import ru.developer.press.myearningkot.helpers.getColorFromRes
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
            lparams(matchParent, dip(45))
            setPadding(padding, dip(8), padding, dip(8))

            var textCell: TextView? = null
            when (cell.type) {
                ColumnType.TEXT,
                ColumnType.NUMBER,
                ColumnType.PHONE,
                ColumnType.DATE -> {
                    imageView(R.drawable.ic_backspace) {
                        imageTintList =
                            ColorStateList.valueOf(context.getColorFromRes(R.color.colorRed))
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
                        textCell = textView(text(cell)) {
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

    private fun text(cell: Cell): String {
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