package ru.developer.press.myearningkot.helpers

import androidx.fragment.app.DialogFragment
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import org.jetbrains.anko.toast
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.activity.CardActivity
import ru.developer.press.myearningkot.dialogs.DialogEditCell
import ru.developer.press.myearningkot.dialogs.DialogEditImageCell
import ru.developer.press.myearningkot.dialogs.editCellTag
import ru.developer.press.myearningkot.helpers.scoups.addDismissListener
import ru.developer.press.myearningkot.helpers.scoups.addShownListener
import ru.developer.press.myearningkot.model.Column
import ru.developer.press.myearningkot.model.ColumnType
import ru.developer.press.myearningkot.model.InputTypeNumberColumn
import ru.developer.press.myearningkot.model.NumberColumn

class EditCellControl private constructor(
    private val cardActivity: CardActivity,
    private val column: Column,
    sourceValue: String,
    private val changed: (sourceValue: String) -> Unit
) {
    companion object {
        fun showEditCellDialog(
            cardActivity: CardActivity,
            column: Column,
            sourceValue: String,
            changed: (sourceValue: String) -> Unit
        ): EditCellControl {
            return EditCellControl(cardActivity, column, sourceValue, changed)
                .editCell()
        }
    }

    private var value = ""
    private var dialog: DialogFragment? = null

    init {
        value = sourceValue
    }

    private fun editCell(): EditCellControl {
        dialog = when (column.getType()) {
            ColumnType.SWITCH -> {
                val toBoolean = !value.toBoolean()
                value = toBoolean.toString()
                changed(value)
                null
            }
            ColumnType.COLOR -> {
                dialog = ColorPickerDialog
                    .newBuilder()
                    .setColor(value.toInt())
                    .setShowAlphaSlider(false)
                    .create().apply {
                        setColorPickerDialogListener(
                            object : ColorPickerDialogListener {
                                override fun onDialogDismissed(dialogId: Int) {

                                }

                                override fun onColorSelected(dialogId: Int, color: Int) {
                                    value = color.toString()
                                    changed(value)
                                }
                            })
                    }
                dialog?.show(cardActivity.supportFragmentManager, "colorPicker")
                dialog
            }

            ColumnType.IMAGE -> {
                dialog = DialogEditImageCell(column, value) {
                    changed(it)
                }
                dialog?.show(
                    cardActivity.supportFragmentManager,
                    editCellTag
                )
                dialog
            }
            else -> {
                if (column is NumberColumn && column.inputType == InputTypeNumberColumn.FORMULA) {
                    cardActivity.toast(cardActivity.getString(R.string.formula_works_for_this_column))
                    null
                } else {
                    dialog = DialogEditCell(column, value) {
                        changed(it)
                    }
                    dialog?.show(
                        cardActivity.supportFragmentManager,
                        editCellTag
                    )
                    dialog
                }
            }
        }
        return this
    }

    private fun dismissListener(dismiss: () -> Unit): EditCellControl {
        dialog?.addDismissListener {
            dismiss.invoke()
        }
        return this
    }

    private fun showListener(shown: () -> Unit): EditCellControl {
        dialog?.addShownListener {
            shown.invoke()
        }
        return this
    }
}