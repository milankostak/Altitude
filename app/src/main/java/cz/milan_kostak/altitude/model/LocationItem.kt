package cz.milan_kostak.altitude.model

import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Column

@Table(database = DatabaseModel::class)

class LocationItem : BaseModel() {

    var saved = false
    var set = false

    @Column
    @PrimaryKey(autoincrement = true)
    var id: Int = 0

    @Column
    var name: String = ""

    @Column
    var time: Long = 0

    @Column
    var latitude: Double = .0

    @Column
    var longitude: Double = .0

    @Column
    var accuracy: Float = -1f
    fun hasAccuracy(): Boolean {
        return accuracy != -1f
    }

    // altitude in meters above the WGS 84 reference ellipsoid
    @Column
    var altitude: Double = -10000.0
    fun hasAltitude(): Boolean {
        return altitude != -10000.0
    }

    // altitude in meters above the WGS 84 reference geoid
    @Column
    var altitudeReal: Double = -10000.0
    fun hasAltitudeReal(): Boolean {
        return altitude != -10000.0
    }

    @Column
    var verticalAccuracy: Float = -1f
    fun hasVerticalAccuracy(): Boolean {
        return verticalAccuracy != -1f
    }

    // speed in km/h
    @Column
    var speed: Float = -1f
    fun hasSpeed(): Boolean {
        return speed != -1f
    }

    @Column
    var speedAccuracy: Float = -1f
    fun hasSpeedAccuracy(): Boolean {
        return speedAccuracy != -1f
    }

    // value in range <0;360>
    @Column
    var bearing: Float = -1f
    fun hasBearing(): Boolean {
        return bearing != -1f
    }

    @Column
    var bearingAccuracy: Float = -1f
    fun hasBearingAccuracy(): Boolean {
        return bearingAccuracy != -1f
    }

    @Column
    var provider: String = ""
    fun hasProvider(): Boolean {
        return !provider.isEmpty()
    }

    @Column
    var satellites: Int = -1
    fun hasSatellites(): Boolean {
        return satellites != -1
    }

}