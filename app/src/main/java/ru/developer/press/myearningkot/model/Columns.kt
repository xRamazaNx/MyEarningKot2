package ru.developer.press.myearningkot.model

import android.content.Context
import android.view.View
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import ru.developer.press.myearningkot.*
import ru.developer.press.myearningkot.adapters.ParamModel
import ru.developer.press.myearningkot.database.BelongIds
import ru.developer.press.myearningkot.database.Card
import ru.developer.press.myearningkot.helpers.*
import java.math.BigDecimal
import java.util.*
import kotlin.random.Random

sealed class Column(
    var name: String,
    pageId: String,
    cardId: String
) : BelongIds(pageId, cardId), FormulaId, ElementPosition {

    @SerializedName("itf")
    override var idToFormula: Long = Random.nextLong()
    override var position: Int = -1

    companion object {
        const val minWidth: Int = 70

        @SerializedName("tc")
        var titleColor: Int = 0

        fun fromJson(json: String): Column {
            val gson = GsonBuilder().registerTypeAdapter(
                Column::class.java,
                JsonDeserializerWithColumn()
            ).create()
            return gson.fromJson(json, Column::class.java)
        }
    }

    var width: Int = minWidth

    @SerializedName(column_cast_gson)
    var className: String = javaClass.name

    @SerializedName("tp")
    val titlePref: PrefForTextView = PrefForTextView().apply {
        isBold = true
        color = titleColor
    }

    abstract fun pref(): Prefs

    fun resetTitlePref() {
        titlePref.apply {
            isBold = true
            color = titleColor
        }
    }


    @Transient
    lateinit var columnTypeControl: ColumnTypeControl

    fun createCellView(context: Context): View {
        return columnTypeControl.createCellView(context)
    }

    protected fun getProvideProperty(provideCardPropertyForCell: ProvideCardPropertyForCell): ProvideValueProperty {
        return object : ProvideValueProperty {
            override fun getWidthColumn(): Int = width
            override var provideCardPropertyForCell =
                provideCardPropertyForCell
            override var typePref: Prefs? = pref()

        }
    }

    abstract fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell)
    abstract fun setDefaultPref()
    fun getType(): ColumnType = when (this) {
        is NumerationColumn -> ColumnType.NUMERATION
        is TextColumn -> ColumnType.TEXT
        is NumberColumn -> ColumnType.NUMBER
        is DateColumn -> ColumnType.DATE
        is PhoneColumn -> ColumnType.PHONE
        is ListColumn -> ColumnType.LIST
        is ImageColumn -> ColumnType.IMAGE
        is SwitchColumn -> ColumnType.SWITCH
        is ColorColumn -> ColumnType.COLOR
    }

}

class NumerationColumn(name: String, pageId: String, cardId: String) :
    Column(name, pageId, cardId) {
    companion object {
        var color = 0
    }

    @SerializedName("cp")
    private var typePref = PrefText()
    override fun pref(): PrefText {
        return typePref
    }

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = NumerationTypeControl(getProvideProperty(provideCardProperty))
    }

    init {
        setDefaultPref()
    }

    override fun setDefaultPref() {
        typePref.resetPref()
        typePref.apply {
            prefForTextView.isItalic = true
            prefForTextView.textSize = 14
            prefForTextView.color = color
        }
    }

}

class TextColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    @SerializedName("cp")
    private var typePref = PrefText()

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = TextTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()
    }

    override fun pref(): PrefText {
        return typePref
    }
}

class NumberColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    @SerializedName("cp")
    private var typePref = PrefNumber()

    var formula: Formula = Formula()
    var inputType: InputTypeNumberColumn = InputTypeNumberColumn.MANUAL

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = NumberTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()

    }

    override fun pref(): PrefNumber {
        return typePref
    }

    fun calcFormula(row: Row, card: Card): String {
        return try {
            val string = formulaString(card, row)
            val value: Double? = Calc.evaluate(string)
            value?.let { BigDecimal(it).toPlainString() } ?: ""
        } catch (exception: Exception) {
            "Error formula cell"
        }
    }

    fun formulaString(
        card: Card,
        row: Row,
    ): String {
        val string = StringBuilder()
        try {
            formula.formulaElements.forEach {
                if (it.type == Formula.COLUMN_ID) {
                    var index = -1
                    card.columns.forEachIndexed { i, column ->
                        if (column.idToFormula.toString() == it.value) {
                            index = i
                            return@forEachIndexed
                        }
                    }
                    if (index == -1) {
                        formula.formulaElements.remove(it)
                        calcFormula(row, card)
                    } else {

                        val numberColumn = card.columns[index] as NumberColumn
                        val value: String =
                            // проверяем колона работает по формуле или ручной ввод
                            if (numberColumn.inputType == InputTypeNumberColumn.FORMULA) {
                                numberColumn.calcFormula(row, card)
                            } else {
                                val cell = row.cellList[index]
                                cell.updateTypeValue(numberColumn.typePref)
                                cell.displayValue
                            }
                        string.append(value)
                    }
                } else
                    string.append(it.value)
            }
        } catch (ex: Exception) {
            string.clear()
            string.append("")
        }
        return string.toString()
    }
}

class PhoneColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    companion object {
        var nameOfMan = ""
        var lastName = ""
        var phone = ""
        var organization = ""
    }

    @SerializedName("cp")
    private var typePref = PrefPhone()

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = PhoneTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()
    }

    override fun pref(): PrefPhone {
        return typePref
    }

    fun getPhoneParamList(): MutableList<ParamModel> {

        return mutableListOf<ParamModel>().apply {

            typePref.sort.forEach { id ->
                when (id) {
                    0 ->
                        add(ParamModel(nameOfMan, typePref.name, 0))
                    1 ->
                        add(ParamModel(lastName, typePref.lastName, 1))
                    2 ->
                        add(ParamModel(phone, typePref.phone, 2))
                    3 ->
                        add(ParamModel(organization, typePref.organization, 3))
                }
            }
        }
    }

    fun editPhoneParam(paramModel: ParamModel) {
        val check = paramModel.isCheck
        when (paramModel.id) {
            0 -> typePref.name = check
            1 -> typePref.lastName = check
            2 -> typePref.phone = check
            3 -> typePref.organization = check
        }
    }

    fun sortPositionParam(list: List<ParamModel>) {
        list.forEachIndexed { index, param ->
            typePref.sort[index] = param.id
        }
    }

}

class DateColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    @SerializedName("cp")
    private var typePref = PrefDate()

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = DateTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()
    }

    override fun pref(): PrefDate {
        return typePref
    }

}

class ColorColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    @SerializedName("cp")
    private var typePref = PrefColor()

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = ColorTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()
    }

    override fun pref(): PrefColor {
        return typePref
    }
//        выбор фигруы для цвета
}

class SwitchColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    @SerializedName("cp")
    private var typePref = PrefSwitch()

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = SwitchTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()
    }

    override fun pref(): PrefSwitch {
        return typePref
    }
    // может будут выборы типа
}

class ImageColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    @SerializedName("cp")
    private var typePref = PrefImage()

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = ImageTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()
    }

    override fun pref(): PrefImage {
        return typePref
    }

    // может рамки не знаю
}

class ListColumn(name: String, pageId: String, cardId: String) : Column(name, pageId, cardId) {
    @SerializedName("cp")
    private var typePref = ListTypePrefText()

    override fun updateTypeControl(provideCardProperty: ProvideCardPropertyForCell) {
        columnTypeControl = ListTypeControl(getProvideProperty(provideCardProperty))
    }

    override fun setDefaultPref() {
        typePref.resetPref()
    }

    override fun pref(): ListTypePrefText {
        return typePref
    }

}

