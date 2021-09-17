package ru.developer.press.myearningkot.helpers.prefLayouts

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.android.synthetic.main.pref_column_date.view.*
import kotlinx.android.synthetic.main.prefs_text_view.view.*
import kotlinx.android.synthetic.main.prefs_total.view.*
import kotlinx.android.synthetic.main.toolbar_pref.view.*
import kotlinx.android.synthetic.main.width_seek_bar_layout.view.*
import org.jetbrains.anko.*
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.activity.CommonCardActivity
import ru.developer.press.myearningkot.databinding.*
import ru.developer.press.myearningkot.dialogs.colorDialog
import ru.developer.press.myearningkot.helpers.colorRes
import ru.developer.press.myearningkot.helpers.getDate
import ru.developer.press.myearningkot.helpers.getDateTypeList
import ru.developer.press.myearningkot.helpers.showItemChangeDialog
import ru.developer.press.myearningkot.model.*

fun Context.getPrefTextLayout(
    prefForTextView: MutableList<PrefForTextView>,
    isWorkAlignPanel: Boolean,
    callback: PrefTextChangedCallback?
): View {
    val noNameBinding = PrefsNoNameBinding.inflate(layoutInflater)
    val toolbarPrefBinding = ToolbarPrefBinding.bind(noNameBinding.root)

    fun init() {
        textPrefButtonsInit(noNameBinding.root, prefForTextView, isWorkAlignPanel) {
            callback?.prefChanged()
        }
    }
    init()
    toolbarPrefBinding.defaultPref.setOnClickListener {
        prefForTextView.forEach {
            it.resetPref()
        }
        callback?.prefChanged()
        init()
    }


    return noNameBinding.root
}

fun Context.getPrefTotalLayout(
    totals: MutableList<Total>,
    callback: PrefTotalChangedCallBack
): View {

    val totalBinding = PrefsTotalBinding.inflate(layoutInflater)
    val widthBinding = WidthSeekBarLayoutBinding.bind(totalBinding.root)
    val toolBarBinding = ToolbarPrefBinding.bind(totalBinding.root)
    val firstTotal = totals[0]

    val widthColumnSeekBar = widthBinding.widthColumnSeekBar

    widthColumnSeekBar.progress = firstTotal.width
    widthColumnSeekBar.setOnSeekBarChangeListener(object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
            val progress = p0!!.progress
            if (progress > 30) {
                totals.forEach {
                    it.width = progress
                }
                callback.widthProgress()
            }
        }

        override fun onStartTrackingTouch(p0: SeekBar?) {
        }

        override fun onStopTrackingTouch(p0: SeekBar?) {
            callback.widthChanged()
        }
    })


    val prefList = mutableListOf<PrefForTextView>().apply {
        totals.forEach {
            add(it.totalPref.prefForTextView)
        }
    }
    val typePref = firstTotal.totalPref

    val digitDown = totalBinding.digitsCountDown
    val digitUp = totalBinding.digitsCountUp
    val digitsSizeTextView = totalBinding.digitsSize
    val grouping = totalBinding.groupNumberSwitch
    val ignoreSwitchWork = totalBinding.ignoreSwitchColumnWorkSwitch

    digitsSizeTextView.text = typePref.digitsCount.toString()
    grouping.isChecked = typePref.isGrouping
    grouping.setOnCheckedChangeListener { _, b ->
        totals.forEach {
            it.totalPref.isGrouping = b
        }
        callback.prefChanged()
    }

    ignoreSwitchWork.isChecked = firstTotal.isIgnoreSwitchWork
    ignoreSwitchWork.setOnCheckedChangeListener { _, b ->
        totals.forEach {
            it.isIgnoreSwitchWork = b
        }
        callback.prefChanged()
    }


    val editDigit = fun(digitOffset: Int) {
        var digit = typePref.digitsCount
        digit += digitOffset

        if (digit < 0)
            digit = 0

        totals.forEach {
            it.totalPref.digitsCount = digit
        }
        callback.prefChanged()
        digitsSizeTextView.text = digit.toString()
    }
    digitDown.setOnClickListener {
        editDigit(-1)
    }
    digitUp.setOnClickListener {
        editDigit(1)
    }
    fun init() {
        textPrefButtonsInit(totalBinding.root, prefList, false) {
            callback.prefChanged()
        }
    }
    init()
    toolBarBinding.defaultPref.setOnClickListener {
        totals.forEach {
            it.totalPref.resetPref()
        }
        callback.prefChanged()
        init()
    }

    totalBinding.formulaTotal.setOnClickListener {
        val allTotals: List<Total> = callback.getTotals()
        val filterTotalList = mutableListOf<Total>().apply {
            addAll(allTotals)
            // удаляем сами выбранные колоны
            removeAll(totals)
            //удаляем те колоны которые указывают в формуле на выбранные колоны
            // цикл по всем нумберколонам
            mutableListOf<Total>().also { listToFind ->
                listToFind.addAll(this)
            }.forEach {
                // достаем из этой колоны все ид колон задействованных в формуле
                val totalIdList = it.formula.getTotalIdList()
                // смотрим в цикле встречаются ли в них те колоны которые выделены
                totalIdList.forEach { id ->
                    // проверяем у всех выделенных
                    totals.forEach { selectTotal ->
                        // одна из выбранных колон учавствует в формуле в не выделеной колоне(все выделенные и так не попадают в список в формуле)
                        if (selectTotal.idToFormula == id) {
                            // удаляем в алл эту колону
                            this.remove(it)
                        }
                    }
                }

            }
        }
        FormulaLayout.formulaDialogShow(
            firstTotal.formula,
            this,
            callback.getNumberColumns(),
            filterNTotals = filterTotalList,
            allNTotals = allTotals
        ) { formula ->
            totals.forEach {
                it.formula = formula
            }
            callback.calcFormula()
        }
    }


    return totalBinding.root
}

