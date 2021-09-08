package ru.developer.press.myearningkot.viewmodels

import androidx.lifecycle.ViewModel
import ru.developer.press.myearningkot.App.Companion.dao
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.database.Page
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.helpers.scoups.updateTypeControlColumn
import ru.developer.press.myearningkot.model.NumberColumn
import java.util.concurrent.atomic.AtomicBoolean

// этот класс создается (ViewModelProviders.of(this).get(Class::class.java))
// и существует пока существует активити до уничтожения он только обновляет данные представления
class MainViewModel : ViewModel() {

    companion object {
        var cardClick: (cardId: String) -> Unit = {}
        var cardLongClick: (cardId: String) -> Unit = {}
        var isSelectMode: AtomicBoolean = AtomicBoolean(false)
    }

    private val selectedCardIds = mutableSetOf<String>()

    private var openedCardId: String = ""
    private val pageList: MutableList<MyLiveData<Page>> = mutableListOf()
    var currentPagePosition = 0 // TODO: 21.08.2021  назначать при переключении

    val initializeLD = liveData<Boolean>()
    suspend fun initialization() {
        dao.getPageList().forEach { page ->
            page.cards.forEach { it.value!!.isUpdating = false }
            pageList.add(liveDataFromMain(page))
        }
        initializeLD.postValue(true)
    }

    init {
        initializeLD.postValue(false)
        isSelectMode.set(false)
        // нажали на карточку
        cardClick = { idCard ->
            if (isSelectMode.get()) {
                cardLongClick.invoke(idCard)
            } else {
                openedCardId = idCard
                openCardEvent.call(idCard)
            }
        }

        cardLongClick = { idCard ->
            if (selectedCardIds.contains(idCard)) {
                selectedCardIds.remove(idCard)
            } else {
                selectedCardIds.add(idCard)
            }
            isSelectMode.set(selectedCardIds.isNotEmpty())
        }
    }

    val openCardEvent = SingleLiveEvent<String>()

    fun getPages(): MutableList<MyLiveData<Page>> {
        return pageList
    }

    fun getTabName(position: Int): String {
        return pageList[position].value!!.name
    }

    private fun getPositionCardInPage(indexPage: Int, card: Card): Int {
        val cardsInPage = pageList[indexPage].value!!.cards
        // тут будет логика определения позиции с учетом сортировки
//        cardsInPage.forEachIndexed { index, cardTemp ->
//            if (card === cardTemp) {
//                return index
//            }
//        }
        return cardsInPage.size
    }

    fun createCard(
        indexPage: Int,
        sampleID: String,
        name: String,
        updateView: (position: Int) -> Unit
    ) {
        runOnViewModel {

            val pageLiveData = pageList[indexPage]
            val page = pageLiveData.value!!

            val card: Card = dao.getSampleCard(sampleID)
            // для того что бы удвлить времянки
            card.rows.clear()
            // добавляем в базу данных новую Card присовение ид очень важно
            card.pageId = page.refId
            if (name.isNotEmpty())
                card.name = name
            card.isUpdating = true
            // добавляем в базу
            dao.addCard(card)
            // узнать позицию для добавления во вкладку и для ее обновления во вью... а ее надо узнавать смотря какая сортировка
            val position = getPositionCardInPage(indexPage, card)
            // добавляем во вкладку
            page.cards.add(position, liveDataFromMain(card))
//                pageLiveData.value = page
            main {
                updateView(position)
            }
        }
    }

    fun addPage(pageName: String, callback: (Page?) -> Unit): Boolean {
        val find: MyLiveData<Page>? = pageList.find { it.value?.name == pageName }
        runOnViewModel {
            if (find == null) {
                val page: Page = dao.addPage(pageName, pageList.size)
                pageList.add(liveDataFromMain(page))
                callback.invoke(page)
            } else {
                callback.invoke(null)
            }
        }
        // если нулл значит такой вкладки нет и можно добавить
        return find == null
    }

//    fun updateCardInPage(idCard: Long, selectedTabPosition: Int): Int {
//        var position = 0
//        val cards = pageList[selectedTabPosition].value!!.cards
//        cards.forEachIndexed { index, card ->
//            if (card.value!!.id == idCard) {
//                runOnIoViewModel {
//                    val updatedCard = dataController.getCard(idCard)
//                    calcCard(updatedCard)
//                    cards[index].postValue(updatedCard)
//                    position = index
//                    return@launch
//                }
//            }
//        }
//        return position
//    }

    fun getCardInPage(selectedTabPosition: Int, position: Int): Card {
        return pageList[selectedTabPosition].value!!.cards[position].value!!
    }

    private fun calcCard(card: Card) {
        card.apply {
            columns.filterIsInstance<NumberColumn>().forEach { column ->
                updateTypeControlColumn(column)
            }
        }
    }

    fun calcAllCards() {
        pageList.forEach { page ->
            page.value!!.cards.forEach { card ->
                card.value?.let {
                    calcCard(it)
                }
            }
        }
    }

//    fun pageColorChanged(color: Int, selectedPage: Int) {
//        val liveData = pageList[selectedPage]
//        val page: Page? = liveData.value
//        page?.let {
//            dataController.updatePage(it)
//            liveData.postValue(it)
//        }
//    }

    fun checkUpdatedCard(selectedTabPosition: Int) {
        val page = pageList[selectedTabPosition]
        val find = page.value!!.cards.find { it.value!!.refId == openedCardId }
        find?.let {
            runOnViewModel {
                val card = dao.getCard(openedCardId)
                openedCardId = ""
                card.isUpdating = true
                it.postValue(card)
            }

        }
//
//        pageList.forEach { liveData ->
//            liveData.value?.cards?.forEach {
//                val value = it.value
//                if (value?.id == openedCardId) {
//                    value.isUpdating = true
//                    it.postValue(value)
//                }
//                return
//            }
//        }
    }

    fun loginSuccess() {
        runOnViewModel {
            dao.syncRefs()
        }
    }

    fun changedPage(id: String, updateViewPager: () -> Unit) {
        synchronized(pageList) {
            runOnViewModel {
                val pageDB = dao.getPage(id)
                val find = pageList.find { it.value!!.name == pageDB!!.name }
                if (find == null) {
                    pageList.add(liveDataFromMain(pageDB))
                    pageList.sortBy { it.value?.position }
                    updateViewPager.invoke()
                } else {
                    // на всякий пожарный
                    find.value!!.refId = pageDB!!.refId
                    find.updateValue()
                }
            }
        }
    }

    fun deletePage(tabPosition: Int, updateView: (position: Int) -> Unit) {
        runOnViewModel {
            val pageLiveData = pageList[tabPosition]
            pageList.removeAt(tabPosition)
            dao.deletePage(pageLiveData.value!!)
            main {
                updateView.invoke(tabPosition)
            }
        }
    }

    fun getPage(refId: String): Page? = pageList.find { it.value?.refId == refId }?.value

    fun getPage(position: Int): Page? = pageList[position].value


//    fun deletePage(position: Int, deleteEvent: (Boolean) -> Unit) {
//        val isEmpty = dataController.pageCount == 0
//        if (!isEmpty) {
//            dataController.pageList.removeAt(position)
//        }
//        deleteEvent(!isEmpty)
//    }

}