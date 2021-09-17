package ru.developer.press.myearningkot.activity

import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView.ItemAnimator.ItemAnimatorFinishedListener
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_card.*
import kotlinx.android.synthetic.main.activity_card.view.*
import kotlinx.android.synthetic.main.card.*
import kotlinx.android.synthetic.main.card.view.*
import kotlinx.coroutines.*
import ru.developer.press.myearningkot.*
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.prefLayouts.InputLayout
import ru.developer.press.myearningkot.helpers.scoups.updateTypeControlRow
import ru.developer.press.myearningkot.model.*
import ru.developer.press.myearningkot.viewmodels.CardViewModel.SelectMode
import java.lang.Runnable

open class CardActivity : CommonCardActivity() {

    private val editCardRegister =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null) {
                data.getStringExtra(CARD_ID)?.let { id ->
                    viewModel.apply {
                        runOnViewModel {
                            updateCardFromDao(id)
                            main {
                                updateActivity()
                                setSelectMode(SelectMode.NONE)
                                onResume()
                            }
                        }
                    }
                }
            }
        }
    private var isLongClick = false

    private val rowClickListener = object : RowClickListener {
        override var isOpenEditDialogProcess: Boolean = false
        override fun cellClick(cellInfo: CellInfo) {
            if (isLongClick) {
                viewModel.rowClicked(cellInfo.rowPosition)
                viewModel.sortedRows[cellInfo.rowPosition].elementView.animateRipple()
            } else {
                if (viewModel.selectMode() == SelectMode.ROW) {
                    isLongClick = true
                    cellClick(cellInfo)
                    return
                }
                viewModel.cellClicked(cellInfo) { isDoubleTap ->
                    if (isDoubleTap) {
                        if (!isOpenEditDialogProcess) {
                            if (cellInfo.cell.type != ColumnType.SWITCH) {
                                isOpenEditDialogProcess = true
                                postDelay(500) {
                                    isOpenEditDialogProcess = false
                                }
                            }
                            editCell()
                        }
                    } else {
                        cellInfo.cell.elementView.animateRipple()
                        showInputCell()
                    }
                }
            }
        }
    }

    // должна только за меню отвечать
    private fun selectedModeObserve() {
        var oldSelectMode: SelectMode? = null
        var pasteIconFromCell: Int = -1
        var pasteIconFromRow: Int = -1
        val menu = toolbar.menu

        viewModel.selectMode.observe(this, observer { selectMode ->
            if (oldSelectMode != selectMode)
                menu.clear()
            when (selectMode) {
                SelectMode.CELL -> {
                    if (oldSelectMode != SelectMode.CELL) {
                        menuInflater.inflate(R.menu.cell_menu, menu)
                        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_check)
                    }
                    // ставим иконку вставить в зависимости доступности вставки
                    val icon = if (viewModel.isCapabilityPasteCell(app().copyCell)) {
                        R.drawable.ic_paste
                    } else
                        R.drawable.ic_paste_disabled

                    if (pasteIconFromCell != icon) {
                        menu.findItem(R.id.pasteCell).setIcon(icon)
                        pasteIconFromCell = icon
                    }
                }

                SelectMode.ROW -> {
                    if (oldSelectMode != SelectMode.ROW) {
                        menuInflater.inflate(R.menu.row_menu, menu)
                        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_check)
                    }

                    val icon = if (viewModel.isCapabilityPasteRow(app().copyRowList)) {
                        R.drawable.ic_paste
                    } else
                        R.drawable.ic_paste_disabled

                    if (pasteIconFromRow != icon) {
                        menu.findItem(R.id.pasteRow).setIcon(icon)
                        pasteIconFromRow = icon
                    }
                }
                else -> {
                    if (oldSelectMode != SelectMode.NONE) {
                        menuInflater.inflate(R.menu.card_main_menu, menu)
                        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_home)
                        if (activityBinding.fbAddRow.isShown) {
                            if (!appBar.isShown)
                                activityBinding.fbAddRow.hide()
                        }
                    }
                }
            }
            if (selectMode != SelectMode.NONE)
                activityBinding.fbAddRow.hide()
            if (selectMode != SelectMode.CELL)
                hideInputCell()

            oldSelectMode = selectMode
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.card_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        postDelay(250) {
            when (item.itemId) {
                R.id.search -> {
                }
                R.id.period -> {
                }
                R.id.sort -> {
                }
                R.id.setting -> {
                    editCardRegister.startPrefActivity(
                        CardInfo.CardCategory.CARD,
                        activity = this,
                        cardId = viewModel.card.refId,
                        title = getString(R.string.setting)
                    )
                }
                // cell
                R.id.editCell -> {
                    editCell()
                }
                R.id.pasteCell -> {
                    pasteCell()
                }
                R.id.copyCell -> {
                    copySelectedCell(false)
                }
                R.id.cutCell -> {
                    copySelectedCell(true)
                }
                // row
                R.id.deleteRow -> {
                    removeSelectedRows()
                }
                R.id.cutRow -> {
                    copySelectedRows(true)
                }
                R.id.copyRow -> {
                    copySelectedRows(false)
                }
                R.id.pasteRow -> {
                    pasteRows()
                }
                R.id.duplicateRow -> {
                    duplicateRows()
                }

                android.R.id.home -> {
                    onBackPressed()
                }
            }
        }
        return true
    }

    override fun updateActivity() {
        super.updateActivity()
        totalAmountView.root.setOnClickListener(null)
        hideUnnecessaryElementsFromTotalAmount()
        setAppBarOffsetChangedListener()
        adapter.setCellClickListener(rowClickListener)
        activityBinding.fbAddRow.setButtonIconResource(R.drawable.ic_add_not_ring_white)
        activityBinding.fbAddRow.setButtonBackgroundColour(colorRes(R.color.colorSecondaryDark))

        activityBinding.tableView.isLong.observe(this@CardActivity, {
            isLongClick = it
        })

        activityBinding.fbAddRow.setOnClickListener {
            viewModel.apply {
                addRow {
                    scrollToPosition(sortedRows.size)
                }
            }
        }
        // наблюдатель для события выделения ячейки
        selectedModeObserve()

    }

    private suspend fun duplicateRows() = io {
        viewModel.apply {
            copySelectedRows(false)
            app().copyRowList?.let { copyRows ->
                duplicateRows(copyRows)
                main {
                    scrollToPosition(sortedRows.size)
                }

            }
        }
    }

    private suspend fun pasteRows() {
        app().copyRowList?.let { list ->
            viewModel.pasteRows(list)
        }
    }

    private suspend fun copySelectedRows(isCut: Boolean) = io {
        app().copyRowList = viewModel.getSelectedRows()
        if (isCut)
            removeSelectedRows()
        else
            viewModel.setSelectMode(SelectMode.ROW)
    }

    private suspend fun copySelectedCell(isCut: Boolean) {
        io {
            viewModel.apply {
                app().copyCell = getCopySelectedCell(isCut)
                // заного назначаю чтоб меню создалось заного и иконка вставки если надо станет серой или белой
                setSelectMode(SelectMode.CELL)
            }
        }
    }

    private suspend fun pasteCell() {
        // на вход принимается функция которая должна обновить строку после вставки
        app().copyCell?.let { cell ->
            viewModel.pasteCell(cell) {
                updateInputLayout()
            }
        }
    }

    private suspend fun removeSelectedRows() = io {
        viewModel.apply {
            deleteRows { position ->
                adapter.notifyItemChanged(position)
            }
        }
    }

    private fun hideUnnecessaryElementsFromTotalAmount() {
        totalAmountView.apply {
            datePeriodCard.visibility = GONE
            nameCard.visibility = GONE
        }
    }

    private fun setAppBarOffsetChangedListener() {
        activityBinding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->

            if (viewModel.selectMode() != SelectMode.NONE)
                return@OnOffsetChangedListener
            val heightToolbar = appBarLayout.toolbar.height

            val isHide = -verticalOffset == heightToolbar
            val isShow = verticalOffset == 0

            if (isShow) {
                activityBinding.fbAddRow.show()
            }
            if (isHide) {
                activityBinding.fbAddRow.hide()
            }
        })
    }

    override fun onBackPressed() {
        viewModel.apply {
            if (selectMode() != SelectMode.NONE) {
                unSelect()
            } else {
                updatedCardStatus.observe(this@CardActivity) {
                    if (!it)
                        finish()
                }
            }
        }
    }

    private fun editCell() {
        viewModel.apply {
            selectCellInfo()?.let {
                val columnPosition = it.columnPosition
                val rowPosition = it.rowPosition

                val row = sortedRows[rowPosition]
                val column = card.columns[columnPosition]
                val selectCell = sortedRows[rowPosition].cellList[columnPosition]

                EditCellControl.edit(
                    cardActivity = this@CardActivity,
                    column = column,
                    sourceValue = selectCell.sourceValue
                ) { newValue ->
                    runOnViewModel {
                        selectCell.sourceValue = newValue

                        card.updateTypeControlRow(row)

                        updateRowToDB(row)
                        main {
                            updateTotals()
                        }
                        updateAdapter()
                        main {
                            updateInputLayout()
                        }
                    }
                }
            }
        }
    }

    private fun scrollToPosition(position: Int) {
        recycler.scrollToPosition(position) //  у нас на одну больше из за отступа для плейт
        appBar.setExpanded(false, true)
    }

    private fun showInputCell() {
        if (updateInputLayout()) {
            activityBinding.inputCellLayout.post {
                activityBinding.inputCellLayout
                    .animate()
                    .translationY(-inputCellLayout.height.toFloat())
                    .setDuration(250)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
        } else hideInputCell()
    }

    private fun updateInputLayout(): Boolean {
        viewModel.apply {
            selectCellInfo()?.let { cellInfo ->
                if (cellInfo.cell.type == ColumnType.SWITCH)
                    return false
                val layout = InputLayout.inflateInputLayout(
                    this@CardActivity,
                    cellInfo.cell,
                    card,
                    object : InputLayout.InputCallBack {
                        override fun openCellDialog() {
                            editCell()
                        }

                        override fun notifyCellChanged() {
                            runOnLifeCycle {
                                card.updateTypeControlRow(sortedRows[cellInfo.rowPosition])
                                updateAdapter()
                            }
                        }

                        override fun close() {
                            onBackPressed()
                        }

                    })
                activityBinding.inputCellLayout.removeAllViews()
                activityBinding.inputCellLayout.addView(layout)
            }
        }
        return true
    }

    private fun hideInputCell() {
        inputCellLayout.post {
            inputCellLayout
                .animate()
                .translationY(inputCellLayout.height.toFloat())
                .setDuration(250)
                .setInterpolator(AccelerateInterpolator())
                .start()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModelInitializer.invokeOnCompletion {
            viewModel.selectMode.postUpdate()
            title = viewModel.card.name
        }
    }

    private val waitForAnimationsToFinishRunnable =
        Runnable { waitForAnimationsToFinish() }

    // When the data in the recycler view is changed all views are animated. If the
// recycler view is animating, this method sets up a listener that is called when the
// current animation finishes. The listener will call this method again once the
// animation is done.
    private fun waitForAnimationsToFinish() {
        if (activityBinding.recycler.isAnimating) { // The recycler view is still animating, try again when the animation has finished.
            activityBinding.recycler.itemAnimator?.isRunning(animationFinishedListener)
            return
        }
        // The recycler view have animated all it's views
        notifyItems()
    }

    // Listener that is called whenever the recycler view have finished animating one view.
    private val animationFinishedListener =
        ItemAnimatorFinishedListener {
            // The current animation have finished and there is currently no animation running,
            // but there might still be more items that will be animated after this method returns.
            // Post a message to the message queue for checking if there are any more
            // animations running.
            Handler(Looper.getMainLooper()).post(waitForAnimationsToFinishRunnable)
        }
}