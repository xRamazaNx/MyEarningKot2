package ru.developer.press.myearningkot.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.jetbrains.anko.dip
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.databinding.CardBinding
import ru.developer.press.myearningkot.helpers.SingleObserverLiveData
import ru.developer.press.myearningkot.helpers.observer
import ru.developer.press.myearningkot.helpers.scoups.inflatePlate
import ru.developer.press.myearningkot.helpers.scoups.setClickToTotals
import ru.developer.press.myearningkot.viewmodels.MainViewModel

class AdapterCard(
    context: Context,
    private val cards: MutableList<SingleObserverLiveData<Card>>
) :
    RecyclerView.Adapter<AdapterCard.CardHolder>() {
    private val selectCardDistance = context.dip(-42).toFloat()
    private lateinit var inflater: LayoutInflater
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder {
        if (!this::inflater.isInitialized) {
            inflater = LayoutInflater.from(parent.context)
        }
        val view = inflater.inflate(R.layout.card, parent, false)
        return CardHolder(view)
    }

    override fun getItemCount(): Int {
        return cards.size
    }

    override fun onBindViewHolder(holder: CardHolder, position: Int) {
        holder.bind(cards[position])
    }

    inner class CardHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardBinding = CardBinding.bind(view)
        private val observer = observer<Card> { card ->

            card.inflatePlate(cardBinding)

            cardBinding.root.setOnClickListener {
                click.invoke(it, card)
            }
            cardBinding.root.setOnLongClickListener {
                longClick.invoke(it, card)
            }

            card.setClickToTotals(cardBinding) { view, isLong, _, _ ->
                if (isLong) {
                    longClick.invoke(view, card)
                } else {
                    click.invoke(view, card)
                }
            }

            if (card.isUpdating) {
                card.isUpdating = false
                cardBinding.root.animate().alpha(0f).withEndAction {
                    cardBinding.root.animate().alpha(1f)
                }
            }

            if (card.isSelect) {
                cardBinding.all.translationX = selectCardDistance
            } else {
                cardBinding.all.translationX = 0f
            }
        }

        fun bind(liveData: SingleObserverLiveData<Card>) {
            liveData.observe(cardBinding.root.context as AppCompatActivity, observer)
        }

        private val click: (View, Card) -> Unit = { _, card ->
            if (MainViewModel.isSelectMode.get()) {
                switchSelection(card)
            }
            MainViewModel.cardClick.invoke(card.refId)
        }

        private val longClick: (View, Card) -> Boolean = { _, card ->
            switchSelection(card)
            MainViewModel.cardLongClick.invoke(card.refId)
            true
        }

        private fun switchSelection(card: Card) {
            if (card.isSelect) {
                animTranslationX(0f)
            } else {
                animTranslationX(selectCardDistance)
            }
            card.isSelect = !card.isSelect
        }

        private fun animTranslationX(x: Float, endBlock: () -> Unit = {}) {
            cardBinding.all.animate()
                .translationX(x)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction(endBlock)
                .start()
        }
    }
}
