package ru.developer.press.myearningkot.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.forEach
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_card.*
import kotlinx.android.synthetic.main.card.*
import kotlinx.android.synthetic.main.total_item_layout.view.*
import kotlinx.android.synthetic.main.total_item_value.view.*
import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent
import org.jetbrains.anko.backgroundColorResource
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.toast
import org.jetbrains.anko.wrapContent
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.dialogs.DialogBasicPrefCard
import ru.developer.press.myearningkot.dialogs.DialogSetName
import ru.developer.press.myearningkot.dialogs.choiceDialog
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.prefLayouts.*
import ru.developer.press.myearningkot.helpers.prefLayouts.ElementPrefType.*
import ru.developer.press.myearningkot.helpers.scoups.setClickToTotals
import ru.developer.press.myearningkot.model.*
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.positiveButton


const val CARD_ID = "card_id"
const val CARD_CATEGORY = "card_category"
const val TITLE_PREF_ACTIVITY = "title_pref_activity"

fun ActivityResultLauncher<Intent>.startPrefActivity(
    category: CardInfo.CardCategory,
    activity: Context? = null,
    cardId: String,
    title: String,
) {

    val intent = Intent(activity, PrefCardActivity::class.java)

    intent.putExtra(CARD_ID, cardId)
    intent.putExtra(CARD_CATEGORY, category.name)

    intent.putExtra(TITLE_PREF_ACTIVITY, title)

    launch(intent)
}

class PrefCardActivity : CommonCardActivity() {

