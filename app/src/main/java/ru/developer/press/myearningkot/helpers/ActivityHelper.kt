package ru.developer.press.myearningkot.helpers

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import ru.developer.press.myearningkot.App

fun Activity.app(): App {
    return application as App
}

class ActivityResultHelper(
    caller: ActivityResultCaller,
    callback: (ActivityResult) -> Unit
) {
    private val result: ActivityResultLauncher<Intent> =
        caller.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            callback.invoke(it)
        }

    fun launch(intent: Intent) {
        result.launch(intent)
    }

}