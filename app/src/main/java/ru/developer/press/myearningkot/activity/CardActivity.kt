package ru.developer.press.myearningkot.activity

import android.animation.Animator
import android.graphics.Color
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
import ru.developer.press.myearningkot.helpers.scoups.updateTypeControlCell
import ru.developer.press.myearningkot.model.*
import ru.developer.press.myearningkot.viewmodels.CardViewModel.SelectMode
import java.lang.Runnable

open class CardActivity : CommonCardActivity() {

    private val editCardRegister =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null) {
                data.getStringExtra(CARD_ID)?.let { id ->
                    viewModel.runOnViewModel {
                        viewModel.updateCardFromDao(id)
                        main {
                            updateActivity()
                            viewModel.selectMode.value = SelectMode.NONE
                            onResume()
                        }
                    }
                }
            }
        }
    private var isLongClick = false

    private fun selectedModeObserve() {
        val menu = toolbar.menu
        viewModel.selectMode.observe(this, { selectMode ->
            menu.clear()
            when (selectMode) {
                SelectMode.CELL -> {
                    menuInflater.inflate(R.menu.cell_menu, menu)
                    // ставим иконку вставить в зависимости доступности вставки
                    if (viewModel.isEqualTypeCellAndCopyCell(app().copyCell)) {
                        menu.findItem(R.id.pasteCell).setIcon(R.drawable.ic_paste)
                    } else
                        menu.findItem(R.id.pasteCell).setIcon(R.drawable.ic_paste_disabled)
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_check)
                }

                SelectMode.ROW -> {
                    menuInflater.inflate(R.menu.row_menu, menu)
                    if (viewModel.isCapabilityPaste(app().copyRowList)) {
                        menu.findItem(R.id.pasteRow).setIcon(R.drawable.ic_paste)
                    } else
                        menu.findItem(R.id.pasteRow).setIcon(R.drawable.ic_paste_disabled)
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_check)
                }
                else -> {
                    menuInflater.inflate(R.menu.card_main_menu, menu)
                    fbAddRow.speedDialMenuAdapter = null
                    fbAddRow.setButtonIconResource(R.drawable.ic_add_not_ring_white)
                    fbAddRow.setButtonBackgroundColour(getColorFromRes(R.color.colorSecondaryDark))
                    // тут именно это пусть будет
                    Handler(Looper.getMainLooper()).post { waitForAnimationsToFinish() }
                    if (fbAddRow.isShown) {
                        if (!appBar.isShown)
                            fbAddRow.hide()
                    }
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_home)
                }
            }
            if (selectMode != SelectMode.NONE)
                fbAddRow.hide()
            if (selectMode != SelectMode.CELL)
                hideInputCell()
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.card_main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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
        return super.onOptionsItemSelected(item)
    }

    override fun updateActivity() {
        doStart()
        hideViewWhileScroll()
        hideUnnecessaryElementsFromTotalAmount()
        fbAddRow.setContentCoverColour(Color.TRANSPARENT)
        progressBar.visibility = GONE

        tableView.isLong.observe(this@CardActivity, {
            isLongClick = it
        })

        viewModel.apply {
            titleLiveData.observe(this@CardActivity, {
                title = it
            })

            fbAddRow.setOnClickListener {
                addRow {
                    scrollToPosition(sortedRows.size)
                }
            }
        }
        adapter.setCellClickListener(rowClickListener)
        // наблюдатель для события выделения ячейки
        selectedModeObserve()

    }

    private fun duplicateRows() {
        viewModel.apply {
            duplicateRows()
            scrollToPosition(sortedRows.size)
        }
    }

    private fun pasteRows() {
        viewModel.apply {
            pasteRows(app().copyRowList)
            selectMode.value = SelectMode.NONE
        }

    }

    private fun copySelectedRows(isCut: Boolean) {
        app().copyRowList = viewModel.getSelectedRows()
        if (isCut)
            removeSelectedRows()
        else
            viewModel.selectMode.value = SelectMode.ROW
    }

    private fun copySelectedCell(isCut: Boolean) {
        viewModel.apply {
            app().copyCell = getCopySelectedCell(isCut)
            // заного назначаю чтоб меню создалось заного и иконка вставки если надо станет серой или белой
            selectMode.value = SelectMode.CELL
        }
        if (isCut)
            notifyAdapter()
    }

    private fun pasteCell() {
        // на вход принимается функция которая должна обновить строку после вставки
        viewModel.pasteCell(app().copyCell) {
            updateInputLayout()
        }
    }

    private fun removeSelectedRows() {
        runMainOnLifeCycle {
            viewModel.apply {
                deleteRows { position ->
                    adapter.notifyItemChanged(position)
                }
            }
        }
    }

    private val waitForAnimationsToFinishRunnable =
        Runnable { waitForAnimationsToFinish() }

    // When the data in the recycler view is changed all views are animated. If the
