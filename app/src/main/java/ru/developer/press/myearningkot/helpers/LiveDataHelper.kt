package ru.developer.press.myearningkot.helpers

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

abstract class MyLiveData<T>(t: T?) : MutableLiveData<T>() {
    init {
        t?.let {
            value = it
        }
    }

    fun postUpdate() {
        postValue(value)
    }

    suspend fun setVal(value: T) = main {
        setValue(value)
    }
}

fun <T> liveData(t: T? = null): MyLiveData<T> {
    return object : MyLiveData<T>(t) {}
}

/**liveData с единственным обсерверром
 * применяется например для recyclerView
 * поможет избежать утечки обсерверов при notifyData
 * */
class SingleObserverLiveData<T>(t: T?) : MyLiveData<T>(t) {
    private var oldObserver: Observer<in T>? = null
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        oldObserver?.let { removeObserver(it) }
        oldObserver = observer
        super.observe(owner, observer)
    }
}

suspend fun <T> liveDataFromMain(t: T? = null): MyLiveData<T> {
    return main { object : MyLiveData<T>(t) {} }
}

fun <T> observer(changed: (T) -> Unit): Observer<T> {
    return object : Observer<T> {
        override fun onChanged(t: T?) {
            t?.let { changed.invoke(it) }
        }
    }
}

fun <T> singleCallObserver(changed: (T) -> Unit): Observer<T> {
    return object : Observer<T> {
        private var isFirst = true
        private var oldValue: T? = null
        override fun onChanged(t: T?) {
            if (isFirst) {
                isFirst = false
                return
            }
            if (oldValue === t)
                return
            t?.let { changed.invoke(it) }
            oldValue = t
        }

    }
}

class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val mPending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {

        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MyLiveData
        super.observe(owner, { t ->
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(t)
            }
        })
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call(id: T) {
        value = id
    }

    fun call() {
        value = null
    }

    companion object {

        private const val TAG = "SingleLiveEvent"
    }
}