fun Context.getPrefDatePeriod(
    typePref: PrefDate,
    prefChangedCallBack: PrefChangedCallBack
): View {
    val periodBinding = PrefDatePeriodBinding.inflate(layoutInflater)
    textPrefButtonsInit(
        periodBinding.root,
        mutableListOf<PrefForTextView>().apply { add(typePref.prefForTextView) },
        false
    ) {
        prefChangedCallBack.prefChanged()
    }

    val dateTypeTextView = periodBinding.dateTypeTextView

    val typeText = dateTypeTextView.text
    val updateDateType = {
        val date = getDate(typePref.type, enableTime = false)
        val dateTypeString = "$typeText ($date)"
        dateTypeTextView.text = dateTypeString
    }
    updateDateType()
    val showTime = periodBinding.enableTime
    showTime.isChecked = typePref.enableTime

    dateTypeTextView.setOnClickListener { view1 ->
        val context = view1.context
        context.showItemChangeDialog(
            context.getString(R.string.date_type),
            getDateTypeList(),
            typePref.type,
            null
        ) { type ->
            typePref.type = type
            prefChangedCallBack.prefChanged()
            updateDateType()
        }
    }
    showTime.setOnCheckedChangeListener { _, b ->
        typePref.enableTime = b

        prefChangedCallBack.prefChanged()
    }

    return periodBinding.root

}

