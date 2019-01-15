package cz.milan_kostak.altitude

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cz.milan_kostak.altitude.model.LocationItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ListAdapter(
        private val data: MutableList<LocationItem>,
        private val listActivity: ListActivity
) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    private val dateTimeFormat = SimpleDateFormat("dd. MM. yyyy", Locale.getDefault())
    private val coordinatesFormat = DecimalFormat("0.0°")
    private val altitudeFormat = DecimalFormat("0 m")

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val lbName: TextView = view.findViewById(R.id.lbName) as TextView
        val lbAltitude: TextView = view.findViewById(R.id.lbAltitude) as TextView
        val lbDate: TextView = view.findViewById(R.id.lbDate) as TextView
        val lbCoordinates: TextView = view.findViewById(R.id.lbCoords) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        val recyclerView = parent.findViewById<RecyclerView>(R.id.locations_list)
        view.setOnClickListener {
            val itemPosition = recyclerView.indexOfChild(view)
            val item = data[itemPosition]

            val intent = Intent()
            intent.putExtra("locationId", item.id.toString())
            listActivity.setResult(RESULT_OK, intent)
            listActivity.finish()
        }
        view.setOnLongClickListener {
            val builder = AlertDialog.Builder(parent.context)
            builder.setTitle("Confirm delete")
            builder.setPositiveButton("Delete") { _, _ ->
                val itemPosition = recyclerView.indexOfChild(view)
                val item = data[itemPosition]
                if (DbHelper.getItemById(item.id)?.delete()!!) {
                    data.remove(item)
                    notifyItemRemoved(itemPosition)
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()

            true // event is consumed and no further event handling is required
        }
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.lbName.text = data[position].name
        if (data[position].hasAltitudeReal()) {
            viewHolder.lbAltitude.text = altitudeFormat.format(data[position].altitudeReal)
        } else {
            viewHolder.lbAltitude.text = "~" + altitudeFormat.format(data[position].altitude)
        }
        viewHolder.lbDate.text = dateTimeFormat.format(data[position].time)
        viewHolder.lbCoordinates.text = coordinatesFormat.format(data[position].latitude) + "  " + coordinatesFormat.format(data[position].longitude)
    }

    override fun getItemCount() = data.size

    fun updateData(newData: MutableList<LocationItem>) {
        val oldIndices: MutableList<Int> = ArrayList()
        val shifts: MutableList<Int> = ArrayList()

        for (i in newData.indices) {
            for (j in data.indices) {
                if (data[j].id == newData[i].id) {
                    oldIndices.add(j)
                    break
                }
            }
            shifts.add(0)
        }

        for (i in 0 until oldIndices.size) {
            val newPosition = i
            var oldPosition = oldIndices[i]
            oldPosition += shifts[oldPosition]

            if (oldPosition == newPosition) continue

            notifyItemMoved(oldPosition, newPosition)

            for (j in 0 until oldIndices[i]) {
                shifts[j]++
            }
        }

        data.clear()
        data.addAll(newData)
    }

    fun updateAfterImport(newData: List<LocationItem>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

}