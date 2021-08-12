package cz.milan_kostak.altitude.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_item")
class LocationItem {

    @Transient
    var saved = false
    @Transient
    var set = false

    @Transient
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @ColumnInfo(name = "name")
    var name: String = ""

    fun hasName(): Boolean {
        return name.isNotEmpty()
    }

    @ColumnInfo(name = "time")
    var time: Long = 0

    @ColumnInfo(name = "latitude")
    var latitude: Double = .0

    @ColumnInfo(name = "longitude")
    var longitude: Double = .0

    @ColumnInfo(name = "accuracy")
    var accuracy: Float = -1f

    fun hasAccuracy(): Boolean {
        return accuracy != -1f
    }

    // altitude in meters above the WGS 84 reference ellipsoid
    @ColumnInfo(name = "altitude")
    var altitude: Double = -10_000.0

    fun hasAltitude(): Boolean {
        return altitude != -10_000.0
    }

    // altitude in meters above the EGM 2008 geoid
    @ColumnInfo(name = "altitudeReal")
    var altitudeReal: Double = -10_000.0

    fun hasAltitudeReal(): Boolean {
        return altitudeReal != -10_000.0
    }

    @ColumnInfo(name = "verticalAccuracy")
    var verticalAccuracy: Float = -1f

    fun hasVerticalAccuracy(): Boolean {
        return verticalAccuracy != -1f
    }

    // speed in km/h
    @ColumnInfo(name = "speed")
    var speed: Float = -1f

    fun hasSpeed(): Boolean {
        return speed != -1f
    }

    @ColumnInfo(name = "speedAccuracy")
    var speedAccuracy: Float = -1f

    fun hasSpeedAccuracy(): Boolean {
        return speedAccuracy != -1f
    }

    // value in range <0;360>
    @ColumnInfo(name = "bearing")
    var bearing: Float = -1f

    fun hasBearing(): Boolean {
        return bearing != -1f
    }

    @ColumnInfo(name = "bearingAccuracy")
    var bearingAccuracy: Float = -1f

    fun hasBearingAccuracy(): Boolean {
        return bearingAccuracy != -1f
    }

    @ColumnInfo(name = "provider")
    var provider: String = ""

    fun hasProvider(): Boolean {
        return provider.isNotEmpty()
    }

    @ColumnInfo(name = "satellites")
    var satellites: Int = -1

    fun hasSatellites(): Boolean {
        return satellites != -1
    }

    override fun toString(): String {
        return name
    }

}