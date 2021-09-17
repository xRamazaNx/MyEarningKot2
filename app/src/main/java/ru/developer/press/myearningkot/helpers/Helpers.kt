package ru.developer.press.myearningkot.helpers

import android.net.Uri
import android.util.Log
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.logD
import ru.developer.press.myearningkot.model.PrefNumber
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.roundToInt

fun getPathForResource(resourceId: Int): String {
    return Uri.parse("android.resource://" + R::class.java.getPackage()!!.name + "/" + resourceId)
        .toString()
}

fun getDecimalFormatNumber(
    value: Double,
    pref: PrefNumber = PrefNumber()
): String {
    val count = pref.digitsCount
    val groupNumber = pref.isGrouping
    val groupSize = pref.groupSize

    val format = StringBuilder("#")
    repeat(count) {
        if (it == 0)
            format.append('.')
        format.append('#')
    }
    val decimalFormat = DecimalFormat(
        format.toString(),
        DecimalFormatSymbols.getInstance(Locale.getDefault())
    ).apply {
        maximumFractionDigits = count
        roundingMode = RoundingMode.HALF_EVEN
        isGroupingUsed = groupNumber
        groupingSize = groupSize

    }

    return decimalFormat.format(value)
}

fun Any.log(message: String) {
    Log.d(this::class.qualifiedName, message)
}

inline fun <T> tryCatch(tryBlock: () -> T?, catchBlock: () -> T? = { null }): T? {
    return try {
        tryBlock.invoke()
    } catch (ex: Exception) {
        logD(ex.fillInStackTrace().toString())
        catchBlock.invoke()
    }
}

inline fun tryCatch(tryBlock: () -> Unit) {
    try {
        tryBlock.invoke()
    } catch (ex: Exception) {
        logD(ex.fillInStackTrace().toString())
    }
}

fun File.copyTo(file: File) {
    inputStream().use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

/**
 * распределить пропорционально addValue между changedValues и вернуть
 * */
fun sizeDistribution(
    addValue: Int,
    changedValues: List<Int>
): List<Int> {
    val sumOfUnselected = changedValues.sumOf { it }
    return changedValues.fold(mutableListOf()) { list, value ->
        val coef: Float = value / sumOfUnselected.toFloat()
        val sumOf = (sumOfUnselected + addValue)
        val v = sumOf * coef
        list.add(v.roundToInt())
        list
    }
}