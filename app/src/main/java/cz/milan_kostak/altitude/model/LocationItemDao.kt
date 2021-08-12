package cz.milan_kostak.altitude.model

import androidx.room.*

@Dao
interface LocationItemDao {

    @Query("SELECT * FROM location_item")
    fun getAll(): List<LocationItem>

    @Query("SELECT * FROM location_item WHERE id = :locationId")
    fun getItemById(locationId: Int): LocationItem

    @Insert
    fun insert(locationItem: LocationItem): Long

    @Insert
    fun insert(locationItems: List<LocationItem>): List<Long>

    @Update
    fun update(locationItem: LocationItem): Int

    @Update
    fun update(locationItems: List<LocationItem>): Int

    @Delete
    fun delete(locationItem: LocationItem): Int

}