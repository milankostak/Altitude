package cz.milan_kostak.altitude

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.milan_kostak.altitude.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.stream.Collectors

class ListActivity : AppCompatActivity() {

    private lateinit var viewAdapter: ListAdapter

    private lateinit var locationItemDao: LocationItemDao

    private lateinit var backupLauncher: ActivityResultLauncher<Intent>
    private lateinit var importLauncher: ActivityResultLauncher<Intent>

    private var currentSort = SortType.TIME
    private var ascending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            Constants.NAME
        ).build()
        locationItemDao = db.locationItemDao()

        val sortId = getPreferences(Context.MODE_PRIVATE).getInt(Constants.SORT_PREFERENCE_KEY, SortType.TIME.id)
        currentSort = SortType.getById(sortId)

        val ascendingVal = getPreferences(Context.MODE_PRIVATE).getInt(Constants.SORT_ASCENDING_KEY, 1)
        ascending = (ascendingVal == 1)

        val viewManager = LinearLayoutManager(this)
        viewAdapter = ListAdapter(this, locationItemDao)

        findViewById<RecyclerView>(R.id.locations_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        val rbByTime: RadioButton = findViewById(R.id.sort_time)
        val rbByName: RadioButton = findViewById(R.id.sort_name)
        val rbByAltitude: RadioButton = findViewById(R.id.sort_altitude)

        when (currentSort) {
            SortType.TIME -> rbByTime.isChecked = true
            SortType.NAME -> rbByName.isChecked = true
            SortType.ALTITUDE -> rbByAltitude.isChecked = true
        }

        rbByTime.setOnCheckedChangeListener { _, checked -> if (checked) changeSort(SortType.TIME) }
        rbByName.setOnCheckedChangeListener { _, checked -> if (checked) changeSort(SortType.NAME) }
        rbByAltitude.setOnCheckedChangeListener { _, checked -> if (checked) changeSort(SortType.ALTITUDE) }

        CoroutineScope(Dispatchers.IO).launch {
            val newData = locationItemDao.getAll()
            runOnUiThread {
                viewAdapter.setItems(newData)
                updateData()
            }
        }

        backupLauncher = getBackupLauncher()
        importLauncher = getImportLauncher()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        (menu.findItem(R.id.action_ascending).actionView as AppCompatCheckBox).isChecked = ascending
        return true
    }

    private fun changeSort(sortType: SortType) {
        currentSort = sortType
        updateData()
    }

    fun changeOrder(view: View) {
        val cbAscending: CheckBox = view.findViewById(R.id.action_item_checkbox)
        ascending = cbAscending.isChecked
        updateData()
    }

    private fun updateData() {
        val pref = getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putInt(Constants.SORT_PREFERENCE_KEY, currentSort.id)
            putInt(Constants.SORT_ASCENDING_KEY, if (ascending) 1 else 0)
            apply()
        }
        viewAdapter.sortItems(currentSort, ascending)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_backup -> {
            backup()
            true
        }
        R.id.action_import -> {
            import()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun backup() {
        val fileName = "altitude_backup_${Date().time}.json"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        backupLauncher.launch(intent)
    }

    private fun getBackupLauncher(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it?.data?.data?.let { uri ->
                CoroutineScope(Dispatchers.IO).launch {
                    val json = Gson().toJson(locationItemDao.getAll())
                    runOnUiThread {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(json.toByteArray())
                        }
                    }
                }
            }
        }
    }

    private fun import() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Filter to only show results that can be "opened",
            // such as a file (as opposed to a list of contacts or timezones)
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        importLauncher.launch(intent)
    }

    private fun getImportLauncher(): ActivityResultLauncher<Intent> {
        return registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            it?.data?.data?.let { uri ->
                // load data from JSON
                val json = readTextFromUri(uri)
                val listType = object : TypeToken<List<LocationItem>>() {}.type
                val locations = Gson().fromJson<List<LocationItem>>(json, listType)
                Toast.makeText(this, "Imported ${locations.size} items", Toast.LENGTH_SHORT).show()

                CoroutineScope(Dispatchers.IO).launch {
                    locationItemDao.insert(locations)
                    val allItems = locationItemDao.getAll()

                    runOnUiThread {
                        viewAdapter.setItems(allItems)
                        updateData()
                    }
                }
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                return reader.lines().collect(Collectors.joining())
            }
        }
        return ""
    }

}