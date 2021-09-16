package ru.developer.press.myearningkot.dialogs

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.helpers.setAlertButtonColorsAfterShown

fun choiceDialog(init: AlertDialog.Builder.() -> Unit): ChoiceDialog {
    return ChoiceDialog().apply {
        setAlertConfig(init)
    }
}

class ChoiceDialog : DialogFragment() {
    var positiveButtonColorRes = R.color.colorAccent
    var negativeButtonColorRes = R.color.colorAccent

    private var alertInit: AlertDialog.Builder.() -> Unit = {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog: AlertDialog.Builder = AlertDialog.Builder(requireContext()).apply {
            alertInit.invoke(this)
        }

        val alertDialog = dialog.create()
        alertDialog.setAlertButtonColorsAfterShown(positiveButtonColorRes, negativeButtonColorRes)
        return alertDialog
    }

    override fun onResume() {
        super.onResume()
        context?.let {
            dialog?.window?.setBackgroundDrawable(
                ColorDrawable(
                    ContextCompat.getColor(
                        it,
                        R.color.colorDialogBackground
                    )
                )
            )
        }
    }

    fun setAlertConfig(init: AlertDialog.Builder.() -> Unit) {
        alertInit = init
    }
}


