package cz.milan_kostak.altitude

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import cz.milan_kostak.altitude.model.LocationItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class ListAdapter(
        val data: MutableList<LocationItem>,
        val listActivity: ListActivity
) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    private val dateTimeFormat = SimpleDateFormat("dd. MM. yyyy", Locale.getDefault())
    private val coordinatesFormat = DecimalFormat("0.0°")
    private val altitudeFormat = DecimalFormat("0 m")

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnLongClickListener {

        val lbName: TextView = view.findViewById(R.id.lbName)
        val lbAltitude: TextView = view.findViewById(R.id.lbAltitude)
        val lbDate: TextView = view.findViewById(R.id.lbDate)
        val lbCoordinates: TextView = view.findViewById(R.id.lbCoords)

        init {
            view.setOnClickListener(this)
            view.setOnLongClickListener(this)
        }

        override fun onClick(v: View?) {
            val intent = Intent()
            intent.putExtra("locationId", data[layoutPosition].id.toString())
            listActivity.setResult(RESULT_OK, intent)
            listActivity.finish()
        }

        override fun onLongClick(v: View?): Boolean {
            val builder = AlertDialog.Builder(itemView.context)
            builder.setTitle("Confirm delete")
            builder.setPositiveButton("Delete") { _, _ ->
                val item = data[layoutPosition]
                if (DbHelper.getItemById(item.id)?.delete() == true) {
                    data.remove(item)
                    notifyItemRemoved(layoutPosition)
                }
            }
            builder.setNegativeButton("Cancel", null)
            builder.show()

            return true // event is consumed and no further event handling is required
        }
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