// recycler view is animating, this method sets up a listener that is called when the
// current animation finishes. The listener will call this method again once the
// animation is done.
    private fun waitForAnimationsToFinish() {
        if (recycler.isAnimating) { // The recycler view is still animating, try again when the animation has finished.
            recycler.itemAnimator?.isRunning(animationFinishedListener)
            return
        }
        // The recycler view have animated all it's views
        notifyAdapter()
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

    private fun hideUnnecessaryElementsFromTotalAmount() {
        totalAmountView.apply {
            datePeriodCard.visibility = GONE
            nameCard.visibility = GONE
        }
    }

    private fun hideViewWhileScroll() {
        val animListener = object :
            Animator.AnimatorListener {
            override fun onAnimationRepeat(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {
                viewModel.selectMode.value.let { selectMode ->
                    if (selectMode != SelectMode.NONE) {
                        if (!fbAddRow.isShown)
                            fbAddRow.show()
                    } else {
                        if (totalAmountView.translationY == 0f)
                            fbAddRow.show()
                        if (totalAmountView.translationY == totalAmountView.height.toFloat())
                            fbAddRow.hide()
                    }
                }
            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationStart(p0: Animator?) {
            }
        }

        totalAmountView.animate().setListener(animListener)
        appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->

            if (viewModel.selectMode.value != SelectMode.NONE)
                return@OnOffsetChangedListener
            val heightToolbar = appBarLayout.toolbar.height

            val isHide = -verticalOffset == heightToolbar
            val isShow = verticalOffset == 0

//            containerPlate.apply {
            if (isShow) {
                fbAddRow.show()
//                    animate().translationY(0f)
            }
            if (isHide) {
                fbAddRow.hide()
//                    animate().translationY(height.toFloat())

            }
//            }
        })
    }

    override fun onBackPressed() {
        if (fbAddRow.isSpeedDialMenuOpen)
            fbAddRow.closeSpeedDialMenu()
        else
            viewModel.apply {
                selectMode.value.let { selectMode1 ->
                    if (selectMode1 != SelectMode.NONE) {
                        unSelect()
                    } else {
                        updatedCardStatus.observe(this@CardActivity) {
                            if (!it)
                                finish()
                        }
                    }
                }
            }
    }

    val rowClickListener = object : RowClickListener {
        override var isOpenEditDialog: Boolean = false
        override fun cellClick(rowPosition: Int, cellPosition: Int) {
            if (isLongClick) {
                viewModel.rowClicked(rowPosition) {
                    notifyAdapter()
                    adapter.notifyItemChanged(rowPosition)
                }
            } else {
                if (viewModel.selectMode.value == SelectMode.ROW) {
                    isLongClick = true
                    cellClick(rowPosition, cellPosition)
                    return
                }
                viewModel.cellClicked(
                    rowPosition,
                    cellPosition
                ) { isDoubleTap ->
                    if (isDoubleTap && !isOpenEditDialog) {
                        isOpenEditDialog = true
                        editCell()
                    } else {
                        showInputCell()
                    }
                }
            }
        }
    }

    private fun editCell() {
        /**
         * тип:
         *      text:          просто показать там ткест в одну строку с прокруткой
         *      цифры:         показать сурс то как мы ввели туда эти цифры уравненем или просто цифры со скролом
         *          формула:   показать формулу (со скролом) и при нажатии предупреждение
         *      контакт:       показать номер человека можно настроить на показ только имени а при наведении можно будет посмотреть номер
         *      дата:          показать полную дату с временем в плоть до секунды и дня недели
         *      цвет:          показать базовую палитру в ряд для выбора с скролом
         *      переключатель: не нужно показывать
         *      изображение:   показать в ряд изображения которые есть с прокрутой, при нажатии меняется ава ячейки
         *      чат:           показать последнее сообщение
         *
         *      [кнопка показа полного экрана] [место для показа инпута] [кнопка ок(галочка)]
         * */


        viewModel.apply {
            getSelectCellPairIndexes()?.let {
                val columnPosition = it.second
                val rowPosition = it.first

                val row = sortedRows[rowPosition]
                val column = card.columns[columnPosition]
                val selectCell = sortedRows[rowPosition].cellList[columnPosition]

                EditCellControl.showEditCellDialog(
                    cardActivity = this@CardActivity,
                    column = column,
                    sourceValue = selectCell.sourceValue
                ) { newValue ->
                    runOnViewModel {
                        selectCell.sourceValue = newValue

                        card.updateTypeControlCell(row, columnPosition)
                        if (column is NumberColumn) {
                            card.columns.forEachIndexed { numberColumnPosition, column ->
                                if (column is NumberColumn && numberColumnPosition != columnPosition)
                                    card.updateTypeControlCell(
                                        row,
                                        numberColumnPosition
                                    )
                            }
                        }

                        updateRowToDB(row)
                        updateTotals()

                        updateAdapter()
                        main {
                            updateInputLayout()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModelInitializer.invokeOnCompletion {
            viewModel.selectMode.updateValue()

        }
    }

    private fun scrollToPosition(position: Int) {
        recycler.scrollToPosition(position) //  у нас на одну больше из за отступа для плейт
        appBar.setExpanded(false, true)
    }

    private fun showInputCell() {
        updateInputLayout()
        inputCellLayout.post {
            inputCellLayout
                .animate()
                .translationY(-inputCellLayout.height.toFloat())
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun updateInputLayout() {
        viewModel.apply {
            getSelectCellPairIndexes()?.let {
                val rowPosition = it.first
                val columnPosition = it.second

                val cell = sortedRows[rowPosition].cellList[columnPosition]
                val layout = InputLayout.inflateInputLayout(
                    this@CardActivity,
                    cell,
                    object : InputLayout.InputCallBack {
                        override fun openCellDialog() {
                            editCell()
                        }

                        override fun notifyCellChanged() {
                            runOnLifeCycle {
                                card.updateTypeControlCell(sortedRows[rowPosition], columnPosition)
                                updateAdapter()
                            }
                        }

                        override fun close() {
                            onBackPressed()
                        }

                    })
                inputCellLayout.removeAllViews()
                inputCellLayout.addView(layout)
            }
        }
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
}

/**
 * - обновление активити
 * - выделение ячейки
 * - обновление ячейки (вырезать, вставить, изменить)
 * - добавление строки (вставка)
 * - удаление строк
 * - вырезать строки
 *
 * */