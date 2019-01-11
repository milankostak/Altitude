package cz.milan_kostak.altitude

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import cz.milan_kostak.altitude.model.LocationItem

class ListAdapter(private val data: MutableList<LocationItem>) : RecyclerView.Adapter<ListAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var latitude: TextView = view.findViewById(R.id.latitude) as TextView
        var name: TextView = view.findViewById(R.id.name) as TextView

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapter.ViewHolder {
        // create a new view
        val textView = LayoutInflater.from(parent.context).inflate(R.layout.item_layout, parent, false)
        // set the view's size, margins, paddings and layout parameters

        return ViewHolder(textView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.latitude.text = data[position].latitude.toString()
        viewHolder.name.text = data[position].name
    }

    override fun getItemCount() = data.size
}