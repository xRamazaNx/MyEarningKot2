package ru.developer.press.myearningkot.helpers.scoups

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.total_item_layout.view.*
import kotlinx.android.synthetic.main.total_item_value.view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.padding
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick
import org.jetbrains.anko.wrapContent
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.activity.CardActivity
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.databinding.CardBinding
import ru.developer.press.myearningkot.helpers.Width
import ru.developer.press.myearningkot.helpers.getPathForResource
import ru.developer.press.myearningkot.helpers.prefLayouts.ElementType
import ru.developer.press.myearningkot.model.*
import java.util.*

@SuppressLint("InflateParams")
fun Card.inflatePlate(plateView: CardBinding) {
    val context = plateView.root.context
    val nameCard = plateView.nameCard
    val datePeriodCard = plateView.datePeriodCard
    nameCard.text = name
    val isCardActivity = context is CardActivity
    datePeriodCard.visibility = if (isShowDatePeriod && !isCardActivity) View.VISIBLE else View.GONE
    datePeriodCard.text = dateOfPeriod

    // визуальная настройка
    cardPref.namePref.customize(nameCard, R.font.roboto_medium)
    cardPref.dateOfPeriodPref.prefForTextView.customize(datePeriodCard, R.font.roboto_medium)

    //главный контейнер для заголовков и значений
    val inflater = LayoutInflater.from(context)
    val totalContainer: LinearLayout =
        inflater.inflate(R.layout.total_item_layout, null) as LinearLayout
    totalContainer.layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent).apply {
        weight = 1f
    }

    val totalContainerDisableScroll = plateView.totalContainerDisableScroll
    val totalContainerScroll = plateView.totalContainerScroll

    //удаляем где бы не были
    totalContainerDisableScroll.removeAllViews()
    totalContainerScroll.removeAllViews()

    // добавляем в главный лейаут для тоталов
    if (enableHorizontalScrollTotal) {
        totalContainerScroll.visibility = View.VISIBLE
        totalContainerDisableScroll.visibility = View.GONE
        totalContainerScroll.addView(totalContainer)
    } else {
        totalContainerScroll.visibility = View.GONE
        totalContainerDisableScroll.visibility = View.VISIBLE
        totalContainerDisableScroll.addView(totalContainer)
    }
    // контейнер для всех значений
    val totalValueLayout = totalContainer.totalValueContainer
    // кнтейнер для всех заголовков
    val totalTitleLayout = totalContainer.totalTitleContainer

    totals.forEachIndexed { index, totalItem ->
        // лайот где валуе и линия
        val valueLayout = inflater.inflate(R.layout.total_item_value, null)

        val layoutParams = LinearLayout.LayoutParams(totalItem.width, matchParent).apply {
            weight = 1f
        }
        valueLayout.layoutParams = layoutParams
        val title = TextView(context).apply {
            this.layoutParams = layoutParams
            gravity = Gravity.CENTER
            padding = dip(3)
        }
        val value = valueLayout.totalValue

        title.text = totalItem.title
        title.maxLines = 1
        title.ellipsize = TextUtils.TruncateAt.END
        totalItem.titlePref.customize(title)

        totalItem.totalPref.prefForTextView.customize(value)
        totalItem.calcFormula(this)
        value.text = totalItem.value

        totalTitleLayout.addView(title)
        totalValueLayout.addView(valueLayout)

        if (index == totals.size - 1) {
            valueLayout._verLine.visibility = View.GONE
        }
    }
}

fun Card.updateTotalAmount(plateView: View) {
    val totalValueLayout = plateView.totalValueContainer
    totals.forEachIndexed { index, totalItem ->
        // лайот где валуе и линия
        val valueLayout = totalValueLayout.getChildAt(index)
        val value = valueLayout.totalValue
        totalItem.calcFormula(this)
        value.text = totalItem.value
    }
}

fun Card.setClickToTotals(
    cardBinding: CardBinding,
    click: (view: View, long: Boolean, elementType: ElementType, position: Int) -> Unit
) {
    val totalContainer = if (enableHorizontalScrollTotal) {
        cardBinding.totalContainerScroll.getChildAt(0)
    } else {
        cardBinding.totalContainerDisableScroll.getChildAt(0)
    }

    val totalTitleContainer = totalContainer.totalTitleContainer
    val totalValueContainer = totalContainer.totalValueContainer

    totals.forEachIndexed { index, _ ->

        val title = totalTitleContainer.getChildAt(index)
        title.onClick {
            click.invoke(title, false, ElementType.TOTAL_TITLE, index)
        }
        title.onLongClick {
            click.invoke(title, true, ElementType.TOTAL_TITLE, index)
        }

        val value = totalValueContainer.getChildAt(index)
        value.onClick {
            click.invoke(value, false, ElementType.TOTAL, index)
        }
        value.onLongClick {
            click.invoke(value, true, ElementType.TOTAL, index)
        }
    }
}

