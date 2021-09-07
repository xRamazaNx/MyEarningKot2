package ru.developer.press.myearningkot.dagger

import androidx.activity.viewModels
import dagger.Component
import dagger.Module
import dagger.Provides
import ru.developer.press.myearningkot.activity.*
import ru.developer.press.myearningkot.viewmodels.CardViewModel

@Module
class CardViewModelModule(private val activity: CommonCardActivity) {
    @Provides
    fun cardViewModel(): CardViewModel {
        return activity.viewModels<CardViewModel>().value.apply {
            uiControl = activity
            initialization(cardInfo())
        }
    }
    private fun cardInfo(): CardInfo {
        val id = activity.intent.getStringExtra(CARD_ID)!!
        val category = activity.intent.getStringExtra(CARD_CATEGORY)!!
        return CardInfo(id, CardInfo.CardCategory.valueOf(category))
    }
}

//@Module
//class ContextModule(private val context: Context) {
//    @Provides
//    fun context(): Context {
//        return context
//    }
//}

@Component(modules = [CardViewModelModule::class
//    , ContextModule::class
])
interface CardComponent {
    fun createCardViewModel(): CardViewModel
}