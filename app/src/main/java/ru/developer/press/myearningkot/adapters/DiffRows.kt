package ru.developer.press.myearningkot.adapters

import androidx.recyclerview.widget.DiffUtil
import ru.developer.press.myearningkot.helpers.main
import ru.developer.press.myearningkot.model.Row

class DiffRows(
        old: List<Row>,
        private val adapter: AdapterRow
) : DiffUtil.Callback() {

    private val oldList: MutableList<Row> = mutableListOf()
    private val newList: MutableList<Row> = mutableListOf()

    init {
        old.forEach {
            oldList.add(it.copy())
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].refId == newList[newItemPosition].refId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    suspend fun checkDiffAndUpdate(newList: MutableList<Row>) {
        this.newList.clear()
        this.newList.addAll(newList)

        val calculateDiff = DiffUtil.calculateDiff(this, false)
        main {
            calculateDiff.dispatchUpdatesTo(adapter)
        }

        oldList.clear()
        newList.forEach {
            oldList.add(it.copy())
        }
    }
}