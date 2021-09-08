package ru.developer.press.myearningkot.activity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.contains
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import kotlinx.android.synthetic.main.activity_card.*
import kotlinx.android.synthetic.main.card.*
import kotlinx.android.synthetic.main.card.view.*
import kotlinx.coroutines.*
import org.jetbrains.anko.backgroundColorResource
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.adapters.AdapterRow
import ru.developer.press.myearningkot.adapters.DiffRows
import ru.developer.press.myearningkot.dagger.CardViewModelModule
import ru.developer.press.myearningkot.dagger.DaggerCardComponent
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.scoups.inflatePlate
import ru.developer.press.myearningkot.helpers.scoups.inflateView
import ru.developer.press.myearningkot.helpers.scoups.updateTotalAmount
import ru.developer.press.myearningkot.logD
import ru.developer.press.myearningkot.viewmodels.CardViewModel


interface UIControl {
    // для первичного и единстенного вызова
    // обновляет активити после инициализации viewModel и т.д.
    fun updateActivity()

    // 3 метода для адаптера
    fun notifyItem(position: Int)
    fun notifyItemRange(start: Int, count: Int)
    fun notifyItems()
}

// чтобы узнать мы открыли в настройках карточку или шаблон
class CardInfo(var idCard: String, var cardCategory: CardCategory) {
    enum class CardCategory {
        CARD, SAMPLE
    }
}

@SuppressLint("Registered")
abstract class CommonCardActivity : AppCompatActivity(), UIControl {
    protected lateinit var adapter: AdapterRow
    lateinit var columnContainer: LinearLayout
    lateinit var viewModel: CardViewModel

    protected val viewModelInitializer: Job = lifecycleScope.launchWhenCreated {
        viewModel =
            DaggerCardComponent
                .builder()
                .cardViewModelModule(CardViewModelModule(this@CommonCardActivity)).build()
                .createCardViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card)
        totalAmountView.backgroundColorResource = R.color.colorPrimary
        _all.backgroundColorResource = R.color.colorPrimary
        columnContainer = LinearLayout(this).also {
            it.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setTitleTextColor(getColorFromRes(R.color.colorOnPrimary))

        // для того что бы тоталвью не пропускал сквозь себя клики на ресайклер с записями
//        totalAmountView.callOnClick()

    }

    @SuppressLint("InflateParams")
    fun doStart() {
        viewModel.apply {
            createTitles()
            // подписываем
            cardLiveData.observe(this@CommonCardActivity, {
                it.inflatePlate(totalAmountView)
            })
            totalLiveData.observe(this@CommonCardActivity, {
                it.updateTotalAmount(totalAmountView)
            })
        }
        horizontalScrollSwitch()
        initRecyclerView()
    }

    @SuppressLint("InflateParams")
    fun createTitles() {
        // создаем заголовки колон и подписываемся
        columnContainer.removeAllViews()
        repeat(viewModel.card.columns.size) {
            val title: TextView = layoutInflater.inflate(R.layout.title_column, null) as TextView
            columnContainer.addView(title)
        }

        viewModel.columnsLiveData.observe(this@CommonCardActivity) { columns ->
            columns.forEachIndexed { index, column ->
                val title = columnContainer.getChildAt(index)
                column.inflateView(title as TextView)
            }
        }
    }

    fun horizontalScrollSwitch() {

        val currentLayout: View?
        if (viewModel.isEnableHorizontalScroll()) {
            if (columnDisableScrollContainer.contains(columnContainer)) {
                columnDisableScrollContainer.removeView(columnContainer)
            }
            columnDisableScrollContainer.visibility = GONE
            columnScrollContainer.visibility = VISIBLE
            if (!columnScrollContainer.contains(columnContainer))
                columnScrollContainer.addView(columnContainer)
            currentLayout = columnScrollContainer
        } else {
            if (columnScrollContainer.contains(columnContainer)) {
                columnScrollContainer.removeView(columnContainer)
            }
            columnScrollContainer.visibility = GONE
            columnDisableScrollContainer.visibility = VISIBLE

            if (!columnDisableScrollContainer.contains(columnContainer))
                columnDisableScrollContainer.addView(columnContainer)
            currentLayout = columnDisableScrollContainer
        }
        currentLayout?.backgroundColorResource = R.color.colorPrimary
    }

    protected open fun initRecyclerView() {
        recycler.apply {

            layoutManager = CustomLinearLayoutManager(this@CommonCardActivity)

            this@CommonCardActivity.adapter = getAdapterForRecycler()

            adapter = this@CommonCardActivity.adapter
        }

    }

    protected fun getAdapterForRecycler(): AdapterRow {
        return AdapterRow(null, viewModel, totalAmountView).also {
            it.setHasStableIds(true)
            viewModel.diffRowsUpdater = DiffRows(viewModel.sortedRows, it)
        }
    }

    // что бы recycler не выебывался когда удаляю айтемы.
    private class CustomLinearLayoutManager(context: Context) : LinearLayoutManager(context) {
        override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
            try {
                super.onLayoutChildren(recycler, state)
            } catch (e: IndexOutOfBoundsException) {
                logD("Inconsistency detected")
            }
        }
    }

    override fun notifyItem(position: Int) {
        adapter.notifyItemChanged(position)
    }

    override fun notifyItemRange(start: Int, count: Int) {
        adapter.notifyItemRangeChanged(start, count)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun notifyItems() {
        adapter.notifyDataSetChanged()
    }
}