    private val totalContainer: LinearLayout
        get() {
            return if (totalContainerDisableScroll.childCount > 0)
                totalAmountView.totalContainerDisableScroll.getChildAt(0) as LinearLayout
            else
                totalAmountView.totalContainerScroll.getChildAt(0) as LinearLayout

        }
    private val selectedControl = PrefSelectedControl()
    private lateinit var prefWindow: PopupWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = intent.getStringExtra(TITLE_PREF_ACTIVITY)
        activityBinding.fbAddRow.visibility = GONE
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_check)

        visibleHideElements()
        disableBehavior()

        initPrefPopupWindow()

        KeyboardVisibilityEvent.setEventListener(this) {
            //            val height =
//                if (it) resources.displayMetrics.heightPixels / 5
//                else (resources.displayMetrics.heightPixels / 2.2).toInt()
//            if (prefWindow.isShowing)
//                prefWindow.update(matchParent, height)
        }

    }

    private fun initPrefPopupWindow() {
        prefWindow = PopupWindow(this)
//        prefWindow.height = (resources.displayMetrics.heightPixels / 2)
        prefWindow.height = wrapContent
        prefWindow.width = matchParent
        prefWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        prefWindow.isFocusable = true
        prefWindow.isOutsideTouchable = true
        prefWindow.animationStyle = R.style.popup_window_animation
        prefWindow.setOnDismissListener {
            root.animate().alpha(1f)
            totalAmountView.root.animate().translationY(0f)
            selectedControl.unSelectAll()
        }
    }

    fun showPrefWindow(view: View, y: Int) {
        if (y > 0) {
            view.post {
                totalAmountView.root.animate().translationY(-view.height.toFloat())
            }
        }
        view.minimumHeight = (resources.displayMetrics.heightPixels / 3)
        prefWindow.contentView = view
        prefWindow.showAtLocation(totalAmountView.root, Gravity.BOTTOM, 0, 0)
        root.animate().alpha(0.3f)

    }

    private fun clickPrefToAdapter() {
        adapter.setCellClickPref { columnIndex ->
            selectColumn(columnIndex)
        }
    }

    private fun selectColumn(columnIndex: Int) {
        selectedControl.select(
            SelectedElement.ElementColumn(columnIndex, ElementType.COLUMN, null).apply {
                columnType = viewModel.card.columns[columnIndex].getType()
            })
    }

    private fun initClicksOfElements() {
        viewModelInitializer.invokeOnCompletion {

            clickPrefToAdapter()

            val card = viewModel.card
            card.columns.forEachIndexed { index, _ ->
                columnContainer.getChildAt(index).setOnClickListener {
                    selectedControl.select(
                        SelectedElement.ElementColumnTitle(
                            index,
                            ElementType.COLUMN_TITLE,
                            it.background
                        )
                    )
                }
            }

            totalAmountView.nameCard.setOnClickListener {
                selectedControl.select(
                    SelectedElement.ElementTextView(
                        it.background,
                        ElementType.NAME
                    )
                )

            }

            totalAmountView.datePeriodCard.setOnClickListener {
                selectedControl.select(
                    SelectedElement.ElementTextView(
                        it.background,
                        ElementType.DATE
                    )
                )
            }

            setClickTotals()
        }
    }

    private fun setClickTotals() {
        viewModel.card.setClickToTotals(totalAmountView) { view, _, type, index ->
            if (type == ElementType.TOTAL)
                selectTotal(index)
            if (type == ElementType.TOTAL_TITLE)
                selectedControl.select(
                    SelectedElement.ElementTotal(
                        index,
                        view.background,
                        ElementType.TOTAL_TITLE
                    )
                )
        }
    }

    private fun visibleHideElements() {

        totalAmountView.apply {
            datePeriodCard.visibility = VISIBLE
            nameCard.visibility = VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.card_pref_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.basicPref -> {
                viewModel.card.let { card ->
                    // ps покрывает обновление всех четырех настроек которые настраиваются в диалоге
                    val dialogBasicPrefCard = DialogBasicPrefCard(card) {
                        prefChanged = {
                            runOnLifeCycle {
                                if (it == DialogBasicPrefCard.TypeOfChange.rows) {
                                    adapter = getAdapterForRecycler()
                                    main {
                                        horizontalScrollSwitch()
                                        activityBinding.recycler.adapter = adapter
                                    }

                                } else {
                                    main {
//                                        setShowTotalInfo(card.isShowTotalInfo)//
                                        updatePlate()
                                    }
                                }
                                main {
                                    initClicksOfElements()
//                                    if (card.isShowTotalInfo)
                                    selectedControl.updateSelected()
//                                    else selectedControl.unSelectAll()
                                }
                            }
                        }
                    }
                    dialogBasicPrefCard.show(supportFragmentManager, "dialogBasicPrefCard")
                }
            }
            R.id.addColumn -> {
                val list = getColumnTypeList()
                showItemChangeDialog(getString(R.string.change_type_data), list, -1, null) {
                    val columnType = getColumnTypeEnumList()[it]

                    viewModel.addColumn(columnType, list[it])
                    horizontalScrollSwitch()
                    initRecyclerView()
                    // после создания адаптера надо зановго заложить умение выделяться
                    initClicksOfElements()

                    horizontalScrollView.post {
                        horizontalScrollView.fullScroll(ScrollView.FOCUS_RIGHT)
                    }
                    selectedControl.unSelectAll()
                    val index = viewModel.card.columns.size - 1
                    selectColumn(index)
                }
            }
            R.id.addTotal -> {
                viewModel.addTotal()
                updatePlate(false)
                initClicksOfElements()
                selectedControl.unSelectAll()
                selectTotal(viewModel.card.totals.size - 1)
            }
            R.id.elementSetting -> {
                selectedControl.showPref()
            }
            R.id.moveToRight -> {
                selectedControl.moveToRight()
            }
            R.id.moveToLeft -> {
                selectedControl.moveToLeft()
            }
            R.id.deleteColumn -> {
                selectedControl.delete()
            }
            R.id.renameElement -> {
                selectedControl.rename()
            }
            android.R.id.home -> {
                setCardOfResult()
            }
        }
        return true
    }

    override fun updateActivity() {
        super.updateActivity()
        initSelectCallback()
        initClicksOfElements()
        totalAmountView.root.setOnClickListener(null)
    }

    private fun disableBehavior() {
        val params = activityBinding.toolbar.layoutParams as AppBarLayout.LayoutParams
        params.scrollFlags = 0
    }

    override fun onBackPressed() {
        if (!selectedControl.unSelectAll()) choiceDialog {
            setTitle(R.string.save_changes)
            setMessage("")
            positiveButton(R.string.save) {
                setCardOfResult()
            }
            negativeButton(R.string.cancel) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }.apply {
            negativeButtonColorRes = R.color.colorRed
        }.show(supportFragmentManager, "pref_confirm")
    }

    fun getColumnsFromSelectedElements(selectedElementList: List<SelectedElement>): MutableList<Column> {
        return mutableListOf<Column>().apply {
            selectedElementList.filterIsInstance(SelectedElement.ElementColumn::class.java)
                .forEach {
                    val columnIndex = it.columnIndex
                    val column = viewModel.card.columns[columnIndex]
                    add(column)
                }
        }
    }

    private fun setCardOfResult() {
        runMainOnLifeCycle {
            activityBinding.progressBar.visibility = VISIBLE
            viewModel.updateCardInDao()
            val data = Intent()
            data.putExtra(CARD_ID, viewModel.card.refId)
            setResult(Activity.RESULT_OK, data)
            finish()
        }
    }

    private fun initSelectCallback() {
        selectedControl.selectCallback = object : SelectCallback {
            val card = viewModel.card
            override fun select(selectedElement: SelectedElement) {
                when (selectedElement.elementType) {
                    ElementType.COLUMN -> {
                        val selectedColumn = selectedElement as SelectedElement.ElementColumn
                        val columnIndex = selectedColumn.columnIndex
//                            val column = card.columns[columnIndex]
                        // назначение ячейкам что они выбраны
                        viewModel.selectionColumn(columnIndex, true)
                        // обновление
                        // для обновления ширины задать а потом убрать
//                            adapter.updateWidthIndex = columnIndex
//                            adapter.widthSelected = column.width
                        notifyItems()
                    }
                    ElementType.COLUMN_TITLE -> {
                        val selectedColumnTitle =
                            selectedElement as SelectedElement.ElementColumnTitle
                        val columnIndex = selectedColumnTitle.columnIndex

                        // выделили заголовок
                        val columnView = columnContainer.getChildAt(columnIndex) as TextView
                        setSelectBackground(columnView)

                    }
                    ElementType.NAME -> {
                        setSelectBackground(nameCard)
                    }
                    ElementType.DATE -> {
                        setSelectBackground(datePeriodCard)

                    }
                    ElementType.TOTAL -> {
                        val selectTotalItem = selectedElement as SelectedElement.ElementTotal
                        val index = selectTotalItem.index
                        val totalView =
                            totalContainer.totalValueContainer.getChildAt(index).totalValue
                        setSelectBackground(totalView)
                    }
                    ElementType.TOTAL_TITLE -> {
                        val selectTotalItem = selectedElement as SelectedElement.ElementTotal
                        val index = selectTotalItem.index
                        val totalView = totalContainer.totalTitleContainer.getChildAt(index)
                        setSelectBackground(totalView)
                    }
                }
            }

            private fun changedCard() {
                updatePlate()
            }

            override fun unSelect(selectedElement: SelectedElement?) {

                when (selectedElement?.elementType) {
                    ElementType.COLUMN -> {
                        val elementColumn = selectedElement as SelectedElement.ElementColumn
                        viewModel.selectionColumn(elementColumn.columnIndex, false)
                        // обновление
                        notifyItems()
                        // удаление вью для настройки
                    }
                    ElementType.COLUMN_TITLE -> {
                        val elementColumn =
                            selectedElement as SelectedElement.ElementColumnTitle
                        val columnIndex = elementColumn.columnIndex
                        columnContainer.getChildAt(columnIndex).background =
                            elementColumn.oldDrawable

                    }
                    ElementType.NAME -> {
                        totalAmountView.nameCard.background = selectedElement.oldDrawable
                    }
                    ElementType.DATE -> {
                        totalAmountView.datePeriodCard.background = selectedElement.oldDrawable
                    }
                    ElementType.TOTAL -> {
                        val selectTotalItem = selectedElement as SelectedElement.ElementTotal
                        val index = selectTotalItem.index
                        val totalView =
                            totalContainer.totalValueContainer.getChildAt(index).totalValue
                        totalView.background = selectedElement.oldDrawable
                    }
                    ElementType.TOTAL_TITLE -> {
                        val selectTotalItem = selectedElement as SelectedElement.ElementTotal
                        val index = selectTotalItem.index
                        val totalView = totalContainer.totalTitleContainer.getChildAt(index)
                        totalView.background = selectedElement.oldDrawable
                    }
                }

            }

            override fun showPref(elementPref: ElementPref) {
                var yOff = 0
                val prefLayout = when (elementPref.elementPrefType) {
                    TEXT_VIEW -> {
                        var isWorkAlignPanel = true

                        // проверка для настройки выравнивания
                        elementPref.selectedElementList.forEach {
                            val elementType = it.elementType
                            if (elementType == ElementType.NAME || elementType == ElementType.DATE || elementType == ElementType.TOTAL_TITLE) isWorkAlignPanel =
                                false
                        }
                        if (!isWorkAlignPanel) {
                            yOff = totalAmountView.root.height
                        }
                        val prefList = mutableListOf<PrefForTextView>()

                        // сбор настроек
                        elementPref.selectedElementList.forEach {
                            val prefForTextView = when (it.elementType) {
                                ElementType.COLUMN_TITLE -> {
                                    val elementColumn = it as SelectedElement.ElementColumnTitle
                                    val column = card.columns[elementColumn.columnIndex]
                                    column.titlePref
                                }
                                ElementType.NAME -> card.cardPref.namePref

                                ElementType.TOTAL_TITLE -> {
                                    val totalItem =
                                        card.totals[(it as SelectedElement.ElementTotal).index]
                                    totalItem.titlePref
                                }
                                else -> null
                            }
                            if (prefForTextView != null) {
                                prefList.add(prefForTextView)
                            }
                        }
                        getPrefTextLayout(
                            prefList,
                            isWorkAlignPanel,
                            object : PrefTextChangedCallback {
                                override fun nameEdit(text: String) {
                                    elementPref.selectedElementList.forEach {
                                        when (it.elementType) {

                                            ElementType.COLUMN_TITLE -> {
                                                val elementColumn =
                                                    it as SelectedElement.ElementColumnTitle
                                                val column =
                                                    card.columns[elementColumn.columnIndex]
                                                column.name = text
                                            }
                                            ElementType.NAME -> {
                                                card.name = text
                                            }
                                            ElementType.TOTAL_TITLE -> {
                                                val elementTotal =
                                                    it as SelectedElement.ElementTotal
                                                card.totals[elementTotal.index].title = text
                                            }
                                            else -> {

                                            }
                                        }
                                    }
                                    prefChanged()
                                }

                                override fun prefChanged() {
                                    changedCard()
                                    viewModel.updateColumnDL()
                                }
                            })
                    }

                    TOTAL -> {
                        yOff = totalAmountView.root.height

                        val listTotal = mutableListOf<Total>().apply {
                            elementPref.selectedElementList.filterIsInstance(SelectedElement.ElementTotal::class.java)
                                .forEach {
                                    add(card.totals[it.index])
                                }
                        }
                        getPrefTotalLayout(listTotal, object : PrefTotalChangedCallBack {
                            override fun prefChanged() {
                                updatePlate()
                            }

                            override fun calcFormula() {
                                card.totals.forEach {
                                    it.calcFormula(card)
                                }
                                prefChanged()
                            }

                            override fun getNumberColumns(): MutableList<NumberColumn> {
                                return card.columns.filterIsInstance<NumberColumn>()
                                    .toMutableList()
                            }

                            override fun getTotals(): List<Total> = card.totals
                            override fun widthProgress() {
                                listTotal.forEach { total ->

                                    val index = card.totals.indexOf(total)
                                    totalContainer.totalValueContainer.getChildAt(index).layoutParams.width =
                                        total.width
                                    totalContainer.totalTitleContainer.getChildAt(index).layoutParams.width =
                                        total.width
                                }
                                totalContainer.requestLayout()
                            }

                            override fun widthChanged() {
                                updatePlate()
                            }
                        })
                    }

                    DATE_PERIOD -> {
                        yOff = totalAmountView.root.height
                        getPrefDatePeriod(
                            card.cardPref.dateOfPeriodPref,
                            object : PrefChangedCallBack {
                                override fun prefChanged() {
                                    changedCard()
                                }
                            })
                    }

                    else -> {
                        val columnList =
                            getColumnsFromSelectedElements(elementPref.selectedElementList)
                        getPrefColumnLayout(
                            columnList,
                            elementPref.columnType,
                            object : PrefColumnChangedCallback {
                                override fun widthChanged() {
                                    viewModel.updateColumnDL()
                                    initRecyclerView()
                                    initClicksOfElements()

                                    widthDrawer.positionList.clear()
                                    widthDrawer.invalidate()
                                    // фуакция на изменение ширины колоны
                                }

                                override fun prefChanged() {
                                    runMainOnLifeCycle {
                                        io {
                                            columnList.forEach {
                                                viewModel.updateTypeControlColumn(
                                                    it
                                                )
                                            }
                                        }
                                        notifyItems()
                                        updatePlate()
                                    }
                                }

                                override fun widthProgress() {
                                    columnList.forEach { column ->

                                        val index = card.columns.indexOf(column)
                                        columnContainer.getChildAt(index).layoutParams.width =
                                            column.width
                                    }
                                    columnContainer.requestLayout()

                                    widthDrawer.positionList.clear()
                                    columnContainer.forEach {
                                        val title = it
                                        val rightPosition =
                                            title.x + it.width - horizontalScrollView.scrollX
                                        widthDrawer.positionList.add(
                                            rightPosition
                                        )
                                    }

                                    widthDrawer.invalidate()
                                }

                                override fun recreateView() {
                                    runMainOnLifeCycle {
                                        io {
                                            adapter = getAdapterForRecycler()
                                        }
                                        horizontalScrollSwitch()
                                        activityBinding.recycler.adapter = adapter
                                        clickPrefToAdapter()
                                    }
                                }

                                override fun getNumberColumns(): MutableList<NumberColumn> {
                                    val filterIsInstance =
                                        card.columns.filterIsInstance<NumberColumn>()
                                    return filterIsInstance.toMutableList()

                                }
                            })
                    }
                }
                //
                showPrefWindow(prefLayout.apply {
                    backgroundColorResource = R.color.colorBackground
                }, yOff)
            }

            fun reSelectAfterMove(list: List<Any>) {
                val isColumn = selectedControl.selectPrefType == COLUMN
                selectedControl.unSelectAll()
                if (isColumn) list.filterIsInstance<Column>().forEach { column ->
                    val columnIndex = card.columns.indexOf(column)
                    selectColumn(columnIndex)
                }
                else list.filterIsInstance<Total>().forEach { total ->
                    val columnIndex = card.totals.indexOf(total)
                    selectTotal(columnIndex)
                }

            }

            override fun setVisiblePrefButton(isVisible: Boolean) {
                activityBinding.toolbar.menu.findItem(R.id.elementSetting).isVisible = isVisible
                visibleButton(selectedControl.selectPrefType, selectedControl.isRenameMode)

            }

            override fun moveToRight(selectedElementList: List<SelectedElement>) {

                if (selectedControl.selectPrefType == COLUMN) {
                    val columnList = getColumnsFromSelectedElements(selectedElementList)
                    viewModel.moveToRightColumn(columnList) {
                        if (!it) toast(getString(R.string.moving_not_available))
                        else {
                            viewModel.updateColumnDL()
                            initRecyclerView()
                            initClicksOfElements()
                            reSelectAfterMove(columnList)
                        }
                    }
                } else {
                    val totalList: List<Total> =
                        getTotalsFromSelectedElements(selectedElementList)
                    viewModel.moveToRightTotal(totalList) {
                        if (!it) toast(getString(R.string.moving_not_available))
                        else {
                            initClicksOfElements()
                            reSelectAfterMove(totalList)
                        }
                    }
                }
            }

            fun getTotalsFromSelectedElements(selectedElementList: List<SelectedElement>): List<Total> {
                return mutableListOf<Total>().apply {
                    selectedElementList.filterIsInstance(SelectedElement.ElementTotal::class.java)
                        .forEach {
                            val totalIndex = it.index
                            val total = card.totals[totalIndex]
                            add(total)
                        }
                }
            }

            override fun moveToLeft(selectedElementList: List<SelectedElement>) {
                if (selectedControl.selectPrefType == COLUMN) {

                    val columnList = getColumnsFromSelectedElements(selectedElementList)
                    viewModel.moveToLeftColumn(columnList) {
                        if (!it) toast(getString(R.string.moving_not_available))
                        else {
                            viewModel.updateColumnDL()
                            initRecyclerView()
                            initClicksOfElements()
                            reSelectAfterMove(columnList)
                        }
                    }
                } else {
                    val totalList = getTotalsFromSelectedElements(selectedElementList)
                    viewModel.moveToLeftTotal(totalList) {
                        if (!it) toast(getString(R.string.moving_not_available))
                        else {
                            initClicksOfElements()
                            reSelectAfterMove(totalList)
                        }
                    }
                }
            }

            override fun delete(selectedElementList: List<SelectedElement>) {
                if (selectedControl.selectPrefType == COLUMN) {

                    val columnList = getColumnsFromSelectedElements(selectedElementList)
                    selectedControl.unSelectAll()
                    columnList.forEach {
                        val deleteColumnResult: Boolean = viewModel.deleteColumn(it)
                        if (!deleteColumnResult) {
                            toast(getString(R.string.cannot_delete_numbering_column))
                        }
                    }
                    viewModel.updateColumnDL()
                    initRecyclerView()
                    initClicksOfElements()
                } else {
                    val totalList = getTotalsFromSelectedElements(selectedElementList)
                    selectedControl.unSelectAll()
                    totalList.forEach {
                        val deleteTotal = viewModel.deleteTotal(it)
                        deleteTotal.let { delTotal ->
                            if (delTotal) {
                                updatePlate(false)
                            } else {
                                //#edit надо тут показать окошко с этим сообщением
                                toast("Нельзя удалить единственный итог, если хотите отключить итоговую панель, отключите ее в настройках карточки")
                            }
                        }
                    }
                    initClicksOfElements()
                }
            }

            override fun rename(selectedElementList: MutableList<SelectedElement>) {
                val element = selectedElementList[0]
                val text = when (element.elementType) {
                    ElementType.COLUMN_TITLE -> {
                        val columnTitleElement = element as SelectedElement.ElementColumnTitle
                        val column = card.columns[columnTitleElement.columnIndex]
                        column.name
                    }
                    ElementType.TOTAL_TITLE -> {
                        val totalElement = element as SelectedElement.ElementTotal
                        card.totals[totalElement.index].title

                    }
                    else -> {
                        card.name
                    }
                }

                DialogSetName().setName(text).setTitle(getString(R.string.name_element))
                    .setPositiveListener {
                        when (element.elementType) {
                            ElementType.COLUMN_TITLE -> {
                                val columnTitleElement =
                                    element as SelectedElement.ElementColumnTitle
                                val column = card.columns[columnTitleElement.columnIndex]
                                column.name = it
                                val columnTitle =
                                    columnContainer.getChildAt(columnTitleElement.columnIndex) as TextView
                                columnTitle.text = it
                            }
                            ElementType.TOTAL_TITLE -> {
                                val totalElement = element as SelectedElement.ElementTotal
                                card.totals[totalElement.index].title = it
                                updatePlate()
                            }
                            else -> {
                                card.name = it
                                updatePlate()
                            }
                        }
                        true
                    }.show(supportFragmentManager, "setNameElement")
            }
        }
    }

    private fun selectTotal(totalIndex: Int) {
        selectedControl.select(
            SelectedElement.ElementTotal(
                totalIndex,
                totalContainer.totalValueContainer.getChildAt(
                    totalIndex
                ).background,
                ElementType.TOTAL
            )
        )
    }

    override fun onResume() {
        super.onResume()
        activityBinding.toolbar.post {
            val isVisible = selectedControl.isSelect
            activityBinding.toolbar.menu.findItem(R.id.elementSetting).isVisible = isVisible
            visibleButton(selectedControl.selectPrefType, selectedControl.isRenameMode)
        }
    }

    private fun visibleButton(prefType: ElementPrefType, isRenameMode: Boolean) {
        val isColumnOrTotal = prefType == TOTAL || prefType == COLUMN

        setVisibleMenuItem(R.id.moveToRight, isColumnOrTotal)
        setVisibleMenuItem(R.id.moveToLeft, isColumnOrTotal)
        setVisibleMenuItem(R.id.deleteColumn, isColumnOrTotal)
        setVisibleMenuItem(R.id.renameElement, isRenameMode)
    }

    private fun setVisibleMenuItem(id: Int, visible: Boolean) {
        activityBinding.toolbar.menu.findItem(id)?.isVisible = visible
    }

    private fun updatePlate(reselect: Boolean = true) {
        viewModel.updatePlate()
        setClickTotals()
        if (reselect)
            selectedControl.updateSelected()
    }
}
