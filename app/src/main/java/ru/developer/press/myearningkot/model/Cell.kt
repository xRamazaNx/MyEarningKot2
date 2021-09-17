package ru.developer.press.myearningkot.model

import android.graphics.Color
import android.net.Uri
import android.view.View
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import ru.developer.press.myearningkot.CellTypeControl
import ru.developer.press.myearningkot.helpers.Calc
import ru.developer.press.myearningkot.helpers.getDate
import ru.developer.press.myearningkot.helpers.getDecimalFormatNumber

data class Cell(
    @SerializedName("v")
    var sourceValue: String = ""
) : Backgrounder {

    @Transient
    override var currentBackground: Int = -1

    @Transient
    override lateinit var elementView: View

    @Transient
    var type = ColumnType.TEXT

    @Transient
    var isSelect: Boolean = false

    @Transient
    var isPrefColumnSelect = false

    @Transient
    lateinit var cellTypeControl: CellTypeControl

    @Transient
    var displayValue: String = ""

    fun updateTypeValue(pref: Prefs) {
        when (pref) {
            is PrefNumber -> {
                displayValue =
                    if (sourceValue == "")
                        ""
                    else
                        try {
                            val value = Calc.evaluate(sourceValue)!!
                            getDecimalFormatNumber(value, pref)
                        } catch (exception: Exception) {
                            "Error numbers"
                        }
            }
            is PrefDate -> {
                displayValue =
                    if (sourceValue == "")
                        ""
                    else {
                        val timeML: Long = sourceValue.toLong()
                        getDate(pref.type, timeML, pref.enableTime)
                    }
            }
            is PrefPhone -> {
                val typeValue = ValuePhone.fromJson(sourceValue)
                val number = typeValue.phone

//                val formatNumber = if (number.isNotEmpty())
//                    PhoneNumberUtils.formatNumber(
//                    number,
//                    Locale.getDefault().country
//                ) else
//                    ""
                val name = typeValue.name
                val lastName = typeValue.lastName
                val organization = typeValue.organization

                val info = StringBuilder("")

                pref.sort.forEachIndexed { index, id ->
                    var s = ""
                    when (id) {
                        0 ->
                            if (pref.name)
                                s = name
                        1 ->
                            if (pref.lastName)
                                s = lastName
                        2 ->
                            if (pref.phone)
                                s = number
                        3 ->
                            if (pref.organization)
                                s = organization
                    }
                    val notEmpty = s.isNotEmpty()
                    if (index > 0 && notEmpty && info.isNotEmpty())
                        info.append("\n")

                    if (notEmpty)
                        info.append(s)
                }
                displayValue = info.toString()
            }
            is PrefImage -> {
                val imageTypeValue = Gson().fromJson(sourceValue, ValueImage::class.java)
                val imageUriList = imageTypeValue.imagePathList
                displayValue =
                    if (imageUriList.isEmpty())
                        Uri.EMPTY.toString()
                    else {
                        val changeImage = imageTypeValue.changeImage
                        if (changeImage > -1)
                            imageUriList[changeImage]
                        else
                            imageUriList[0]
                    }

            }
            else -> {
                displayValue = sourceValue
            }
        }
    }

    fun displayCellView() {
        cellTypeControl.display(elementView, displayValue)
    }

    fun copy(): Cell {
        return Cell(sourceValue).also {
            it.type = type
            it.cellTypeControl = cellTypeControl
            it.displayValue = displayValue
        }
    }

    // опустошить
    fun clear() {
        sourceValue = when (type) {
            ColumnType.PHONE -> Gson().toJson(ValuePhone())
            ColumnType.COLOR -> Color.WHITE.toString()
            ColumnType.SWITCH -> false.toString()
            else -> {
                ""
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Cell)
            return false
        return sourceValue == other.sourceValue
    }

    override fun hashCode(): Int {
        var result = sourceValue.hashCode()
        result = 31 * result + currentBackground
        result = 31 * result + elementView.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + isSelect.hashCode()
        result = 31 * result + isPrefColumnSelect.hashCode()
        result = 31 * result + cellTypeControl.hashCode()
        result = 31 * result + displayValue.hashCode()
        return result
    }
}