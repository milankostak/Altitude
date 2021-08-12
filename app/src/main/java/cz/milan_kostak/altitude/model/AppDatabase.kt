package cz.milan_kostak.altitude.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LocationItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationItemDao(): LocationItemDao

}
