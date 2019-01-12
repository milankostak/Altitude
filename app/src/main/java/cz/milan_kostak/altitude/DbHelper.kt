package cz.milan_kostak.altitude

import com.raizlabs.android.dbflow.sql.language.SQLite
import cz.milan_kostak.altitude.model.LocationItem
import cz.milan_kostak.altitude.model.LocationItem_Table

object DbHelper {

    @JvmStatic
    fun getItemById(locationId: Int): LocationItem? {
        return SQLite.select().from(LocationItem::class.java).where(LocationItem_Table.id.eq(locationId)).querySingle()
    }

    @JvmStatic
    fun getAllItems(): MutableList<LocationItem> {
        return SQLite.select().from(LocationItem::class.java).queryList()
    }
}