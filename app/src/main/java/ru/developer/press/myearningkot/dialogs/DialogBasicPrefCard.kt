package ru.developer.press.myearningkot.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.card_basic_pref_layout.view.*
import kotlinx.coroutines.delay
import org.jetbrains.anko.layoutInflater
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.helpers.getValutaTypeList
import ru.developer.press.myearningkot.helpers.runOnLifeCycle
import ru.developer.press.myearningkot.helpers.showItemChangeDialog

class DialogBasicPrefCard(
    private val card: Card,
    init: DialogBasicPrefCard.() -> Unit
) : DialogFragment() {
    lateinit var prefChanged: (TypeOfChange) -> Unit

    init {
        init.invoke(this)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = AlertDialog.Builder(requireContext()).apply {
//            setCustomTitle(TextView(context).apply {
//                text = context.getString(R.string.general_setting)
//                textColorResource = R.color.light_gray
//                val dp24 = context.dpsToPixels(24)
//                val dp8 = context.dpsToPixels(8)
//                setPadding(dp24, dp24, dp24, dp8)
//                textSize = 20f
//            })
            val layout = context.layoutInflater.inflate(R.layout.card_basic_pref_layout, null)

            // выбор валюты
            val textViewValutaType = layout.textViewValutaType
            val listValutaType = getValutaTypeList(context)
            val valuta = "${getString(R.string.valuta)} (${listValutaType[card.valuta]})"
            textViewValutaType.text = valuta
            textViewValutaType.setOnClickListener {
                context.showItemChangeDialog(
                    "Выберите валюту",
                    listValutaType,
                    card.valuta,
                    null,
                    fun(selected) {
                        card.valuta = selected
                        val newValuta =
                            "${getString(R.string.valuta)} (${listValutaType[selected]})"
                        textViewValutaType.text = newValuta
                        updateCard(TypeOfChange.rows)
                    })
            }

            val switchEnableHorizontalScroll = layout.switchEnableHorizontalScrollItems
            val switchEnableSomeStroke = layout.switchEnableSomeStroke

            switchEnableHorizontalScroll.isChecked = card.enableHorizontalScroll
            switchEnableSomeStroke.isChecked = card.enableSomeStroke

            switchEnableHorizontalScroll.setOnCheckedChangeListener { _, b ->
                card.enableHorizontalScroll = b
                updateCard(TypeOfChange.rows)
            }
            switchEnableSomeStroke.setOnCheckedChangeListener { _, b ->
                card.enableSomeStroke = b
                updateCard(TypeOfChange.rows)
            }
            val heightUp = layout.heightSizeUp
            val heightDown = layout.heightSizeDown
            val heightSize = layout.heightSize

            fun updateHeightInfo() {
                heightSize.text = card.heightCells.toString()
            }
            updateHeightInfo()

            heightUp.setOnClickListener {
                card.heightCells += 1
                updateHeightInfo()
                updateCard(TypeOfChange.rows)
            }
            heightDown.setOnClickListener {
                card.heightCells -= 1
                updateHeightInfo()
                updateCard(TypeOfChange.rows)
            }

            val switchEnableShowDatePeriod = layout.switchEnableDatePeriod
            val switchEnableHorizontalScrollTotals = layout.switchEnableHorizontalScrollTotals
//            val switchShowTotalInfo = view.switchShowTotalInfo

            switchEnableShowDatePeriod.isChecked = card.isShowDatePeriod
            switchEnableHorizontalScrollTotals.isChecked = card.enableHorizontalScrollTotal
//            switchShowTotalInfo.isChecked = card.isShowTotalInfo

            switchEnableShowDatePeriod.setOnCheckedChangeListener { _, isChecked ->
                card.isShowDatePeriod = isChecked
                updateCard(TypeOfChange.plate)
            }
            switchEnableHorizontalScrollTotals.setOnCheckedChangeListener { _, b ->
                card.enableHorizontalScrollTotal = b
                updateCard(TypeOfChange.plate)
            }
//            switchShowTotalInfo.setOnCheckedChangeListener { _, isChecked ->
//                card.isShowTotalInfo = isChecked
//                updateCard()
//            }
            setView(layout)
//            setPositiveButton(" ") { dialogInterface: DialogInterface, _: Int ->
//                dialogInterface.dismiss()
//
//            }
        }

        val alertDialog = dialog.create()
//        alertDialog.setAlertButtonColors(R.color.colorAccent, R.color.colorAccent)
        return alertDialog
    }

    private fun updateCard(typeOfChange: TypeOfChange) {
        runOnLifeCycle {
            delay(250)
            prefChanged(typeOfChange)
        }
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            dialog?.window?.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        it,
                        R.color.colorDialogBackground
                    )
                )
            )
        }
    }

    enum class TypeOfChange {
        rows,
        plate
    }
}
//    override fun onStart() {
//        val window = dialog?.window
//        window?.apply {
//            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
//        }
//        super.onStart()
//    }

