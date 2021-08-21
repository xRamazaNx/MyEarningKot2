package ru.developer.press.myearningkot.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.card.view.*
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.dip
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.activity.setShowTotalInfo
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.scoups.inflatePlate
import ru.developer.press.myearningkot.viewmodels.MainViewModel

class AdapterCard(
    context: Context,
    private val cards: MutableList<MyLiveData<Card>>
) :
    RecyclerView.Adapter<AdapterCard.CardHolder>() {
    private val selectedColor = context.getColorFromRes(R.color.colorSelectCard)
    private val selectCardDistance = context.dip(-52).toFloat()
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
        val activity = itemView.context as AppCompatActivity

        fun bind(liveData: MyLiveData<Card>) {
            liveData.observe(activity, observer { card ->

                setClick(card)
                setLongClick(card)

                card.inflatePlate(itemView)
                itemView.setShowTotalInfo(card.isShowTotalInfo)

                if (card.isUpdating) {
                    itemView.animate().alpha(0f).withEndAction {
                        itemView.animate().alpha(1f)
                        card.isUpdating = false
                    }
                }

                if (card.isSelect) {
                    itemView.translationX = selectCardDistance
                } else {
                    itemView.translationX = 0f
                }
            })
        }

        private fun setLongClick(card: Card) {
            val longClick: (View) -> Boolean = {
                switchSelection(card)
                MainViewModel.cardLongClick.invoke(card.refId)
                true
            }
            itemView.setOnLongClickListener(longClick)
            itemView.totalContainerScroll.setOnLongClickListener(longClick)
        }

        private fun setClick(card: Card) {
            val click: (View) -> Unit = {
                if (MainViewModel.isSelectMode.get()) {
                    switchSelection(card)
                }
                MainViewModel.cardClick.invoke(card.refId)
            }
            itemView.setOnClickListener(click)
            itemView.totalContainerScroll.setOnClickListener(click)
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
            itemView._all.animate()
                .translationX(x)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction(endBlock)
                .start()
        }
    }
}
