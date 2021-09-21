package ru.developer.press.myearningkot.helpers.prefLayouts

import android.annotation.SuppressLint
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
import ru.developer.press.myearningkot.helpers.addClickEffectRippleBackground
import ru.developer.press.myearningkot.helpers.animateRipple
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

    @SuppressLint("ClickableViewAccessibility")
    private fun inputView(context: Context): LinearLayout {
        val type = cell.type
        return context.linearLayout {
            val padding = dip(16)
            val width = dip(24)
            lparams(matchParent, wrapContent)
            minimumHeight = dip(45)
            setPadding(padding, dip(4), padding, dip(4))

            var textCell: TextView? = null
            when (type) {
                ColumnType.TEXT,
                ColumnType.NUMBER,
                ColumnType.PHONE,
                ColumnType.DATE -> {
                    imageView(R.drawable.ic_backspace) {
                        imageTintList =
                            ColorStateList.valueOf(context.colorRes(R.color.colorRed))
                         addClickEffectRippleBackground()
                        onClick {
                            cell.clear()
                            textCell?.text = ""
                            inputCallBack.notifyCellChanged()
                        }
                    }.lparams(width, matchParent)
                }
                else -> {
                    imageView(R.drawable.ic_fullscreen){
                        addClickEffectRippleBackground()
                    }
                        .lparams((width * 1.3).toInt(), matchParent)
                        .onClick {
                            inputCallBack.openCellDialog()
                        }
                }
            }

            horizontalScrollView {
//                val detector =
//                    GestureDetectorCompat(
//                        context,
//                        object : GestureDetector.SimpleOnGestureListener() {
//                            override fun onSingleTapUp(e: MotionEvent): Boolean {
//                                inputCallBack.openCellDialog()
//                                return super.onSingleTapConfirmed(e)
//                            }
//                        })
//                setOnTouchListener { _, event ->
//                    detector.onTouchEvent(event)
//                    false
//                }
                isHorizontalScrollBarEnabled = false
                setPadding(padding, 0, padding, 0)
                when (type) {
                    ColumnType.TEXT,
                    ColumnType.NUMBER,
                    ColumnType.PHONE,
                    ColumnType.DATE -> {
                        textCell = textView(text()) {
                            addClickEffectRippleBackground()
                            textSize = 16f
                            gravity = Gravity.CENTER_VERTICAL
                            maxLines = 1
                            hint = context.getString(R.string.notData)
                            onClick {
                                inputCallBack.openCellDialog()
                            }
                        }.lparams(wrapContent, matchParent)
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
                                        backgroundResource =
                                            R.drawable.background_for_color_in_input_layout
                                        oldCircleSelected = this
                                    }
                                    if (color == currentColor) {
                                        select()
                                    }
                                    onClick {
                                        select()
                                        animateRipple(startRadius = dip(17).toFloat())
                                        cell.sourceValue = color.toString()
                                        inputCallBack.notifyCellChanged()
                                    }
                                }.lparams(dip(50), dip(50))
                            }
                        }.lparams(wrapContent, matchParent)
                    }
                    ColumnType.IMAGE -> {
                    }
                    else -> {
                    }
                }
            }.lparams(0, matchParent, 1f)

//            if (type == ColumnType.PHONE)
//                imageView(R.drawable.ic_phone_white) {
//                    val valuePhone = ValuePhone.fromJson(cell.sourceValue)
//                    colorFilter = if (valuePhone.phone.isBlank()) {
//                        PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY)
//                    } else {
//                        onClick {
//                            context.toast("call to.. ${cell.displayValue}")
//                        }
//                        PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY)
//                    }
//                }.lparams(width, matchParent) {
//                    marginEnd = dip(20)
//                    gravity = Gravity.END
//                }

            imageView(R.drawable.ic_check) {
                addClickEffectRippleBackground()
                onClick {
                    inputCallBack.close()
                }
            }.lparams((width * 1.2).toInt(), matchParent) {
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
                            val typeValue = ValuePhone.fromJson(cell.sourceValue)
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