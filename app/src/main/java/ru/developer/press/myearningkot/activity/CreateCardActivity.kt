package ru.developer.press.myearningkot.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_card.*
import kotlinx.android.synthetic.main.create_card_activity.*
import kotlinx.android.synthetic.main.create_card_activity.recycler
import kotlinx.android.synthetic.main.create_card_activity.toolbar
import org.jetbrains.anko.toast
import ru.developer.press.myearningkot.App.Companion.dao
import ru.developer.press.myearningkot.R
import ru.developer.press.myearningkot.helpers.getColorFromRes
import ru.developer.press.myearningkot.helpers.main
import ru.developer.press.myearningkot.helpers.runOnLifeCycle
import ru.developer.press.myearningkot.viewmodels.CreateCardViewModel

class CreateCardActivity : AppCompatActivity() {

    companion object {
        const val createCardID: String = "createCardID"
        const val createCardName: String = "createCardName"
    }

    private lateinit var adapter: CreateCardViewModel.AdapterForSamples
    private lateinit var viewModel: CreateCardViewModel
    val editSampleRegister: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (data != null) {
                val id = data.getStringExtra(CARD_ID) ?: ""
                if (id.isNotEmpty()) {
                    runOnLifeCycle {
                        viewModel.updateSamples {
                            adapter.updateItem(id)
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_card_activity)

        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(getColorFromRes(R.color.colorOnPrimary))

        runOnLifeCycle {

            viewModel = ViewModelProvider(
                this@CreateCardActivity,
                ViewModelProvider.NewInstanceFactory()
            ).get(CreateCardViewModel::class.java).apply {
                sampleList = dao.getSampleList().toMutableList()
            }
            adapter = viewModel.getAdapter()
            main {
                recycler.layoutManager = LinearLayoutManager(this@CreateCardActivity)
                recycler.adapter = adapter
            }

            create.setOnClickListener {
                val selectId = adapter.selectId
                if (selectId == null) {
                    toast(getString(R.string.select_sample))
                } else {

                    setResult(RESULT_OK, Intent().apply {
                        putExtra(createCardID, selectId)
                        putExtra(createCardName, sampleEditTextName.text.toString())
                    })
                    finish()
                }
            }
            cancel.setOnClickListener {
                finish()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.create_card_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.new_sample -> {

            }
        }
        return true
    }
}