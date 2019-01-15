package cz.milan_kostak.altitude

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioButton

class ListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: ListAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var rbByTime: RadioButton
    private lateinit var rbByName: RadioButton
    private lateinit var rbByAltitude: RadioButton

    enum class SortType {
        TIME, NAME, ALTITUDE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val queryList = DbHelper.getAllItems()

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

        rbByTime.setOnCheckedChangeListener { _, checked -> if (checked) updateData(SortType.TIME) }
        rbByName.setOnCheckedChangeListener { _, checked -> if (checked) updateData(SortType.NAME) }
        rbByAltitude.setOnCheckedChangeListener { _, checked -> if (checked) updateData(SortType.ALTITUDE) }
    }

    private fun updateData(sortType: SortType) {
        val newData = DbHelper.getAllItems(sortType)
        viewAdapter.updateData(newData)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_backup -> {
            // TODO
            true
        }
        R.id.action_import -> {
            // TODO
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

}