fun Card.addTotal(): Total {
    val totalItem = Total(pageId, refId).apply {
        formula.formulaElements.add(Formula.FormulaElement(Formula.OTHER, "0"))
    }
    totals.add(totalItem)

    return totalItem
}

fun Card.deleteTotal(index: Int): Boolean {
    return if (totals.size > 1) {
        totals.removeAt(index)
        true
    } else
        false
}

private fun getNewCell(column: Column): Cell = Cell().apply {
    val gson = Gson()
    sourceValue = when (column) {
        is ColorColumn -> {
            Color.WHITE.toString()
        }
        is ImageColumn -> {
            Gson().toJson(ImageTypeValue())
        }
        is SwitchColumn -> {
            false.toString()
        }
        is ListColumn ->
            ""
        is PhoneColumn ->
            gson.toJson(
                PhoneTypeValue()
            )

        is DateColumn -> {
            ""
        }
        is NumberColumn -> {
            ""
        }
        else -> {
            ""
        }
    }
    cellTypeControl = column.columnTypeControl
}

fun Card.addRow(
    row: Row = Row(pageId, refId).apply {
        cellList = mutableListOf<Cell>().apply {
            columns.forEach { column ->
                add(getNewCell(column))
            }
        }
    }
): Row {
    row.status = Status.ADDED
    rows.add(row)
    updateTypeControlRow(row)
    calcTotals()
    return row
}

fun Card.addRows(index: Int, rowLis: List<Row>) {
    rowLis.forEach { row ->
        row.status = Status.ADDED
        updateTypeControlRow(row)
    }
    rows.addAll(index, rowLis)
    calcTotals()
}

fun Card.addColumn(
    type: ColumnType,
    name: String,
    width: Int? = null,
    position: Int = columns.size
): Column {
    val column = when (type) {
        ColumnType.NUMERATION -> NumerationColumn(name, pageId, refId)
        ColumnType.NUMBER -> NumberColumn(name, pageId, refId)
        ColumnType.PHONE -> PhoneColumn(name, pageId, refId)
        ColumnType.DATE -> DateColumn(name, pageId, refId)
        ColumnType.COLOR -> ColorColumn(name, pageId, refId)
        ColumnType.SWITCH -> SwitchColumn(name, pageId, refId)
        ColumnType.IMAGE -> ImageColumn(name, pageId, refId)
        ColumnType.LIST -> ListColumn(name, pageId, refId)
        ColumnType.TEXT -> TextColumn(name, pageId, refId)
        // не будет ни когда использоваться
        ColumnType.NONE -> TextColumn(name, pageId, refId)
    }
    if (width != null) {
        column.width = width
    }
    columns.add(position, column)
    column.updateTypeControl(this)
    rows.forEach {
        it.cellList.add(position, getNewCell(column))
    }
    updateTypeControlColumn(column)
    calcTotals()
    return column

}

fun Card.calcTotals() {
    totals.forEach {
        it.calcFormula(this)
    }
}

fun Card.addColumnSample(type: ColumnType, name: String, position: Int = columns.size) {
    val column = when (type) {
        ColumnType.NUMERATION -> NumerationColumn(name, pageId, refId)
        ColumnType.NUMBER -> NumberColumn(name, pageId, refId)
        ColumnType.PHONE -> PhoneColumn(name, pageId, refId)
        ColumnType.DATE -> DateColumn(name, pageId, refId)
        ColumnType.COLOR -> ColorColumn(name, pageId, refId)
        ColumnType.SWITCH -> SwitchColumn(name, pageId, refId)
        ColumnType.IMAGE -> ImageColumn(name, pageId, refId)
        ColumnType.LIST -> ListColumn(name, pageId, refId)
        ColumnType.TEXT -> TextColumn(name, pageId, refId)
        // не будет ни когда использоваться
        ColumnType.NONE -> TextColumn(name, pageId, refId)
    }
    column.pageId = pageId
    column.cardId = refId

    columns.add(position, column)
    column.updateTypeControl(this)
    rows.forEach {
        it.cellList.add(position, getCellOfSample(position))
    }
    updateTypeControlColumn(column)
}

