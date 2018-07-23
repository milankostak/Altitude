package cz.milan_kostak.altitude

import android.Manifest
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
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
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
    private lateinit var tvProvider: TextView
    private lateinit var tvSatellites: TextView

    private lateinit var locationManager: LocationManager
    private lateinit var listener: LocationListener

    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
    private val coordinatesFormat = DecimalFormat("0.00000000°")
    private val altitudeFormat = DecimalFormat("0.0 m")
    private val accuracyFormat = DecimalFormat("0 m")
    private val speedFormat = DecimalFormat("0.00 km/h")
    private val plainIntegerFormat = DecimalFormat("0")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTime = findViewById(R.id.tvTime)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAccuracy = findViewById(R.id.tvAccuracy)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvAltitudeReal = findViewById(R.id.tvAltitudeReal)
        tvVerticalAccuracy = findViewById(R.id.tvVerticalAccuracy)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvSpeedAccuracy = findViewById(R.id.tvSpeedAccuracy)
        tvProvider = findViewById(R.id.tvProvider)
        tvSatellites = findViewById(R.id.tvSatellites)

        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener { updatePosition() }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                setLocationToWindow(location)
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
            // User chose the "Settings" item, show the app settings UI...
            true
        }

        R.id.action_show -> {
            true
        }

        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun setLocationToWindow(location: Location) {
        val dateTimeString = dateTimeFormat.format(Date(location.time))
        tvTime.text = dateTimeString

        tvLatitude.text = coordinatesFormat.format(location.latitude)
        tvLongitude.text = coordinatesFormat.format(location.longitude)
        if (location.hasAccuracy()) {
            tvAccuracy.text = accuracyFormat.format(location.accuracy)
        } else {
            tvAccuracy.text = "-"
        }

        if (location.hasAltitude()) {
            tvAltitude.text = altitudeFormat.format(location.altitude)
        } else {
            tvAltitude.text = "-"
        }
        if (location.hasVerticalAccuracy()) {
            tvVerticalAccuracy.text = accuracyFormat.format(location.verticalAccuracyMeters)
        } else {
            tvVerticalAccuracy.text = "-"
        }

        if (location.hasSpeed()) {
            tvSpeed.text = speedFormat.format(location.speed * 3.6)
        } else {
            tvSpeed.text = "-"
        }
        if (location.hasSpeedAccuracy()) {
            tvSpeedAccuracy.text = speedFormat.format(location.speedAccuracyMetersPerSecond * 3.6)
        } else {
            tvSpeedAccuracy.text = "-"
        }

        tvProvider.text = location.provider
        if (location.extras.containsKey("satellites")) {
            val satellitesObject = location.extras.get("satellites")
            if (satellitesObject is Int) {
                tvSatellites.text = plainIntegerFormat.format(satellitesObject)
            }
        }

        getRealAltitude(location)
    }

    private fun updatePosition() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET), 10)
        } else {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
        }
    }

    private fun getRealAltitude(location: Location) {
        try {
            val altitude = RetrieveAltitudeTask().execute(location.latitude.toString(), location.longitude.toString()).get()
            val altitudeReal = location.altitude - altitude
            tvAltitudeReal.text = altitudeFormat.format(altitudeReal)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        } catch (e: ExecutionException) {
            e.printStackTrace()
        }

    }
}


internal class RetrieveAltitudeTask : AsyncTask<String, Void, Float>() {

    override fun doInBackground(vararg params: String): Float {
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
        return 0f
    }

    override fun onPostExecute(altitude: Float?) {

    }
}
