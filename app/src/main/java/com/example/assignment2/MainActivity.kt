package com.example.assignment2

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import android.Manifest
import android.widget.FrameLayout
import com.example.assignment2.models.LocationModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import java.nio.charset.Charset

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mqttClient : Mqtt5AsyncClient? = null
    private val topic : String = "notthatguy"
    private var hasPermissions : Boolean = false
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            results : Map<String, Boolean> ->
        hasPermissions = true
        for (result in results.keys) {
            if (results[result] == false)
                hasPermissions = false
            //updateUI()
        }
    }

    private var map : GoogleMap? = null
    private var pointsList = mutableListOf(LocationModel)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mqttClient = Mqtt5Client.builder()
            .identifier("subscriber")
            .serverHost("broker-816036749.sundaebytestt.com")
            .serverPort(1883)
            .buildAsync()

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED -> {
                hasPermissions = true
            }
            else -> {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.INTERNET))
            }
        }

        try {
            mqttClient?.connect()
            mqttClient?.subscribeWith()
                ?.topicFilter(topic)
                ?.callback({ publish ->
                    runOnUiThread {
                        val received = String(publish.payloadAsBytes, Charset.defaultCharset())
                        val receivedLoc = Gson().fromJson(received, LocationModel::class.java)
                        addMarkerAtLocation(receivedLoc)
                        Log.e("SUBSCRIBE", "Received a message $receivedLoc")
                    }
            })
                ?.send()
                ?.whenComplete({ subAck, throwable ->
                    if (throwable != null) {
                        Log.e("SUBSCRIBE", "COULD NOT SUBSCRIBE")
                    }
                    else {
                        Log.e("SUBSCRIBE", "SUBSCRIPTION SUCCESSFUL")
                    }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }



        updateUI()
    }

    private fun updateUI() {
        val subscribeLayout : ConstraintLayout = findViewById(R.id.subscribe)
        val permissionLayout : ConstraintLayout = findViewById(R.id.permissions)

        subscribeLayout.visibility = if (hasPermissions) View.VISIBLE else View.GONE
        permissionLayout.visibility = if (!hasPermissions) View.VISIBLE else View.GONE
    }

    fun checkPermissions() : Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun getPermissions(view: View) {
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
    }

    private fun addMarkerAtLocation(location: LocationModel) {
        val latLng = LatLng(location.getLat(), location.getLong())

        map?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Marker ${location.getID()}")
        )
        Log.i("MARKER", "ADDED NEW MARKER")
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
    }



}