fun Card.deleteColumn(column: Column? = null): Boolean {
    // если колоны пусты то ни чего не делаем
    if (column is NumerationColumn || columns.size == 1)
        return false

    // ищем колону по параметрам и без
    val lastIndex = columns.size - 1
    val col = when {
        // если
        column != null -> column
        else -> columns[lastIndex]
    }
    val index = columns.indexOf(col)
    // удалаяем
    columns.remove(col)
    rows.forEach {
        it.cellList.removeAt(index)
    }
    calcTotals()
//        // удаляем ид колоны из списка суммируемых если он есть в нем
//        sumColumnId.forEach {
//            if (it == col.id) {
//                sumColumnId.remove(it)
//                return true
//            }
//        }
//        // удаляем ид колоны из списка авансируемых если он есть в нем
//        avansColumnId.forEach {
//            if (it == col.id) {
//                avansColumnId.remove(it)
//                return true
//            }
//
//        }
    return true
}

fun Card.findColumnAtId(idColumn: String): Column? {
    columns.forEach {
        if (it.refId == idColumn)
            return it
    }
    return null
}

fun Card.getCellOfSample(position: Int): Cell {
    val column = columns[position]
    return Cell().apply {
        cellTypeControl = column.columnTypeControl
        sourceValue = when (column) {
            is ImageColumn -> {
                Gson().toJson(ImageTypeValue().apply { imagePathList.add(getPathForResource(R.drawable.ic_sample_image).toString()) })
            }
            is SwitchColumn -> {
                val newVal = (0..20).random() > 10
                newVal.toString()
            }
            is ColorColumn -> {
                val r = (0..255).random()
                val g = (0..255).random()
                val b = (0..255).random()
                val rgb: Int = Color.rgb(r, g, b)
                rgb.toString()
            }
            is ListColumn -> {
                "Выбранный элемент"
            }
            is DateColumn -> {
                Date().time.toString()
            }
            is NumberColumn -> {
                "12345.987"
            }
            is PhoneColumn -> {
                Gson().toJson(PhoneTypeValue(phone = "7 999 123-45-67"))
            }
            is NumerationColumn -> {
                "1"
            }
            // если та где можно использовать текст
            else -> {
                "текст который может быть порой очень длинным"
            }
        }
    }
}

fun Card.updateTypeControl() {
    columns.forEach { column ->
        updateTypeControlColumn(column)
    }
//        fillTotalAmount()
}

fun Card.addSampleRow() {
    val row = mutableListOf<Cell>()
    columns.forEachIndexed { index, _ ->
        row.add(getCellOfSample(index))
    }
    rows.add(Row(pageId, refId).apply { cellList = row })
}

fun Card.deleteRow(index: Int = rows.size - 1) {
    if (rows.isEmpty())
        return
    rows.removeAt(index)
    calcTotals()

}

fun Card.deleteRows(removedList: List<Row>) {
    if (removedList.isNotEmpty())
        rows.removeAll(removedList)
    calcTotals()
}

fun Card.updateTypeControlColumn(column: Column) {
    column.updateTypeControl(this)
    val columnPosition = columns.indexOf(column)
    rows.forEach { row ->
        updateTypeControlCell(row, columnPosition)
    }
}

fun Card.updateTypeControlRow(row: Row) {
    repeat(columns.size) { columnPosition ->
        updateTypeControlCell(row, columnPosition)
    }
}

private fun Card.updateTypeControlCell(row: Row, columnPosition: Int) {
    val cell = row.cellList[columnPosition]
    val column = columns[columnPosition]
    cell.cellTypeControl = column.columnTypeControl
    cell.type = column.getType()
    if (column is NumberColumn) {
        if (column.inputType == InputTypeNumberColumn.FORMULA) {
            cell.sourceValue = column.calcFormula(row, this)
        }
    }
    cell.updateTypeValue(column.pref())
}

fun Card.unSelectCell(): Int {
    rows.forEachIndexed { rowIndex, row ->
        val cell = row.cellList.find { it.isSelect }
        if (cell != null) {
            cell.isSelect = false
            return rowIndex
        }
    }
    return -1
}

fun Card.unSelectRows() {
    rows.forEach {
        it.status = Status.NONE
    }
}

fun Card.getSelectedCell(): Cell? {
    rows.forEach { row ->
        row.cellList.forEach { cell ->
            if (cell.isSelect) {
                return cell
            }
        }
    }
    return null
}

fun Card.deleteRow(row: Row) {
    rows.remove(row)
    calcTotals()
}

fun Card.getSelectedRows(): MutableList<Row> {
    val selRows = mutableListOf<Row>()
    rows.forEach {
        if (it.status == Status.SELECT)
            selRows.add(it)
    }
    return selRows
}

