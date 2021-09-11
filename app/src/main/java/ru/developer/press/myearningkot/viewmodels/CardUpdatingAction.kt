package ru.developer.press.myearningkot.viewmodels

import ru.developer.press.myearningkot.database.Card

class CardUpdatingAction(
    private val card: Card,
    private vararg val actions: Action
) {

    fun runIfFind(action: Action, run: (Card) -> Unit) {
        actions.find { it == action }?.let {
            run.invoke(card)
        }
    }

    enum class Action {
        inflateView,
        updateTotals
    }
}