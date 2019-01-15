package cz.milan_kostak.altitude

import com.raizlabs.android.dbflow.annotation.Collate
import com.raizlabs.android.dbflow.sql.language.From
import com.raizlabs.android.dbflow.sql.language.OrderBy
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
        return getAll().orderBy(LocationItem_Table.time, true).queryList()
    }

    @JvmStatic
    fun getAllItems(sortType: ListActivity.SortType): MutableList<LocationItem> {
        return when (sortType) {
            ListActivity.SortType.TIME -> DbHelper.getAllItems()
            ListActivity.SortType.NAME -> DbHelper.getAll().orderBy(OrderBy.fromProperty(LocationItem_Table.name).collate(Collate.NOCASE).ascending()).queryList()
            ListActivity.SortType.ALTITUDE -> DbHelper.getAll().orderBy(LocationItem_Table.altitudeReal, true).queryList()
        }
    }

    @JvmStatic
    private fun getAll(): From<LocationItem> {
        return SQLite.select().from(LocationItem::class.java)
    }

    @JvmStatic
    fun import(locations: List<LocationItem>?) {
        if (locations != null) {
            for (location in locations) {
                location.save()
            }
        }
    }
}