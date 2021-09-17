package ru.developer.press.myearningkot

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bugsnag.android.Bugsnag
import com.google.firebase.auth.FirebaseAuth
import ru.developer.press.myearningkot.database.DataController
import ru.developer.press.myearningkot.database.UpdatedRefData
import ru.developer.press.myearningkot.helpers.*
import ru.developer.press.myearningkot.model.*

class App : Application(), ActivityLifecycleCallbacks {
    companion object {
        lateinit var dao: DataController
        lateinit var authUser: FirebaseAuth
        var fireStoreChanged = liveData<UpdatedRefData>()
    }

    // от сюда берется только его значение
    var copyCell: Cell? = null

    var copyRowList: List<Row>? = null
        get() {
            return field?.fold(mutableListOf()) { list, row ->
                list.add(row.copy().apply {
                    status = Status.ADDED
                })
                list
            }
        }

    private var currentActivity: AppCompatActivity? = null
    private val pref: SharedPreferences
        get() = getSharedPreferences("app.setting", Context.MODE_PRIVATE)

    override fun onCreate() {
        dao = DataController(applicationContext)
//        authUser = Firebase.auth

        runOnIO {
            registerActivityLifecycleCallbacks(this@App)
            filesFolder = filesDir.path + "/"
            Column.titleColor = colorRes(R.color.textColorTabsTitleNormal)
            NumerationColumn.color = colorRes(R.color.textColorSecondary)
            PrefForCard.nameColor = colorRes(R.color.colorTitle)

            PhoneColumn.apply {
                nameOfMan = getString(R.string.name_man)
                lastName = getString(R.string.last_name)
                phone = getString(R.string.phone)
                organization = getString(R.string.organization)
            }

            Bugsnag.init(applicationContext)
            val isFirst = pref.getBoolean(prefFirstKey, true)
            if (isFirst) {
                dao.createDefaultSamplesJob(applicationContext)
                pref.edit().putBoolean(prefFirstKey, false).apply()
            }
//            dao.syncRefs()
            super.onCreate()
        }
    }

    override fun onActivityPaused(p0: Activity) {

    }

    override fun onActivityStarted(p0: Activity) {
    }

    override fun onActivityDestroyed(p0: Activity) {
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityStopped(p0: Activity) {
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity is AppCompatActivity)
            currentActivity = activity
    }
}

const val prefFirstKey = "isFirst"
//const val prefSampleLastChanged = "sampleLastChanged"
//const val prefEnableDate = "enableDate"
//const val prefSortSelected = "sortSelected"
//
//const val prefEnableSomeStrokeChanged = "enableSomeStrokeChanged"
//const val prefDateLastChanged = "dateLastChanged"
