package ru.developer.press.myearningkot.viewmodels

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.ViewModel
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.sample_card_item.view.*
import org.jetbrains.anko.dip
import org.jetbrains.anko.matchParent
import org.jetbrains.anko.wrapContent
import ru.developer.press.myearningkot.App.Companion.dao
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.activity.CardInfo
import ru.developer.press.myearningkot.activity.CreateCardActivity
import ru.developer.press.myearningkot.activity.startPrefActivity
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.dialogs.ChoiceDialog
import ru.developer.press.myearningkot.dialogs.choiceDialog
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.scoups.inflateView
import ru.developer.press.myearningkot.model.NumerationColumn
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.positiveButton

@SuppressLint("InflateParams")
class CreateCardViewModel : ViewModel() {
    fun updateSamples(update: () -> Unit) {
        runOnViewModel {
            sampleList = dao.getSampleList().toMutableList()
            update.invoke()
        }
    }

    fun getAdapter(): AdapterForSamples {

        val adapterForSamples = AdapterForSamples(sampleList.fold(ArrayList()) { list, card ->
            list.add(AdapterForSamples.SampleItem(card))
            list
        })

        adapterForSamples.deleteCard = { deleteId ->
            runOnViewModel {
                dao.deleteSample(deleteId)
                sampleList.remove(sampleList.find { it.refId == deleteId })
            }

        }
        adapterForSamples.updateItemInCard = { item ->
            item?.let { sampleItem ->
                val find = sampleList.find { it.refId == sampleItem.card.refId }!!
                sampleItem.card = find
            }
        }
        return adapterForSamples
    }

    lateinit var sampleList: MutableList<Card>

    class AdapterForSamples(val list: MutableList<SampleItem>) : RecyclerView.Adapter<AdapterForSamples.SampleCardHolder>() {

        lateinit var deleteCard: (String) -> Unit
        var selectId: String? = null

        data class SampleItem(var card: Card) {
            var isSelect: Boolean = false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SampleCardHolder {
            val context = parent.context
            val view: View = LayoutInflater.from(context)
                    .inflate(R.layout.sample_card_item, null)
                    .apply {
                        layoutParams = LinearLayout.LayoutParams(matchParent, wrapContent)
                    }

            return SampleCardHolder(view)
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: SampleCardHolder, position: Int) {
            val sampleItem = list[position]
            holder.bind(sampleItem) {
                val find = list.find { it.isSelect }
                if (find != null) {
                    find.isSelect = false
                    notifyItemChanged(list.indexOf(find))
                }
                sampleItem.isSelect = true
                selectId = sampleItem.card.refId
                notifyItemChanged(position)
            }
        }

        fun updateItem(id: String) {
            val find = list.find { it.card.refId == id }
            updateItemInCard(find)
            notifyItemChanged(list.indexOfFirst { it.card.refId == id })
        }

        lateinit var updateItemInCard: (find: SampleItem?) -> Unit

        inner class SampleCardHolder(view: View) : RecyclerView.ViewHolder(view) {
            fun bind(sampleItem: SampleItem, click: () -> Unit) {

                val context = itemView.context
                val dip = context.dip(2)
                itemView.setOnClickListener {
                    click.invoke()
                }

                itemView.sampleName.text = sampleItem.card.name

                val columnContainer = itemView.sampleColumnContainer
                columnContainer.removeAllViews()
                sampleItem.card.columns.forEach { column ->
                    if (column is NumerationColumn) return@forEach
                    val title: TextView = LayoutInflater.from(context)
                            .inflate(R.layout.title_column, null) as TextView
                    column.inflateView(title)
                    val layoutParams = title.layoutParams
                    layoutParams.width = wrapContent
                    title.layoutParams = layoutParams
                    title.setPadding(0, 0, dip, 0)
                    columnContainer.addView(title)

                }
                columnContainer.setOnClickListener { click.invoke() }

                itemView.sampleMenu.setOnClickListener { view ->
                    val popupMenu = PopupMenu(context, view)
                    popupMenu.gravity = Gravity.BOTTOM
                    popupMenu.inflate(R.menu.sample_item_menu)
                    popupMenu.show()
                    popupMenu.setOnMenuItemClickListener {
                        val activity = context as CreateCardActivity
                        when (it.itemId) {
                            R.id.edit_sample -> {
                                activity.editSampleRegister.startPrefActivity(CardInfo.CardCategory.SAMPLE,
                                                                              activity,
                                                                              sampleItem.card.refId,
                                                                              view.context.getString(
                                                                                      R.string.setting_sample))
                            }
                            R.id.delete_sample -> {
                                var choiceDialog: ChoiceDialog? = null
                                choiceDialog = choiceDialog {
                                    setTitle("Удалить шаблон \"${sampleItem.card.name}\"?")
                                    setMessage("")
                                    positiveButton(R.string.DELETE) {
                                        itemView.findViewTreeLifecycleOwner()?.runOnLifeCycle {
                                            deleteCard.invoke(sampleItem.card.refId)
                                            list.remove(sampleItem)
                                            main {
                                                notifyItemRemoved(adapterPosition)
                                            }
                                        }
                                    }
                                    negativeButton(R.string.cancel) {
                                        choiceDialog?.dismiss()
                                    }
                                }
                                choiceDialog.negativeButtonColorRes = R.color.colorRed
                                choiceDialog.show(activity.supportFragmentManager,
                                                  "delete_sample_wrong")
                            }
                        }
                        true
                    }
                }
                if (sampleItem.isSelect) {
                    itemView.sampleContainer.setBackgroundResource(R.drawable.row_selected_background)
                } else itemView.sampleContainer.setBackgroundResource(R.drawable.background_for_card)
            }
        }
    }

}