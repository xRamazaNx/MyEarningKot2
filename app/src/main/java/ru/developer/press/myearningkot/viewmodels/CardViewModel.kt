@file:Suppress("UNCHECKED_CAST")

package ru.developer.press.myearningkot.viewmodels

import android.widget.LinearLayout
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import ru.developer.press.myearningkot.App.Companion.dao
import ru.developer.press.myearningkot.ProvideDataRows
import ru.developer.press.myearningkot.activity.CardInfo
import ru.developer.press.myearningkot.activity.UIControl
import ru.developer.press.myearningkot.adapters.AdapterRow.Companion.animatedDuration
import ru.developer.press.myearningkot.adapters.DiffRows
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.scoups.*
import ru.developer.press.myearningkot.model.*

class CardViewModel : ViewModel(),
    ProvideDataRows {

    private lateinit var cardInfo: CardInfo
    lateinit var card: Card
    lateinit var diffRowsUpdater: DiffRows

    private var cellSelectPosition: Int = -1
    private var rowSelectPosition: Int = -1

    val displayParam = DisplayParam()

    val updatedCardStatus = liveData(false)
    var selectMode = liveData(SelectMode.NONE)
    val titleLiveData: MyLiveData<String> = liveData()
    val cardLiveData: MyLiveData<Card> = liveData()
    val totalLiveData: MyLiveData<Card> = liveData()

    var columnLDList = mutableListOf<MyLiveData<Column>>()

    override val sortedRows: MutableList<Row> = mutableListOf()

    override fun getColumns(): MutableList<Column> = card.columns
    override fun getWidth(): Int {
        return if (cardLiveData.value!!.enableHorizontalScroll)
            LinearLayout.LayoutParams.MATCH_PARENT
        else
            displayParam.width
    }

    private fun updateCardLD() {
        cardLiveData.postValue(card)
        titleLiveData.postValue(card.name)

        columnLDList.clear()

        card.columns.forEach {
            columnLDList.add(liveData<Column>().apply {
                postValue(it)
            })
        }
    }

    fun updatePlateChanged() {
        cardLiveData.postValue(card)
    }

    fun updateTotals() {
        totalLiveData.postValue(card)
    }

    private fun updateTypeControl() {
        card.updateTypeControl()
    }

    override fun isEnableHorizontalScroll(): Boolean {
        return cardLiveData.value!!.enableHorizontalScroll
    }

    override fun isEnableSomeStroke(): Boolean {
        return card.enableSomeStroke
    }

    override fun getRowHeight(): Int = card.heightCells
    override fun getSelectCellPairIndexes(): Pair<Int, Int>? {
        var pair: Pair<Int, Int>?
        sortedRows.forEachIndexed { indexRow, row ->
            row.cellList.forEachIndexed { indexCell, cell ->
                if (cell.isSelect) {
                    pair = Pair(indexRow, indexCell)
                    return pair
                }
            }
        }
        return null
    }

    suspend fun updateCardFromDao(cardId: String? = null) {
        cardId?.let { card = dao.getCard(it) }
        sortList()
        updateCardLD()
    }

    suspend fun updateCardInDao() {
        if (cardInfo.cardCategory == CardInfo.CardCategory.CARD) {
            dao.updateCard(card)
        } else
            dao.updateSample(card)
    }

    fun addColumn(columnType: ColumnType, name: String) {
        if (cardInfo.cardCategory == CardInfo.CardCategory.CARD)
            card.addColumn(columnType, name)
        else
            card.addColumnSample(columnType, name)
        updateCardLD()
    }

    fun selectionColumn(columnIndex: Int, isSelect: Boolean) {
        sortList().forEach { row ->
            row.cellList.forEachIndexed { cellIndex, cell ->
                if (columnIndex == cellIndex)
                    cell.isPrefColumnSelect = isSelect
            }
        }
    }

    fun deleteColumn(column: Column): Boolean {
        val deleteResult = card.deleteColumn(column)
        if (deleteResult) {
            updateCardLD()
            updateTypeControl()
        }

        return deleteResult
    }

    // prefFun
    fun moveToRightTotal(selectedTotals: List<Total>, result: (Boolean) -> Unit) {
        val totals = card.totals
        selectedTotals.forEach {
            val index = totals.indexOf(it)
            if (index > totals.size - 2) {
                result(false)
                return
            }
        }

        val indexSortedList = mutableListOf<Int>()
        selectedTotals.forEach {
            indexSortedList.add(totals.indexOf(it))
        }
        indexSortedList.sortDescending()

        indexSortedList.forEach {
            val indexOfTotal = it
            val total = totals[it]

            val totalRight = totals[indexOfTotal + 1]
            totals[indexOfTotal + 1] = total
            totals[indexOfTotal] = totalRight
        }

        updatePlateChanged()
        result(true)

    }

    fun moveToRightColumn(selectedColumns: MutableList<Column>, result: (Boolean) -> Unit) {
        val columns = card.columns
        selectedColumns.forEach {

            val index = columns.indexOf(it)
            if (index == 0 || index > columns.size - 2) {
                result(false)
                return
            }
        }
        val indexSortedList = mutableListOf<Int>()

        selectedColumns.forEach {
            indexSortedList.add(columns.indexOf(it))
        }
        indexSortedList.sortDescending()

        indexSortedList.forEach {
            val indexOfColumn = it
            val column = columns[it]

            val columnRight = columns[indexOfColumn + 1]
            columns[indexOfColumn + 1] = column
            columns[indexOfColumn] = columnRight

            sortList().forEach { row ->
                val cell = row.cellList[indexOfColumn]
                val cellRight = row.cellList[indexOfColumn + 1]

                row.cellList[indexOfColumn + 1] = cell
                row.cellList[indexOfColumn] = cellRight
            }
        }

        updateTypeControl()
        updateCardLD()
        result(true)
    }

    // prefFun
    fun moveToLeftTotal(selectedTotals: List<Total>, result: (Boolean) -> Unit) {
        val totals = card.totals

        selectedTotals.forEach {
            val index = totals.indexOf(it)
            if (index < 1) {
                result(false)
                return
            }
        }
        val indexSortedList = mutableListOf<Int>()

        selectedTotals.forEach {
            indexSortedList.add(totals.indexOf(it))
        }
        indexSortedList.sort()

        indexSortedList.forEach {
            val indexOfTotal = it

            val total = totals[it]
            val totalLeft = totals[indexOfTotal - 1]
            totals[indexOfTotal - 1] = total
            totals[indexOfTotal] = totalLeft
        }

        updatePlateChanged()
        result(true)

    }

    fun moveToLeftColumn(selectedColumns: MutableList<Column>, result: (Boolean) -> Unit) {
        val columns = card.columns

        selectedColumns.forEach {
            val index = columns.indexOf(it)
            if (index < 2) {
                result(false)
                return
            }
        }
        val indexSortedList = mutableListOf<Int>()

        selectedColumns.forEach {
            indexSortedList.add(columns.indexOf(it))
        }
        indexSortedList.sort()

        indexSortedList.forEach {
            val indexOfColumn = it

            val column = columns[it]
            val columnLeft = columns[indexOfColumn - 1]
            columns[indexOfColumn - 1] = column
            columns[indexOfColumn] = columnLeft

            sortList().forEach { row ->
                val cell = row.cellList[indexOfColumn]
                val cellLeft = row.cellList[indexOfColumn - 1]

                row.cellList[indexOfColumn - 1] = cell
                row.cellList[indexOfColumn] = cellLeft
            }
        }

        updateTypeControl()
        updateCardLD()
        result(true)

    }

    fun updateTypeControlColumn(column: Column) {
        card.updateTypeControlColumn(column)
    }

    // тут не создается а обновляется
    fun updateColumnDL() {
        columnLDList.forEachIndexed { index, mutableLiveData ->
            mutableLiveData.value = card.columns[index]
        }
    }

    fun addTotal() {
        card.addTotal()
    }

    fun deleteTotal(it: Total): Boolean {
        return card.deleteTotal(card.totals.indexOf(it))

    }

    fun addRow(end: () -> Unit) {
        runOnViewModel {
            val addRow = card.addRow()
            dao.addRow(addRow)
            sortList()
            main { end.invoke() }
            delay(animatedDuration)
            addRow.status = Status.NONE
            addRow.elementView.animation = null
            card.calcTotals()
            main { end.invoke() }
        }
    }

    private fun sortList(): MutableList<Row> {
        //#postedit
        sortedRows.clear()
        sortedRows.addAll(card.rows)

        return this.sortedRows
    }

    fun cellClicked(
        rowPosition: Int,
        cellPosition: Int,
        function: (Boolean) -> Unit
    ) {

        this.rowSelectPosition = rowPosition
        this.cellSelectPosition = cellPosition

        val cell = sortList()[rowPosition].cellList[cellPosition]
        val isDoubleTap = cell.isSelect
        cell.isSelect = true

        // присваиваем cell только если не было выделено
        selectMode.value?.let {
            if (it == SelectMode.ROW) {
                card.unSelectRows()
            }
        }
        selectMode.value =
            SelectMode.CELL
        function(isDoubleTap)
    }

    fun rowClicked(rowPosition: Int = card.rows.size - 1) {
        runOnViewModel {
            rowSelectPosition = rowPosition

            val row = this.sortedRows[rowPosition]
            val oldStatus = row.status
            row.status = if (oldStatus == Status.SELECT) Status.NONE else Status.SELECT

            // присваиваем cell только если не было выделено
            selectMode.value?.let {
                if (it != SelectMode.ROW) {
                    if (it == SelectMode.CELL) {
                        card.unSelectCell()
                    }
                }
                main {
                    selectMode.value =
                        if (card.getSelectedRows().isEmpty())
                            SelectMode.NONE
                        else
                            SelectMode.ROW
                }
            }
            updateAdapter()
        }
    }

    fun unSelect() {
        card.unSelectCell()
        card.unSelectRows()
        selectMode.value =
            SelectMode.NONE
    }

    private fun updateTypeControlColumn(columnPosition: Int) {
        updateTypeControlColumn(card.columns[columnPosition])
    }

    suspend fun getCopySelectedCell(isCut: Boolean): Cell? {
        card.rows.forEach { row ->
            row.cellList.forEachIndexed { columnIndex, cell ->
                if (cell.isSelect) {
                    val copy = cell.copy()
                    if (isCut) {
                        cell.clear()
                        card.updateTypeControlRow(row)
                        updateTotals()
                        updateAdapter()
                    }
                    return copy
                }
            }
        }
        return null
    }

    fun isEqualTypeCellAndCopyCell(copyCell: Cell?): Boolean {
        val selectedCellType = getSelectedCellType()
        var eq = false
        copyCell?.let {
            eq = it.type == selectedCellType
        }
        return eq
    }

    fun pasteCell(copyCell: Cell?, update: () -> Unit) {
        runOnViewModel {
            if (isEqualTypeCellAndCopyCell(copyCell))
                card.rows.forEach { row ->
                    row.cellList.forEachIndexed { columnPosition, cell ->
                        if (cell.isSelect) {
                            copyCell?.let {
                                cell.sourceValue = it.sourceValue
                                updateRowToDB(row)
                                card.updateTypeControlRow(row)
                                updateTotals()
                            }
                            updateAdapter()
                            main {
                                update()
                            }
                            return@runOnViewModel
                        }
                    }
                }
        }
    }

    suspend fun updateRowToDB(row: Row) {
        io {
            updatedCardStatus.postValue(true)
            dao.updateRow(row)
            updatedCardStatus.postValue(false)
        }
    }

    private fun getSelectedCellType(): ColumnType? {
        return card.getSelectedCell()?.type
    }

    suspend fun deleteRows(updateView: (position: Int) -> Unit) {
        io {
            val deletedRows = sortedRows.filter { it.status == Status.SELECT }
            deletedRows.forEach {
                it.status = Status.DELETED
                main {
                    updateView(
                        sortedRows.indexOf(it)
                    )
                }
            }
            delay(animatedDuration)
            card.deleteRows(deletedRows)
            dao.deleteRows(deletedRows)
            sortList()
            updateAdapter()
            selectMode.postValue(SelectMode.NONE)
        }
        // сортировка листа и обновлении происходит после анимации удаления
    }

    fun getSelectedRows(): List<Row> = card.getSelectedRows()

    fun pasteRows(copyRowList: List<Row>?) {
        runOnViewModel {
            // самый нижний элемент чтобы вставить туда
            val indexLastRow = sortedRows.indexOfLast { it.status == Status.SELECT }
            copyRowList?.let { copyList ->

                if (isCapabilityPaste(copyRowList)) {
                    // выделенные строки ниже которых надо добавить
                    card.getSelectedRows().forEach { it.status = Status.NONE }
                    // отдельный лист чтоб копировать элементы а не ссылки на них потому что в копилист бывают ссылки
                    val list = copyList.fold(mutableListOf<Row>()) { mutableList, row ->
                        mutableList.apply {
                            add(row.copy().also {
                                it.status = Status.ADDED
                            })
                        }
                    }
                    card.rows.addAll(indexLastRow + 1, list)
                    list.forEach {
                        dao.addRow(it)
                    }
                    sortList()

                    list.forEach { row ->
                        card.updateTypeControlRow(row)
                    }

                    updateTotals()
                }

            }
        }
    }

    fun isCapabilityPaste(copyRowList: List<Row>?): Boolean {
        var capability = false
        copyRowList?.let { copyList ->
            val copyFirstRow = copyList[0]
            val currentFirstRow = card.getSelectedRows()[0]

            val copyColumnSize = copyFirstRow.cellList.size
            val currentColumnSize = currentFirstRow.cellList.size

            // равно ли колличесвто колон в копированном и настоящем положении у строк
            if (copyColumnSize == currentColumnSize) {
                capability = true
                // проходим по первым строкам у копированного и настоящего выделенного
                currentFirstRow.cellList.forEachIndexed { index, cell ->
                    // ячейка из скопированной строки
                    val copyCell = copyFirstRow.cellList[index]
                    // равняется ли тип скопированного с настоящим
                    if (cell.type != copyCell.type) {
                        // если хоть один не совпадает то атас
                        capability = false
                        return@forEachIndexed
                    }
                }
            }
        }

        return capability
    }

    fun duplicateRows() {
        val selectedRows = getSelectedRows()
        val rows = card.rows
        rows.forEach {
            it.status = Status.NONE
        }
        sortList()
        sortedRows.last().status = Status.SELECT
        pasteRows(selectedRows)
    }

    // внутри обновление в main
    suspend fun updateAdapter() {
        diffRowsUpdater.checkDiffAndUpdate(sortedRows)
    }

    lateinit var uiControl: UIControl

    fun initialization(cardInfo: CardInfo) {
        this.cardInfo = cardInfo
        runOnViewModel {
            card = if (cardInfo.cardCategory == CardInfo.CardCategory.CARD) {
                dao.getCard(cardInfo.idCard)
            } else {
                dao.getSampleCard(cardInfo.idCard)
            }

            sortList()
            main {

                titleLiveData.value = card.name
                totalLiveData.value = card
                cardLiveData.value = card

                updateColumnDL()
                updateCardLD()

                uiControl.updateActivity()
            }
        }
    }

    enum class SelectMode {
        CELL, ROW, NONE
    }
}
//
//class ViewModelMainFactory(private val pageList: MutableList<Page>) :
//    ViewModelProvider.NewInstanceFactory() {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        return MainViewModel(pageList) as T
//    }
//}

//class ViewModelCardFactory(private val context: Context, private val card: Card) :
//    ViewModelProvider.NewInstanceFactory() {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        return CardViewModel(card) as T
//    }
//}
//
//
//
//