fun textPrefButtonsInit(
    view: View,
    prefForTextViewList: MutableList<PrefForTextView>,
    isWorkAlignPanel: Boolean = true,
    prefChanged: () -> Unit
) {
    val textViewBinding = PrefsTextViewBinding.bind(view)
    val boldButton = textViewBinding.boldButton
    val italicButton = textViewBinding.italicButton

    val firstPrefForTextView = prefForTextViewList[0]
    val initBold = {
        if (firstPrefForTextView.isBold)
            setPressedBackground(boldButton)
        else
            setDefaultBackground(boldButton)
    }
    initBold()

    val initItalic = {

        if (firstPrefForTextView.isItalic)
            setPressedBackground(italicButton)
        else
            setDefaultBackground(italicButton)

    }
    initItalic()

    val alignLeft = textViewBinding.alignLeftButton
    val alignCenter = textViewBinding.alignCenterButton
    val alignRight = textViewBinding.alignRightButton

    val context = textViewBinding.root.context
    val initAlign = {
        val colorFromRes = context.colorRes(R.color.textColorPrimary)
        alignLeft.image?.setTint(colorFromRes)
        alignCenter.image?.setTint(colorFromRes)
        alignRight.image?.setTint(colorFromRes)

        when (firstPrefForTextView.align) {
            TextView.TEXT_ALIGNMENT_TEXT_START -> {
                setPressedBackground(alignLeft)
                setDefaultBackground(alignCenter)
                setDefaultBackground(alignRight)
            }
            TextView.TEXT_ALIGNMENT_CENTER -> {
                setPressedBackground(alignCenter)
                setDefaultBackground(alignLeft)
                setDefaultBackground(alignRight)
            }
            else -> {
                setPressedBackground(alignRight)
                setDefaultBackground(alignLeft)
                setDefaultBackground(alignCenter)
            }
        }
    }
    if (!isWorkAlignPanel) {
        fun disable(imageButton: ImageButton) {
            setDefaultBackground(imageButton)
            val colorFromRes = context.colorRes(R.color.textColorSecondary)
            imageButton.setColorFilter(colorFromRes)
            imageButton.isClickable = false


        }
        disable(alignLeft)
        disable(alignCenter)
        disable(alignRight)
    } else {

        initAlign()
        alignLeft.setOnClickListener {
            prefForTextViewList.forEach {
                it.align = TextView.TEXT_ALIGNMENT_TEXT_START
            }
            initAlign()
            prefChanged()
        }
        alignCenter.setOnClickListener {
            prefForTextViewList.forEach {

                it.align = TextView.TEXT_ALIGNMENT_CENTER
            }
            initAlign()
            prefChanged()
        }
        alignRight.setOnClickListener {
            prefForTextViewList.forEach {

                it.align = TextView.TEXT_ALIGNMENT_TEXT_END
            }
            initAlign()
            prefChanged()
        }
    }

    val textSize = textViewBinding.textSize
    textSize.text = firstPrefForTextView.textSize.toString()

    val textSizeDown = textViewBinding.textSizeDown
    val textSizeUp = textViewBinding.textSizeUp

    val textColor = textViewBinding.textColor
    val initColorTextView = {
        textColor.compoundDrawables.forEach {
            it?.setTint(firstPrefForTextView.color)
        }
    }
    textColor.post {
        initColorTextView()
    }

    boldButton.setOnClickListener {
        val isBold = firstPrefForTextView.isBold
        prefForTextViewList.forEach {
            it.isBold = !isBold
        }
        initBold()
        prefChanged()
    }
    italicButton.setOnClickListener {
        val isItalic = firstPrefForTextView.isItalic

        prefForTextViewList.forEach {

            it.isItalic = !isItalic
        }
        initItalic()
        prefChanged()
    }

    textSizeDown.setOnClickListener {
        val size = firstPrefForTextView.textSize - 1
        if (size < 6)
            return@setOnClickListener

        textSize.text = size.toString()
        prefForTextViewList.forEach {
            it.textSize = size
        }
        prefChanged()
    }
    textSizeUp.setOnClickListener {
        val size = firstPrefForTextView.textSize + 1

        if (size > 48)
            return@setOnClickListener
        textSize.text = size.toString()
        prefForTextViewList.forEach {
            it.textSize = size
        }
        prefChanged()
    }

    textColor.setOnClickListener {
        val activity = context as CommonCardActivity
        colorDialog(firstPrefForTextView.color) { color ->
            prefForTextViewList.forEach {
                it.color = color
            }
            initColorTextView()
            prefChanged()
        }.show(activity.supportFragmentManager, "colorPicker")
    }
}

private fun setPressedBackground(imageButton: ImageButton) {
    imageButton.setColorFilter(imageButton.context.colorRes(R.color.accent))
    imageButton.scaleX = 1.1F
    imageButton.scaleY = 1.1F
//    imageButton.backgroundResource = R.drawable.shape_selected
}

interface PrefTextChangedCallback : PrefChangedCallBack {
    fun nameEdit(text: String)
}

interface PrefChangedCallBack {
    fun prefChanged()
}

interface PrefTotalChangedCallBack : PrefChangedCallBack {
    fun calcFormula()
    fun getNumberColumns(): MutableList<NumberColumn>
    fun getTotals(): List<Total>
    fun widthProgress()
    fun widthChanged()
}
/*
класс который помогает выделять и убирать вылеление
    класс в котором 2 вещи
        выделенная сущность сущность
        и выделено или нет

   тут свойства выделяемых обьектов

   2 типа сущностей
        колоны
        обычные текстовые
 */

fun setDefaultBackground(imageButton: ImageButton) {
    val outValue = TypedValue()
    imageButton.context.theme.resolveAttribute(
        android.R.attr.selectableItemBackground,
        outValue,
        true
    )

    imageButton.scaleX = 1f
    imageButton.scaleY = 1f
    imageButton.setColorFilter(imageButton.context.colorRes(R.color.textColorPrimary))
//    view.setBackgroundResource(outValue.resourceId)
}
