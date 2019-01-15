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

        viewHolder.itemView.setOnClickListener {
            val intent = Intent()
            intent.putExtra("locationId", data[position].id.toString())
            listActivity.setResult(RESULT_OK, intent)
            listActivity.finish()
        }
        viewHolder.itemView.setOnLongClickListener {
            val builder = AlertDialog.Builder(viewHolder.itemView.context)
            builder.setTitle("Confirm delete")
            println("$position ${data[position].name}")
            builder.setPositiveButton("Delete") { _, _ ->
                val item = data[position]
                if (DbHelper.getItemById(item.id)?.delete()!!) {
                    data.remove(item)
                    notifyItemRemoved(position)
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()

            true // event is consumed and no further event handling is required
        }
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