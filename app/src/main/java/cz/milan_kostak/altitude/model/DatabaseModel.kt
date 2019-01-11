package cz.milan_kostak.altitude.model

import com.raizlabs.android.dbflow.annotation.Database

@Database(name = DatabaseModel.NAME, version = DatabaseModel.VERSION)
object DatabaseModel {

    const val NAME = "AltitudeDatabase"

    const val VERSION = 1

}
