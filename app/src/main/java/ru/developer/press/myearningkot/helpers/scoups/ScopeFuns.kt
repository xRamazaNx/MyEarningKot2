package ru.developer.press.myearningkot.helpers.scoups

import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.android.synthetic.main.card.view.*
import kotlinx.android.synthetic.main.total_item_layout.view.*
import kotlinx.android.synthetic.main.total_item_value.view.*
import ru.developer.press.myearningkot.database.Card
import kotlinx.android.synthetic.main.total_item_layout.view.totalValueContainer as totalValueContainer1

fun Card.updateTotalAmount(plateView: View) {
    val totalValueLayout = plateView.totalValueContainer1
    totals.forEachIndexed { index, totalItem ->
        // лайот где валуе и линия
        val valueLayout = totalValueLayout.getChildAt(index)
        val value = valueLayout.totalValue
        totalItem.calcFormula(this)
        value.text = totalItem.value
    }
}

fun <T : DialogFragment> T.addDismissListener(dismiss: (T) -> Unit) {
    lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroy() {
            dismiss.invoke(this@addDismissListener)
        }
    })
}

fun <T : DialogFragment> T.addShownListener(shown: (T) -> Unit) {
    lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun resume() {
            shown.invoke(this@addShownListener)
        }
    })
}
