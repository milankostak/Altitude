package cz.milan_kostak.altitude

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.room.Room
import cz.milan_kostak.altitude.model.AppDatabase
import cz.milan_kostak.altitude.model.Constants
import cz.milan_kostak.altitude.model.LocationItem
import cz.milan_kostak.altitude.model.LocationItemDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.URL
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity() {

    private lateinit var loadingIcon: ProgressBar
    private lateinit var btRequestPosition: Button

    private lateinit var tvTime: TextView
    private lateinit var tvLatitude: TextView
    private lateinit var tvLongitude: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvAltitudeReal: TextView
    private lateinit var loadingIconRealAlt: ProgressBar
    private lateinit var tvVerticalAccuracy: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvSpeedAccuracy: TextView
    private lateinit var tvBearing: TextView
    private lateinit var tvBearingAccuracy: TextView
    private lateinit var tvProvider: TextView
    private lateinit var tvSatellites: TextView

    private lateinit var locationManager: LocationManager
    private lateinit var listener: LocationListener

    private lateinit var locationItemDao: LocationItemDao

    private val dateTimeFormat = SimpleDateFormat("dd. MM. yyyy HH:mm:ss", Locale.getDefault())
    private val coordinatesFormat = DecimalFormat("0.00000000°")
    private val altitudeFormat = DecimalFormat("0.0 m")
    private val accuracyFormat = DecimalFormat("0 m")
    private val speedFormat = DecimalFormat("0.00 km/h")
    private val degreesFormat = DecimalFormat("0°")
    private val plainIntegerFormat = DecimalFormat("0")

    private var currentLocationItem = LocationItem()
    private var requestInProgress: Boolean = false

    private val PERMISSIONS_REQUEST_LOCATION = 10
    private val LIST_ACTIVITY_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            Constants.NAME
        ).build()
        locationItemDao = db.locationItemDao()

        loadingIcon = findViewById(R.id.loadingIcon)
        tvTime = findViewById(R.id.tvTime)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAccuracy = findViewById(R.id.tvAccuracy)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvAltitudeReal = findViewById(R.id.tvAltitudeReal)
        loadingIconRealAlt = findViewById(R.id.loadingIconRealAlt)
        tvVerticalAccuracy = findViewById(R.id.tvVerticalAccuracy)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvSpeedAccuracy = findViewById(R.id.tvSpeedAccuracy)
        tvBearing = findViewById(R.id.tvBearing)
        tvBearingAccuracy = findViewById(R.id.tvBearingAccuracy)
        tvProvider = findViewById(R.id.tvProvider)
        tvSatellites = findViewById(R.id.tvSatellites)

        btRequestPosition = findViewById(R.id.btRequestPosition)
        btRequestPosition.setOnClickListener { updatePositionButtonHandler() }

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                requestInProgress = false
                loadingIcon.visibility = View.GONE
                btRequestPosition.text = resources.getText(R.string.request_location)
                setLocation(location)
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}

            override fun onProviderEnabled(s: String) {}

            override fun onProviderDisabled(s: String) {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_save -> {
            save()
            true
        }
        R.id.action_show -> {
            showList()
            true
        }
        else -> {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            super.onOptionsItemSelected(item)
        }
    }

    private fun save() {
        // is set but not saved
        if (currentLocationItem.set && !currentLocationItem.saved) {

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT

            val containerParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            )
            containerParams.topMargin = 8
            containerParams.leftMargin = 58
            containerParams.rightMargin = 58

            val container = FrameLayout(this)
            container.addView(input)
            container.layoutParams = containerParams

            val superContainer = FrameLayout(this)
            superContainer.addView(container)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Location name")
            builder.setView(superContainer)
            builder.setPositiveButton("Save") { _, _ ->
                currentLocationItem.name = input.text.toString().trim()

                CoroutineScope(Dispatchers.IO).launch {
                    val result = locationItemDao.insert(currentLocationItem)
                    runOnUiThread {
                        if (result > 0) {
                            Toast.makeText(this@MainActivity, "Saved", Toast.LENGTH_SHORT).show()
                            currentLocationItem.saved = true
                        } else {
                            Toast.makeText(this@MainActivity, "Error when saving!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            builder.setNegativeButton("Cancel", null)

            val dialog = builder.show()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        } else {
            if (!currentLocationItem.set) {
                Toast.makeText(this, "Empty not saved!", Toast.LENGTH_SHORT).show()
            } else if (currentLocationItem.saved) {
                Toast.makeText(this, "Already saved.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showList() {
        val intent = Intent(this, ListActivity::class.java)
        startActivityForResult(intent, LIST_ACTIVITY_CODE)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LIST_ACTIVITY_CODE && resultCode == RESULT_OK) {
            data?.getStringExtra("locationId")?.toInt()?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    val locationItem = locationItemDao.getItemById(it)
                    runOnUiThread {
                        currentLocationItem = locationItem
                        currentLocationItem.saved = true
                        currentLocationItem.set = true
                        setLocationToUI()
                        if (!currentLocationItem.hasAltitudeReal()) {
                            getRealAltitude(currentLocationItem)
                        }
                    }
                }
            }
        }
    }

    private fun setLocation(location: Location) {
        currentLocationItem = LocationItem()
        currentLocationItem.set = true
        currentLocationItem.saved = false
        currentLocationItem.name = ""

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
            currentLocationItem.altitude = -10_000.0
        }
        currentLocationItem.altitudeReal = -10_000.0

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

        setLocationToUI()
        getRealAltitude(currentLocationItem)
    }

    private fun setLocationToUI() {
        var title = resources.getString(R.string.app_name)
        if (currentLocationItem.hasName()) {
            title += " - " + currentLocationItem.name
        }
        supportActionBar?.title = title

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
        if (!requestInProgress) {
            requestInProgress = true
            loadingIcon.visibility = View.VISIBLE
            btRequestPosition.text = resources.getText(R.string.stop_request)
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
            //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, listener);
        } else {
            locationManager.removeUpdates(listener)
            requestInProgress = false
            loadingIcon.visibility = View.GONE
            btRequestPosition.text = resources.getText(R.string.request_location)
        }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                // if request is cancelled, the result arrays are empty
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    requestPositionUpdate()
                }
                return
            }
            else -> {
                // ignore all other requests
            }
        }
    }

    fun openMap(view: View) {
        if (currentLocationItem.set) {
            val uri = "geo:" + currentLocationItem.latitude + "," + currentLocationItem.longitude
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            baseContext.startActivity(intent)
        }
    }

    /**
     * Get and set real altitude converted from WGS 84 reference ellipsoid to EGM 2008 geoid.
     */
    private fun getRealAltitude(locationItem: LocationItem) {
        loadingIconRealAlt.visibility = View.VISIBLE
        tvAltitudeReal.visibility = View.INVISIBLE
        RetrieveAltitudeTask(WeakReference(this), locationItem.altitude)
                .execute(locationItem.latitude.toString(), locationItem.longitude.toString())
    }

    fun onRetrieveAltitudeTaskComplete(altitudeDiff: Float?, locationAltitude: Double) {
        try {
            if (altitudeDiff != null) {
                val altitudeReal = locationAltitude - altitudeDiff
                currentLocationItem.altitudeReal = altitudeReal
                tvAltitudeReal.text = altitudeFormat.format(altitudeReal)
                if (currentLocationItem.saved) {
                    // if already saved then update
                    locationItemDao.update(currentLocationItem)
                }
            } else {
                currentLocationItem.altitudeReal = -10_000.0
                tvAltitudeReal.text = "-"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            loadingIconRealAlt.visibility = View.GONE
            tvAltitudeReal.visibility = View.VISIBLE
        }
    }

}

internal class RetrieveAltitudeTask(
        private val activityWR: WeakReference<MainActivity>,
        private val locationAltitude: Double
) : AsyncTask<String, Void, Float>() {

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
                return matcher.group(1)?.toFloat()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            urlConnection?.disconnect()
        }
        return null
    }

    override fun onPostExecute(altitudeDiff: Float?) {
        activityWR.get()?.onRetrieveAltitudeTaskComplete(altitudeDiff, locationAltitude)
    }
}
