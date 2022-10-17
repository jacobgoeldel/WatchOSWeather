package com.example.weatherapp

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.example.weatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlin.math.abs


class MainActivity : Activity() {

    private lateinit var locationManager: LocationManager
    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()
    private val weatherApiKey = "0058bfad72c3b4f91a865f24027f5b54"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!hasGps()) {
            Log.d(TAG, "This hardware doesn't have GPS.")
            // App won't work
        } else {
            getWeatherData()
        }
    }

    private fun hasGps(): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)

    private fun getWeatherData() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 0);
            return
        } else {
            val gpsClient = LocationServices.getFusedLocationProviderClient(this)

            // Get the gps location
            gpsClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).addOnSuccessListener {
                if(it == null) {
                    runOnUiThread {
                        findViewById<TextView>(R.id.txt_temperature).text = "?"
                    }

                    return@addOnSuccessListener
                }

                val lat = it.latitude
                val lon = it.longitude

                val request = Request.Builder()
                    .url("https://api.openweathermap.org/data/2.5/weather?lat=${lat}&lon=${lon}&appid=${weatherApiKey}&units=imperial")
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.d("Info", "API Request Failed!")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val json = response.body()?.string()?.let { it1 -> JSONObject(it1) }
                        if (json != null) {
                            runOnUiThread {
                                findViewById<TextView>(R.id.txt_temperature).text = "%.0f°".format(
                                    abs(json.getJSONObject("main").getDouble("temp")))
                                findViewById<TextView>(R.id.txt_description).text = json.getJSONArray("weather").getJSONObject(0).getString("description")
                            }
                        }
                    }
                })
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        getWeatherData()
    }

    override fun onResume() {
        super.onResume()
        getWeatherData()
    }

}