package ru.developer.press.myearningkot.adapters

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ernestoyaquello.dragdropswiperecyclerview.DragDropSwipeAdapter
import org.jetbrains.anko.*
import ru.developer.press.myearningkot.ProvideDataRows
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.RowClickListener
import ru.developer.press.myearningkot.RowDataListener
import ru.developer.press.myearningkot.helpers.animateColor
import ru.developer.press.myearningkot.helpers.getColorFromRes
import ru.developer.press.myearningkot.helpers.prefLayouts.setSelectBackground
import ru.developer.press.myearningkot.model.*

class AdapterRow(
    private var rowClickListener: RowClickListener?,
    private val provideDataRows: ProvideDataRows,
    private val totalView: View?
) : RecyclerView.Adapter<RowHolder>() {

    companion object {
        const val animatedDuration = 700L
    }

    private var cellClickPrefFunction: ((Int) -> Unit)? = null
    fun setCellClickPref(cellClickFun: ((Int) -> Unit)?) {
        cellClickPrefFunction = cellClickFun
    }

    fun setCellClickListener(_rowClickListener: RowClickListener?) {
        rowClickListener = _rowClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder {
        val context = parent.context

        if (viewType == -1)
            return RowHolder(FrameLayout(context).apply {
                backgroundColorResource = R.color.colorPrimaryDark
            })
        val width = provideDataRows.width()

        val rowHeight = context.dip(provideDataRows.rowHeight())
        val rowView = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                width,
                if (provideDataRows.isEnableSomeStroke()) {
                    minimumHeight = rowHeight
                    wrapContent
                } else rowHeight
            )
            orientation = LinearLayout.HORIZONTAL
        }
        val rowHolder = RowHolder(FrameLayout(context).apply {
            addView(rowView)
        })

        provideDataRows.columns().forEachIndexed { index, column ->
            val cellView: View = column.createCellView(context)
            if (index == 0)
                rowHolder.rowNumber = cellView as TextView

            rowView.addView(cellView)
            // держим все вью в листе для удобства использования
            rowHolder.viewList.add(cellView)
        }

        // нажатие для настройки колоны
        return rowHolder
    }

    override fun getItemViewType(position: Int): Int {
        val size = provideDataRows.sortedRows.size
        return if (size == position) -1 else 0
    }

    override fun getItemId(position: Int): Long {
        val sortedRows = provideDataRows.sortedRows
        if (position == sortedRows.size)
            return -1
        return sortedRows[position].refId.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return provideDataRows.sortedRows.size + 1 // отступ от тотал
    }

    override fun onBindViewHolder(holder: RowHolder, rowPosition: Int) {
        if (holder.itemViewType == -1) {
            holder.itemView.layoutParams =
                FrameLayout.LayoutParams(matchParent, totalView?.height ?: 0)
            return
        }

        val sortedRows = provideDataRows.sortedRows
        val row = sortedRows[rowPosition]

        // выделение ячейки без обновления холдера а то мигает мерзко
        holder.viewList.forEachIndexed { columnPosition, view ->
            val cell = row.cellList[columnPosition]
            cell.elementView = view
            cell.displayCellView()
            when {
                // колона выделена- обвести
                cell.isPrefColumnSelect -> {
                    setSelectBackground(cell.elementView)
                }
                // ячейка выделена - обвести
                cell.isSelect -> {
                    cell.setBackground(R.drawable.cell_selected_background)
                }
                else ->
                    cell.setBackground(R.drawable.cell_default_background)
            }

            if (cellClickPrefFunction != null) {
                view.setOnClickListener {
                    cellClickPrefFunction!!.invoke(columnPosition)
                }
            } else {
                if (columnPosition > 0) {
                    rowClickListener?.let { clickListener ->
                        view.setOnClickListener { _ ->
                            // информация предыдущего выделеного элемента (ячейки)
                            provideDataRows.selectCellInfo()?.let {
                                val selectedCell = it.cell
                                // если ячейка на которую кликнули не равна той что была кликнута
                                // то надо убирается выделение из предыдущего элемента
                                if (selectedCell !== cell) {
                                    selectedCell.isSelect = false
                                    selectedCell.setBackground(R.drawable.cell_default_background)
                                }
                            }
                            cell.setBackground(R.drawable.cell_selected_background)
                            clickListener.cellClick(CellInfo(cell, rowPosition, columnPosition))
                        }
                    }
                }
            }
        }

        val previousRow = if (rowPosition > 0) sortedRows[rowPosition - 1] else null
        val secondRow =
            if (rowPosition < sortedRows.lastIndex) sortedRows[rowPosition + 1] else null
        holder.bind(
            row,
            provideDataRows.columns(),
            previousRow,
            secondRow
        )
    }

}

class RowHolder(view: View) : DragDropSwipeAdapter.ViewHolder(view), RowDataListener {
    var viewList = mutableListOf<View>()
    var rowNumber: TextView? = null
    private var positionRow = 0

    fun bind(
        row: Row,
        columns: MutableList<Column>,
        previousRow: Row?,
        secondRow: Row?
    ) {
        if (itemViewType == -1) {
            return
        }
        val context = itemView.context
        row.elementView = itemView

        positionRow = adapterPosition

        rowNumber?.text = (layoutPosition + 1).toString()

        when (row.status) {
            Status.SELECT -> {
//                rowNumber?.text = "✔"
                rowNumber?.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.colorSecondary
                    )
                )

                val isPrevSelect = previousRow?.status == Status.SELECT
                val isSecondSelect = secondRow?.status == Status.SELECT
                if (isPrevSelect && isSecondSelect)
                    row.setBackground(R.drawable.row_selected_background_border)
                else if (isPrevSelect) {
                    row.setBackground(R.drawable.row_selected_background_bottom)
                } else if (isSecondSelect) {
                    row.setBackground(R.drawable.row_selected_background_top)
                } else {
                    row.setBackground(R.drawable.row_selected_background)
                }
            }
            Status.ADDED -> {
                row.status = Status.NONE
                row.elementView.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.anim_left_to_right
                    )
                )
                row.elementView.animateColor(
                    context.getColorFromRes(R.color.colorSecondaryLight),
                    Color.TRANSPARENT,
                    AdapterRow.animatedDuration
                )
            }
            Status.DELETED -> {
                if (row.elementView.animation == null) {
                    row.elementView.animateColor(
                        colorFrom = Color.TRANSPARENT,
                        colorTo = context.getColorFromRes(R.color.colorRemovedItem),
                        duration = AdapterRow.animatedDuration
                    ) {
                        row.elementView.backgroundColor = Color.TRANSPARENT
                    }
                }
            }
            else -> {
                // если есть хоть один свитч который настроен на поведение меняющее запись
                row.cellList.forEachIndexed { index, cell ->
                    val column = columns[index]
                    if (column is SwitchColumn) {
                        val behavior = column.pref().behavior
                        row.crossOut(itemView, behavior.crossOut && cell.sourceValue.toBoolean())
                    }
                }
                row.elementView.animation = null
                row.elementView.translationX = 0F
                row.setBackground(R.color.colorBackgroundCard)
            }
        }
    }

    override fun scrollRowNumber(x: Float) {
        rowNumber?.translationX = x
    }

    override fun getItemHeight(): Int {
        return itemView.height
    }


}
