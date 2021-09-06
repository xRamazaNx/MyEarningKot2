package ru.developer.press.myearningkot.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.developer.press.myearningkot.PageFragment
import ru.developer.press.myearningkot.database.Page
import ru.developer.press.myearningkot.helpers.MyLiveData

class AdapterViewPagerToMain(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    pages: MutableList<MyLiveData<Page>>
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragments = mutableListOf<PageFragment>()

    init {
        pages.forEach {
            addPage(it.value!!.refId)
        }
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(pagePosition: Int): Fragment {
        return fragments[pagePosition]
    }

    fun insertCardToPosition(positionPage: Int, positionCard: Int) {
        fragments[positionPage].insertToPosition(positionCard)
    }

    fun addPage(pageId: String) {
        fragments.add(
            PageFragment.create(pageId)
        )
    }

    fun deletePage(position: Int) {
        fragments.removeAt(position)
        notifyItemRemoved(position)
    }
}
