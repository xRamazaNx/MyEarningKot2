package ru.developer.press.myearningkot.activity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.CallSuper
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
import ru.developer.press.myearningkot.databinding.ActivityCardBinding
import ru.developer.press.myearningkot.databinding.CardBinding
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.scoups.inflatePlate
import ru.developer.press.myearningkot.helpers.scoups.inflateView
import ru.developer.press.myearningkot.helpers.scoups.updateTotalAmount
import ru.developer.press.myearningkot.logD
import ru.developer.press.myearningkot.viewmodels.CardViewModel


interface UIControl {
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
    lateinit var activityBinding: ActivityCardBinding
    lateinit var totalAmountView: CardBinding

    protected val viewModelInitializer: Job = lifecycleScope.launchWhenCreated {
        val id = intent.getStringExtra(CARD_ID)!!
        val category = intent.getStringExtra(CARD_CATEGORY)!!
        val cardInfo = CardInfo(id, CardInfo.CardCategory.valueOf(category))

        viewModel = viewModels<CardViewModel>().value
        viewModel.apply {
            val diametric = resources.displayMetrics
            displayParam.width = diametric.widthPixels
            displayParam.height = diametric.heightPixels
            uiControl = this@CommonCardActivity
            initialization(cardInfo)
            createColumnsTitles()
            updateActivity()
        }
    }

    // для первичного и единстенного вызова
    // обновляет активити после инициализации viewModel и т.д.
    @CallSuper // need to call first
    open fun updateActivity() {
        horizontalScrollSwitch()
        initRecyclerView()
        viewModel.apply {
            // подписываем
            cardLiveData.observe(this@CommonCardActivity, observer {
                it.inflatePlate(totalAmountView)
            })
            totalLiveData.observe(this@CommonCardActivity, observer {
                it.updateTotalAmount(totalAmountView.root)
            })
            columnsLiveData.observe(this@CommonCardActivity, observer { columns ->
                val columnCount = columns.size
                if (columnContainer.childCount != columnCount) {
                    createColumnsTitles()
                } else
                    columns.forEachIndexed { index, column ->
                        val title = columnContainer.getChildAt(index)
                        column.inflateView(title as TextView)
                    }
            })
        }

        activityBinding.progressBar.visibility = GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        activityBinding = ActivityCardBinding.inflate(layoutInflater)
        totalAmountView = activityBinding.totalAmountView
        
        setContentView(activityBinding.root)

        totalAmountView.root.backgroundColorResource = R.color.colorPrimary
        totalAmountView.all.backgroundColorResource = R.color.colorPrimary
        columnContainer = LinearLayout(this).also {
            it.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
        }

        setSupportActionBar(activityBinding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activityBinding.toolbar.setTitleTextColor(getColorFromRes(R.color.colorOnPrimary))

        // для того что бы тоталвью не пропускал сквозь себя клики на ресайклер с записями
//        totalAmountView.callOnClick()

    }

    @SuppressLint("InflateParams")
    fun createColumnsTitles() {
        // создаем заголовки колон и подписываемся
        fun create(columnCount: Int) {
            columnContainer.removeAllViews()
            repeat(columnCount) {
                val title: TextView =
                    layoutInflater.inflate(R.layout.title_column, null) as TextView
                columnContainer.addView(title)
            }
        }

        create(viewModel.card.columns.size)
        viewModel.updateColumnDL()
    }

    fun horizontalScrollSwitch() {

        val currentLayout: View?
        val columnDisableScrollContainer = activityBinding.columnDisableScrollContainer
        val columnScrollContainer = activityBinding.columnScrollContainer
        
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
        currentLayout.backgroundColorResource = R.color.colorPrimary
    }

    protected open fun initRecyclerView() {
        activityBinding.recycler.apply {

            layoutManager = CustomLinearLayoutManager(this@CommonCardActivity)

            this@CommonCardActivity.adapter = getAdapterForRecycler()

            adapter = this@CommonCardActivity.adapter
        }

    }

    protected fun getAdapterForRecycler(): AdapterRow {
        return AdapterRow(null, viewModel, totalAmountView.root).also {
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