package cz.milan_kostak.altitude

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.raizlabs.android.dbflow.config.FlowConfig
import com.raizlabs.android.dbflow.config.FlowManager
import cz.milan_kostak.altitude.model.LocationItem
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvAltitudeReal: TextView
    private lateinit var tvVerticalAccuracy: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvSpeedAccuracy: TextView
    private lateinit var tvBearing: TextView
    private lateinit var tvBearingAccuracy: TextView
    private lateinit var tvProvider: TextView
    private lateinit var tvSatellites: TextView

    private lateinit var locationManager: LocationManager
    private lateinit var listener: LocationListener

    private val dateTimeFormat = SimpleDateFormat("dd. MM. yyyy HH:mm:ss", Locale.getDefault())
    private val coordinatesFormat = DecimalFormat("0.00000000°")
    private val altitudeFormat = DecimalFormat("0.0 m")
    private val accuracyFormat = DecimalFormat("0 m")
    private val speedFormat = DecimalFormat("0.00 km/h")
    private val degreesFormat = DecimalFormat("0°")
    private val plainIntegerFormat = DecimalFormat("0")

    private var currentLocationItem = LocationItem()

    private val PERMISSIONS_REQUEST_LOCATION = 10
    private val LIST_ACTIVITY_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // This instantiates DBFlow
        FlowManager.init(FlowConfig.Builder(this).build())
        //FlowManager.getDatabase(DatabaseModel::class.java).reset(this)

        tvTime = findViewById(R.id.tvTime)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAccuracy = findViewById(R.id.tvAccuracy)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvAltitudeReal = findViewById(R.id.tvAltitudeReal)
        tvVerticalAccuracy = findViewById(R.id.tvVerticalAccuracy)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvSpeedAccuracy = findViewById(R.id.tvSpeedAccuracy)
        tvBearing = findViewById(R.id.tvBearing)
        tvBearingAccuracy = findViewById(R.id.tvBearingAccuracy)
        tvProvider = findViewById(R.id.tvProvider)
        tvSatellites = findViewById(R.id.tvSatellites)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener { updatePositionButtonHandler() }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                setLocation(location)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}

            override fun onProviderEnabled(s: String) {}

            override fun onProviderDisabled(s: String) {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_save -> {
            // is set but not saved
            if (currentLocationItem.set && !currentLocationItem.saved) {
                if (currentLocationItem.save()) {
                    Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Not saved!", Toast.LENGTH_SHORT).show()
                }
            }

            true
        }

        R.id.action_show -> {
            val intent = Intent(this, ListActivity::class.java)
            startActivityForResult(intent, LIST_ACTIVITY_CODE)
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_ACTIVITY_CODE) {
            if (resultCode == RESULT_OK) {
                val locationId = data!!.getStringExtra("locationId").toInt()
                val locationItem = DbHelper.getItemById(locationId)
                currentLocationItem = locationItem!!
                currentLocationItem.saved = true
                currentLocationItem.set = true
                setLocationToWindow()
            }
        }
    }

    private fun setLocation(location: Location) {
        currentLocationItem = LocationItem()
        currentLocationItem.set = true
        currentLocationItem.saved = false

        currentLocationItem.name = "temp"

        currentLocationItem.time = location.time
        currentLocationItem.latitude = location.latitude
        currentLocationItem.longitude = location.longitude

        if (location.hasAccuracy()) {
            currentLocationItem.accuracy = location.accuracy
        } else {
            currentLocationItem.accuracy = -1f
        }

        // altitude in meters above the WGS 84 reference ellipsoid.
        if (location.hasAltitude()) {
            currentLocationItem.altitude = location.altitude
        } else {
            currentLocationItem.altitude = -10000.0
        }
        currentLocationItem.altitudeReal = -10000.0

        if (location.hasVerticalAccuracy()) {
            currentLocationItem.verticalAccuracy = location.verticalAccuracyMeters
        } else {
            currentLocationItem.verticalAccuracy = -1f
        }

        // speed in m/s
        if (location.hasSpeed()) {
            currentLocationItem.speed = location.speed * 3.6f
        } else {
            currentLocationItem.speed = -1f
        }
        if (location.hasSpeedAccuracy()) {
            currentLocationItem.speedAccuracy = location.speedAccuracyMetersPerSecond * 3.6f
        } else {
            currentLocationItem.speedAccuracy = -1f
        }

        if (location.hasBearing()) {
            currentLocationItem.bearing = location.bearing
        } else {
            currentLocationItem.bearing = -1f
        }
        if (location.hasBearingAccuracy()) {
            currentLocationItem.bearingAccuracy = location.bearingAccuracyDegrees
        } else {
            currentLocationItem.bearingAccuracy = -1f
        }

        currentLocationItem.provider = location.provider
        currentLocationItem.satellites = -1
        if (location.extras.containsKey("satellites")) {
            val satellitesObject = location.extras.get("satellites")
            if (satellitesObject is Int) {
                tvSatellites.text = plainIntegerFormat.format(satellitesObject)
            }
        }

        setLocationToWindow()
        getRealAltitude(location)
    }

    private fun setLocationToWindow() {
        tvTime.text = dateTimeFormat.format(Date(currentLocationItem.time))

        tvLatitude.text = coordinatesFormat.format(currentLocationItem.latitude)
        tvLongitude.text = coordinatesFormat.format(currentLocationItem.longitude)

        if (currentLocationItem.hasAccuracy()) {
            tvAccuracy.text = accuracyFormat.format(currentLocationItem.accuracy)
        } else {
            tvAccuracy.text = "-"
        }

        if (currentLocationItem.hasAltitude()) {
            tvAltitude.text = altitudeFormat.format(currentLocationItem.altitude)
        } else {
            tvAltitude.text = "-"
        }
        if (currentLocationItem.hasAltitudeReal()) {
            tvAltitudeReal.text = altitudeFormat.format(currentLocationItem.altitudeReal)
        } else {
            tvAltitudeReal.text = "-"
        }
        if (currentLocationItem.hasVerticalAccuracy()) {
            tvVerticalAccuracy.text = accuracyFormat.format(currentLocationItem.verticalAccuracy)
        } else {
            tvVerticalAccuracy.text = "-"
        }

        if (currentLocationItem.hasSpeed()) {
            tvSpeed.text = speedFormat.format(currentLocationItem.speed)
        } else {
            tvSpeed.text = "-"
        }
        if (currentLocationItem.hasSpeedAccuracy()) {
            tvSpeedAccuracy.text = speedFormat.format(currentLocationItem.speedAccuracy)
        } else {
            tvSpeedAccuracy.text = "-"
        }

        if (currentLocationItem.hasBearing()) {
            tvBearing.text = degreesFormat.format(currentLocationItem.bearing)
        } else {
            tvBearing.text = "-"
        }
        if (currentLocationItem.hasBearingAccuracy()) {
            tvBearingAccuracy.text = degreesFormat.format(currentLocationItem.bearingAccuracy)
        } else {
            tvBearingAccuracy.text = "-"
        }

        if (currentLocationItem.hasProvider()) {
            tvProvider.text = currentLocationItem.provider
        } else {
            tvProvider.text = "-"
        }

        if (currentLocationItem.hasSatellites()) {
            tvSatellites.text = plainIntegerFormat.format(currentLocationItem.satellites)
        } else {
            tvSatellites.text = "-"
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestPositionUpdate() {
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
    }

    private fun updatePositionButtonHandler() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET), PERMISSIONS_REQUEST_LOCATION)
        } else {
            requestPositionUpdate()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                // if request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestPositionUpdate()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    /**
     * Get and set real altitude converted from WGS 84 reference ellipsoid to WGS 84 geoid.
     */
    private fun getRealAltitude(location: Location) {
        try {
            val altitude = RetrieveAltitudeTask().execute(location.latitude.toString(), location.longitude.toString()).get()
            if (altitude != null) {
                val altitudeReal = location.altitude - altitude
                currentLocationItem.altitudeReal = altitudeReal
                tvAltitudeReal.text = altitudeFormat.format(altitudeReal)
            } else {
                currentLocationItem.altitudeReal = -10000.0
                tvAltitudeReal.text = "-"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


internal class RetrieveAltitudeTask : AsyncTask<String, Void, Float>() {

    override fun doInBackground(vararg params: String): Float? {
        var urlConnection: HttpsURLConnection? = null
        try {
            val url = URL("https://geographiclib.sourceforge.io/cgi-bin/GeoidEval?input=" + params[0] + "+" + params[1])
            urlConnection = url.openConnection() as HttpsURLConnection
            urlConnection.connect()
            val inputStream = BufferedInputStream(urlConnection.inputStream)
            val result = BufferedReader(InputStreamReader(inputStream)).lines().collect(Collectors.joining(""))

            val pattern = "EGM2008</a> = <font color=\"blue\">(.+?)</font>"
            val r = Pattern.compile(pattern)

            val matcher = r.matcher(result)
            if (matcher.find()) {
                val altitudeString = matcher.group(1)
                return java.lang.Float.parseFloat(altitudeString)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
        }
        return null
    }

    override fun onPostExecute(altitude: Float?) {

    }
}
