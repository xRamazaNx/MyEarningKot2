package ru.developer.press.myearningkot.model

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import org.jetbrains.anko.backgroundColorResource
import ru.developer.press.myearningkot.ElementPosition
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.database.BelongIds

class Row(
    pageId: String,
    cardId: String
) : BelongIds(pageId, cardId), Backgrounder, ElementPosition {

    @Transient
    var status = Status.NONE

    @Transient
    override var currentBackground: Int = -1

    @Transient
    override lateinit var elementView: View

    var cellList = mutableListOf<Cell>()

    fun crossOut(itemView: View, isCrossOut: Boolean) {
        val frameLayout = itemView as FrameLayout
        if (isCrossOut) {
            val drawable = ContextCompat.getDrawable(itemView.context, R.drawable.cross_line)
            frameLayout.foreground = drawable
            itemView.backgroundColorResource = R.color.textColorSecondary
        } else {
            val colorDrawable = ColorDrawable(Color.TRANSPARENT)
            frameLayout.foreground = colorDrawable

        }
    }

    fun copy(saveAll: Boolean = false): Row {
        return Row(pageId, cardId).also { copyRow ->
            if (saveAll) {
                copyRow.refId = refId
                copyRow.dateCreate = dateCreate
                copyRow.dateChange = dateChange
                copyRow.isSaveOnFire = isSaveOnFire
                copyRow.isDelete = isDelete
            }
            cellList.forEach { cell ->
                copyRow.cellList.add(cell.copy())
            }
        }
    }

    override var position: Int = -1

    override fun equals(other: Any?): Boolean {
        if (other !is Row)
            return false

        return cellList == other.cellList
                && super.equals(other as BelongIds)
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + currentBackground
        result = 31 * result + elementView.hashCode()
        result = 31 * result + cellList.hashCode()
        return result
    }
}