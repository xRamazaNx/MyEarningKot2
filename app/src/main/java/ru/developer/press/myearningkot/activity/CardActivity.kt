package ru.developer.press.myearningkot.activity

import android.animation.Animator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView.ItemAnimator.ItemAnimatorFinishedListener
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_card.*
import kotlinx.android.synthetic.main.activity_card.view.*
import kotlinx.android.synthetic.main.card.*
import kotlinx.android.synthetic.main.card.view.*
import kotlinx.coroutines.*
import ru.developer.press.myearningkot.*
import ru.developer.press.myearningkot.App.Companion.dao
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.model.*
import ru.developer.press.myearningkot.viewmodels.CardViewModel
import ru.developer.press.myearningkot.viewmodels.CardViewModel.SelectMode
import ru.developer.press.myearningkot.viewmodels.ViewModelCardFactory
import java.lang.Runnable

open class CardActivity : BasicCardActivity() {

    private val editCardRegister =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                val data = it.data
                if (data != null) {
                    val id = data.getStringExtra(CARD_ID) ?: ""
                    if (id.isNotEmpty()) {
                        if (viewModel == null) {
                            recreate()
                        } else {
                            viewModel?.runOnViewModel {
                                val card = io {
                                    dao.getCard(id)
                                }

                                viewModel!!.updateCard(card)
                                createTitles()
                                updateHorizontalScrollSwitched()
                                initRecyclerView()
                                viewModel?.apply {
                                    selectMode.value = SelectMode.NONE
                                }

                                onResume()
                            }
                        }
                    }
                }
            }
    override var viewModel: CardViewModel? = null
    private var isLongClick = false
    private val launch = runMainOnLifeCycle {
        val id = intent.getStringExtra(CARD_ID)!!
        val card = dao.getCard(id)
        createViewModel(card)
        progressBar.visibility = GONE
        doStart()
        viewModel?.apply {
            titleLiveData.observe(this@CardActivity, {
                title = it
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // внести все нужные события ид, подписки и т.д.
        launch.start()
        tableView.isLong.observe(this, {
            isLongClick = it
        })
        fbAddRow.setContentCoverColour(Color.TRANSPARENT)
        hideViewWhileScroll()
    }


    private fun selectedModeObserve() {
        val menu = toolbar.menu
        viewModel?.selectMode?.observe(this, { selectMode ->
            menu.clear()
            when (selectMode) {
                SelectMode.CELL -> {
                    menuInflater.inflate(R.menu.cell_menu, menu)
                    // ставим иконку вставить в зависимости доступности вставки
                    if (viewModel!!.isEqualTypeCellAndCopyCell(app().copyCell)) {
                        menu.findItem(R.id.pasteCell).setIcon(R.drawable.ic_paste)
                    } else
                        menu.findItem(R.id.pasteCell).setIcon(R.drawable.ic_paste_disabled)
                    supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_check)
                }

                SelectMode.ROW -> {
                    menuInflater.inflate(R.menu.row_menu, menu)
                    if (viewModel!!.isCapabilityPaste(app().copyRowList)) {
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
        })

    }

    private fun createViewModel(card: Card) {
        viewModel = ViewModelProvider(
                this, ViewModelCardFactory(
                this,
                card
        )
        ).get(CardViewModel::class.java)
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
                        PrefCardInfo.CardCategory.CARD,
                        activity = this,
                        cardId = viewModel!!.card.refId,
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

    private fun duplicateRows() {
        viewModel?.apply {
            duplicateRows()
            scrollToPosition(sortedRows.size)
        }
    }

    private fun pasteRows() {
        viewModel?.apply {
            pasteRows(app().copyRowList)
            selectMode.value = SelectMode.NONE
        }

    }

    private fun copySelectedRows(isCut: Boolean) {
        app().copyRowList = viewModel?.getSelectedRows()
        if (isCut)
            removeSelectedRows()
        else
            viewModel?.selectMode?.value = SelectMode.ROW
    }

    private fun copySelectedCell(isCut: Boolean) {
        viewModel?.apply {
            app().copyCell = getCopySelectedCell(isCut)
            // заного назначаю чтоб меню создалось заного и иконка вставки если надо станет серой или белой
            selectMode.value = SelectMode.CELL
        }
        if (isCut)
            notifyAdapter()
    }

    private fun pasteCell() {
        // на вход принимается функция которая должна обновить строку после вставки
        viewModel?.pasteCell(app().copyCell) {
            // обновление строки после вставки данных
            adapter.notifyItemChanged(it)
        }
    }

    private fun removeSelectedRows() {
        runMainOnLifeCycle {
            viewModel?.apply {
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
                viewModel?.selectMode?.value?.let { selectMode ->
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

            if (viewModel?.selectMode?.value != SelectMode.NONE)
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
            viewModel?.apply {
                selectMode.value?.let { selectMode1 ->
                    if (selectMode1 != SelectMode.NONE) {
                        unSelect()
                    } else {
                        updatedCardStatus.observe(this@CardActivity) {
                            if (!it)
                                finish()
                        }
                    }
                }
            } ?: finish()
    }

    private val rowClickListener = object : RowClickListener {
        override fun cellClick(rowPosition: Int, cellPosition: Int) {
            if (isLongClick) {
                viewModel?.rowClicked(rowPosition) {
                    notifyAdapter()
                    adapter.notifyItemChanged(rowPosition)
                }
            } else {
                if (viewModel!!.selectMode.value == SelectMode.ROW) {
                    isLongClick = true
                    cellClick(rowPosition, cellPosition)
                    return
                }
                viewModel?.cellClicked(
                        rowPosition,
                        cellPosition
                ) { isDoubleTap ->
                    if (isDoubleTap) {
                        editCell()
                    }
                }
            }
        }
    }

    private fun editCell() {
        viewModel?.editCell(this)
    }

    override fun onResume() {
        super.onResume()
        launch.invokeOnCompletion {
            viewModel?.apply {
                val value = selectMode.value
                selectMode.value = value
            }

            fbAddRow.setOnClickListener {
                viewModel?.apply {
                    addRow {
                        scrollToPosition(sortedRows.size)
                    }
                }
            }
            adapter.setCellClickListener(rowClickListener)
            // наблюдатель для события выделения ячейки
            selectedModeObserve()
            hideUnnecessaryElementsFromTotalAmount()
        }
    }

    private fun scrollToPosition(position: Int) {
        recycler.scrollToPosition(position) //  у нас на одну больше из за отступа для плейт
        appBar.setExpanded(false, true)
    }
}