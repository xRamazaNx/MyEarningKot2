package ru.developer.press.myearningkot.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.jaredrummler.android.colorpicker.ColorPickerDialog
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener
import kotlinx.android.synthetic.main.card.view.*
import kotlinx.android.synthetic.main.total_item_layout.view.*
import kotlinx.android.synthetic.main.total_item_value.view.*
import org.jetbrains.anko.backgroundColorResource
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.helpers.setAlertButtonColors

fun <T : DialogFragment> T.addDismissListener(dismiss: (T) -> Unit) {
    lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroy() {
            dismiss.invoke(this@addDismissListener)
        }
    })
}

fun <T : DialogFragment> T.addShownListener(shown: (T) -> Unit) {
    lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun resume() {
            shown.invoke(this@addShownListener)
        }
    })
}

fun colorDialog(color: Int, colorChanged: (Int) -> Unit): ColorPickerDialog {
    return ColorPickerDialog
        .newBuilder()
        .setColor(color)
        .setShowAlphaSlider(false)
        .create().apply {
            setColorPickerDialogListener(
                object : ColorPickerDialogListener {
                    override fun onDialogDismissed(dialogId: Int) {

                    }

                    override fun onColorSelected(dialogId: Int, color: Int) {
                        colorChanged.invoke(color)
                    }

                })
        }.apply {
            addShownListener { dialogFragment ->
                dialogFragment.dialog?.let {
                    val alertDialog = it as AlertDialog
                    alertDialog.setAlertButtonColors()
                    alertDialog
                        .window
                        ?.decorView
                        ?.backgroundColorResource = R.color.colorDialogBackground

                }
            }
        }

}