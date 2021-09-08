package ru.developer.press.myearningkot.helpers

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*

private fun coroutine(dispatcher: CoroutineDispatcher, run: suspend () -> Unit) =
    CoroutineScope(dispatcher).launch { run.invoke() }

fun runOnDefault(run: suspend () -> Unit) = coroutine(Dispatchers.Default) { run.invoke() }

fun runOnIO(run: suspend () -> Unit) = coroutine(Dispatchers.IO) { run.invoke() }

fun ViewModel.runOnViewModel(
    run: suspend () -> Unit
) = viewModelScope.launch(Dispatchers.IO) { run.invoke() }

fun LifecycleOwner.runOnLifeCycle(run: suspend () -> Unit) =
    lifecycleScope.launch(Dispatchers.IO) { run.invoke() }

fun LifecycleOwner.runMainOnLifeCycle(run: suspend () -> Unit) =
    lifecycleScope.launch(Dispatchers.Main) { run.invoke() }

suspend fun <T> main(block: suspend CoroutineScope.() -> T): T =
    withContext(Dispatchers.Main, block)

suspend fun <T> io(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO, block)

fun LifecycleOwner.postDelay(delay: Long, delayBlock: suspend () -> Unit) {
    runOnLifeCycle {
        delay(delay)
        main {
            delayBlock.invoke()
        }
    }
}

fun ViewModel.postDelay(delay: Long, delayBlock: suspend () -> Unit) {
    runOnViewModel {
        delay(delay)
        main {
            delayBlock.invoke()
        }
    }
}

fun View.postDelay(delay: Long, delayBlock: suspend () -> Unit) {
    findViewTreeLifecycleOwner()?.runOnLifeCycle {
        delay(delay)
        main {
            delayBlock.invoke()
        }
    }
}
