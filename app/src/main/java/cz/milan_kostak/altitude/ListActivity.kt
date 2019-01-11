package cz.milan_kostak.altitude

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.raizlabs.android.dbflow.sql.language.SQLite
import cz.milan_kostak.altitude.model.LocationItem

class ListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        val queryList = SQLite.select().from(LocationItem::class.java).queryList()
        println(queryList.size)
        viewManager = LinearLayoutManager(this)
        viewAdapter = ListAdapter(queryList)

        recyclerView = findViewById<RecyclerView>(R.id.cars_list).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }
    }

}