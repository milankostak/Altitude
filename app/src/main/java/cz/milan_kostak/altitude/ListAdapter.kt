package cz.milan_kostak.altitude

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cz.milan_kostak.altitude.model.LocationItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

class ListAdapter(private val data: MutableList<LocationItem>) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

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
            viewHolder.lbAltitude.text = altitudeFormat.format(data[position].altitude)
        }
        viewHolder.lbDate.text = dateTimeFormat.format(data[position].time)
        viewHolder.lbCoordinates.text = coordinatesFormat.format(data[position].latitude) + "  " + coordinatesFormat.format(data[position].longitude)
    }

    override fun getItemCount() = data.size
}