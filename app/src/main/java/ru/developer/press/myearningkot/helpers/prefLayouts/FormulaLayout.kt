package ru.developer.press.myearningkot.helpers.prefLayouts

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.View.GONE
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.jetbrains.anko.*
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.databinding.FormulaLayoutBinding
import ru.developer.press.myearningkot.databinding.FormulaSymbolsBinding
import ru.developer.press.myearningkot.helpers.Calc
import ru.developer.press.myearningkot.helpers.colorRes
import ru.developer.press.myearningkot.model.Formula
import ru.developer.press.myearningkot.model.Formula.Companion.COLUMN_ID
import ru.developer.press.myearningkot.model.Formula.Companion.OTHER
import ru.developer.press.myearningkot.model.Formula.Companion.TOTAL_ID
import ru.developer.press.myearningkot.model.NumberColumn
import ru.developer.press.myearningkot.model.Total
import splitties.alertdialog.appcompat.alertDialog


const val subtractChar = " − "
const val multiplyChar = " × "

class FormulaLayout(
    val formulaLayoutBinding: FormulaLayoutBinding,
    filterNColumns: List<NumberColumn>,
    private val allNColumns: List<NumberColumn> = filterNColumns,
    filterNTotals: List<Total>? = null,
    private val allNTotals: List<Total>? = filterNTotals,
    _formula: Formula
) {
    private val context = formulaLayoutBinding.root.context
    private val formula = Formula()
    private val columnList = mutableListOf<NumberColumn>()
    private val totalList = mutableListOf<Total>()
    private val displayTextView: TextView = formulaLayoutBinding.formulaTextView

    init {

        formula.copyFrom(_formula)
        columnList.addAll(filterNColumns)
        filterNTotals?.let {
            totalList.addAll(it)
        }

        initClickNumbers()
        initClickOperation(formulaLayoutBinding.root) {
            formula.formulaElements.add(Formula.FormulaElement().apply {
                type = OTHER
                value = " $it "
            })
            displayFormula()
        }
        initClickColumns()
        initClickTotals()

        formulaLayoutBinding.clearElementInFormula.setOnClickListener {
            formula.formulaElements.apply {
                if (isNotEmpty()) {
                    removeAt(size - 1)
                    displayFormula()
                }
            }
        }

        displayFormula()
    }

    private fun initClickTotals() {
        val title = formulaLayoutBinding.totalContainerTitle
        val container = formulaLayoutBinding.totalsContainerInFormula

        if (totalList.isEmpty()) {
            title.visibility = GONE
        }
        val elementList = formula.formulaElements
        totalList.forEach { total ->
            val textView = TextView(context).apply {
                initParamTextView()
                text = total.title
                textColor = context.colorRes(R.color.md_blue_200)
                setOnClickListener {
                    elementList.add(Formula.FormulaElement().apply {
                        type = TOTAL_ID
                        value = total.idToFormula.toString()
                    })

                    displayFormula()
                }
            }
            container.addView(textView)
        }

    }

    private fun initClickColumns() {
        val container = formulaLayoutBinding.columnContainerInFormula
        val elementList = formula.formulaElements
        columnList.forEach { column ->
            val textView = TextView(context).apply {
                initParamTextView()
                text = column.name
                textColor = context.colorRes(R.color.md_green_300)
                setOnClickListener {
                    elementList.add(Formula.FormulaElement().apply {
                        type = COLUMN_ID
                        value = column.idToFormula.toString()
                    })

                    displayFormula()
                }
            }
            container.addView(textView)
        }
    }

    private fun TextView.initParamTextView() {
        padding = context.dip(16)
        textColor = context.colorRes(R.color.textColorPrimary)
        layoutParams = LinearLayout.LayoutParams(wrapContent, matchParent)
    }

    private fun initClickNumbers() {
        val one = formulaLayoutBinding.one
        val two = formulaLayoutBinding.two
        val three = formulaLayoutBinding.three
        val four = formulaLayoutBinding.four
        val five = formulaLayoutBinding.five
        val six = formulaLayoutBinding.six
        val seven = formulaLayoutBinding.seven
        val eight = formulaLayoutBinding.eight
        val nine = formulaLayoutBinding.nine
        val zero = formulaLayoutBinding.zero

        val click: (View) -> Unit = {
            val textView = it as TextView

            formula.formulaElements.add(Formula.FormulaElement().apply {
                type = OTHER
                value = textView.text.toString()
            })

            displayFormula()
        }
        one.setOnClickListener(click)
        two.setOnClickListener(click)
        three.setOnClickListener(click)
        four.setOnClickListener(click)
        five.setOnClickListener(click)
        six.setOnClickListener(click)
        seven.setOnClickListener(click)
        eight.setOnClickListener(click)
        nine.setOnClickListener(click)
        zero.setOnClickListener(click)
    }

    private fun displayFormula() {
        displayTextView.text = formula.getFormulaString(context, allNColumns, allNTotals)
    }


    fun getFormula(): Formula? {

        // заварушка лишняя для проверки подленности формулы
        // если нет то загорится красным
        val stringBuilder = StringBuilder()

        formula.formulaElements.forEach {
            when (it.type) {
                TOTAL_ID,
                COLUMN_ID -> {
                    stringBuilder.append(1.345)
                }
                OTHER -> {
                    stringBuilder.append(it.value)
                }
            }
        }

        return try {
            Calc.evaluate(stringBuilder.toString())
            formula
        } catch (exception: Exception) {
            null
        }
    }

    fun errorFormula() {
        displayTextView.textColor = displayTextView.context.colorRes(R.color.colorRed)
    }

    companion object{
        fun formulaDialogShow(
            formula: Formula,
            context: Context,
            filterNColumns: List<NumberColumn>,
            allNColumns: List<NumberColumn> = filterNColumns,
            filterNTotals: List<Total>?,
            allNTotals: List<Total>?,
            positiveClick: (Formula) -> Unit
        ) {
            val formulaLayoutBinding = FormulaLayoutBinding.inflate(context.layoutInflater)
            val formulaLayout = FormulaLayout(
                formulaLayoutBinding = formulaLayoutBinding,
                filterNColumns = filterNColumns,
                allNColumns = allNColumns,
                filterNTotals = filterNTotals,
                allNTotals = allNTotals,
                _formula = formula
            )

            context.alertDialog {
                setCustomTitle(TextView(context).apply {
                    padding = context.dip(18)
                    textSize = 18F
                    text = "Введите формулу"
                    textColor = context.colorRes(R.color.textColorPrimary)
                })
                setView(formulaLayoutBinding.root)
                setPositiveButton("OK") { dialog, _ ->
                    formulaLayout.getFormula()?.let {
                        positiveClick(it)
                        dialog.dismiss()
                    } ?: formulaLayout.errorFormula()
                }
                setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.dismiss()
                }
            }.apply {
                show()
                getButton(AlertDialog.BUTTON_POSITIVE).textColor =
                    context.colorRes(R.color.accent)
                getButton(AlertDialog.BUTTON_NEGATIVE).textColor =
                    context.colorRes(R.color.accent)
                window?.setBackgroundDrawable(ColorDrawable(context.colorRes(R.color.colorPrimary)))
            }
        }

        fun initClickOperation(view: View, callBack: (String) -> Unit) {
            val symbolsBinding = FormulaSymbolsBinding.bind(view)
            val add = symbolsBinding.add
            val sub = symbolsBinding.subtract
            val mult = symbolsBinding.multiply
            val div = symbolsBinding.divide
            val percent = symbolsBinding.percent
            val leftBracket = symbolsBinding.leftBracket
            val rightBracket = symbolsBinding.rightBracket
            val point = symbolsBinding.point

            val click: (View) -> Unit = {
                val textView = it as TextView
                var op = textView.text.toString()
                if (it == symbolsBinding.subtract)
                    op = "-"
                if (it == symbolsBinding.multiply)
                    op = "*"

                callBack(op)
            }

            add.setOnClickListener(click)
            sub.setOnClickListener(click)
            mult.setOnClickListener(click)
            div.setOnClickListener(click)
            percent.setOnClickListener(click)
            leftBracket.setOnClickListener(click)
            rightBracket.setOnClickListener(click)
            point.setOnClickListener(click)
        }
    }
}