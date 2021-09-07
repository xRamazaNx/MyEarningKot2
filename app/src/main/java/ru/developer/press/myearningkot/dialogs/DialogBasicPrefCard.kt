package ru.developer.press.myearningkot.dialogs

import android.app.Dialog
import android.content.DialogInterface
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
import ru.developer.press.myearningkot.helpers.*

class DialogBasicPrefCard(
    private val card: Card,
    private val basicPrefEvent: () -> Unit
) : DialogFragment() {

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
            textViewValutaType.text =
                "${getString(R.string.valuta)} (${listValutaType[card.valuta]})"
            textViewValutaType.setOnClickListener {
                context.showItemChangeDialog(
                    "Выберите валюту",
                    listValutaType,
                    card.valuta,
                    null,
                    fun(selected) {
                        card.valuta = selected
                        textViewValutaType.text =
                            "${getString(R.string.valuta)} (${listValutaType[selected]})"
                        updateCard()
                    })
            }


            val switchEnableHorizontalScroll = layout.switchEnableHorizontalScrollItems
            val switchEnableSomeStroke = layout.switchEnableSomeStroke

            switchEnableHorizontalScroll.isChecked = card.enableHorizontalScroll
            switchEnableSomeStroke.isChecked = card.enableSomeStroke

            switchEnableHorizontalScroll.setOnCheckedChangeListener { _, b ->
                card.enableHorizontalScroll = b
                updateCard()
            }
            switchEnableSomeStroke.setOnCheckedChangeListener { _, b ->
                card.enableSomeStroke = b
                updateCard()
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
                updateCard()
            }
            heightDown.setOnClickListener {
                card.heightCells -= 1
                updateHeightInfo()
                updateCard()
            }

            val switchEnableShowDatePeriod = layout.switchEnableDatePeriod
            val switchEnableHorizontalScrollTotals = layout.switchEnableHorizontalScrollTotals
//            val switchShowTotalInfo = view.switchShowTotalInfo

            switchEnableShowDatePeriod.isChecked = card.isShowDatePeriod
            switchEnableHorizontalScrollTotals.isChecked = card.enableHorizontalScrollTotal
//            switchShowTotalInfo.isChecked = card.isShowTotalInfo

            switchEnableShowDatePeriod.setOnCheckedChangeListener { _, isChecked ->
                card.isShowDatePeriod = isChecked
                updateCard()
            }
            switchEnableHorizontalScrollTotals.setOnCheckedChangeListener { _, b ->
                card.enableHorizontalScrollTotal = b
                updateCard()
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

    private fun updateCard() {
        runOnLifeCycle {
            delay(250)
            basicPrefEvent()
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
}
//    override fun onStart() {
//        val window = dialog?.window
//        window?.apply {
//            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
//        }
//        super.onStart()
//    }

