package cz.milan_kostak.altitude

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioButton
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.milan_kostak.altitude.model.LocationItem
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*


class ListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ListAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var rbByTime: RadioButton
    private lateinit var rbByName: RadioButton
    private lateinit var rbByAltitude: RadioButton

    private val READ_REQUEST_CODE: Int = 42
    private val WRITE_REQUEST_CODE: Int = 43

    private val SORT_PREFERENCE_KEY = "currentSort"
    private var currentSort = SortType.TIME

    enum class SortType(val id: kotlin.Int) {
        TIME(1), NAME(2), ALTITUDE(3);

        companion object {
            fun getById(newId: Int): SortType {
                return SortType.values().single { it.id == newId }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val sortId = getPreferences(Context.MODE_PRIVATE).getInt(SORT_PREFERENCE_KEY, SortType.TIME.id)
        currentSort = SortType.getById(sortId)

        val queryList = DbHelper.getAllItems(currentSort)

        viewManager = LinearLayoutManager(this)
        viewAdapter = ListAdapter(queryList, this)

        recyclerView = findViewById<RecyclerView>(R.id.locations_list).apply {
            setHasFixedSize(true)

            layoutManager = viewManager

            adapter = viewAdapter
        }

        rbByTime = findViewById(R.id.sort_time)
        rbByTime.isChecked = true
        rbByName = findViewById(R.id.sort_name)
        rbByAltitude = findViewById(R.id.sort_altitude)

        when (currentSort) {
            SortType.TIME -> rbByTime.isChecked = true
            SortType.NAME -> rbByName.isChecked = true
            SortType.ALTITUDE -> rbByAltitude.isChecked = true
        }

        rbByTime.setOnCheckedChangeListener { _, checked -> if (checked) updateData(SortType.TIME) }
        rbByName.setOnCheckedChangeListener { _, checked -> if (checked) updateData(SortType.NAME) }
        rbByAltitude.setOnCheckedChangeListener { _, checked -> if (checked) updateData(SortType.ALTITUDE) }
    }

    private fun updateData(sortType: SortType) {
        currentSort = sortType
        val pref = getPreferences(Context.MODE_PRIVATE)
        with(pref.edit()) {
            putInt(SORT_PREFERENCE_KEY, currentSort.id)
            apply()
        }
        val newData = DbHelper.getAllItems(sortType)
        viewAdapter.updateData(newData)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_backup -> {
            createFile()
            true
        }
        R.id.action_import -> {
            import()
            true
        }
        16908332 -> { // R.id.home nebere - nechápu, nechci řešit
            finish()
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun createFile() {
        val fileName = "altitude_backup_" + Date().time + ".json"
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    private fun import() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as a file (as opposed to a list of contacts or timezones)
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/octet-stream"
        }
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                // load data from JSON
                val json = readTextFromUri(uri)
                val listType = object : TypeToken<List<LocationItem>>() {}.type
                val locations = Gson().fromJson<List<LocationItem>>(json, listType)
                DbHelper.import(locations)
                Toast.makeText(this, "Imported ${locations.size} items", Toast.LENGTH_SHORT).show()

                // load imported locations from DB with correct sort
                val newData = DbHelper.getAllItems(currentSort)
                viewAdapter.updateAfterImport(newData)
            }
        } else if (requestCode == WRITE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                val json = Gson().toJson(DbHelper.getAllItems())
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

}