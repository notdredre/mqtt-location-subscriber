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
import android.graphics.Color
import android.os.Build
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment2.db.DBHelper
import com.example.assignment2.location.LocationManager
import com.example.assignment2.location.LocationModel
import com.example.assignment2.student.StudentListAdapter
import com.example.assignment2.student.StudentModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import java.nio.charset.Charset
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

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
    private var pointsMap : HashMap<Int, ArrayList<LatLng>> = HashMap()
    private var following : Boolean = false

    private val dbHelper: DBHelper = DBHelper(this)
    private val locationManager : LocationManager = LocationManager(dbHelper)

    private val studentList: ArrayList<StudentModel> = ArrayList()
    private var studentListAdapter: StudentListAdapter? = null

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
                        dbHelper.createLocation(receivedLoc.getStudentID(), receivedLoc.getLat(), receivedLoc.getLong(), receivedLoc.getVelocity(), receivedLoc.getTimestamp())
                        locationManager.populatePointsMap(pointsMap)
                        updateStudent(receivedLoc.getStudentID())
                        studentListAdapter = StudentListAdapter(studentList)
                        updateUI()
                        if (!following)
                            updateCameraToBounds()
                        else
                            updateCameraToStudent(receivedLoc.getStudentID())
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

    private fun init() {
        locationManager.populatePointsMap(pointsMap)
        drawPolyline()
    }

    private fun updateUI() {
        val subscribeLayout : ConstraintLayout = findViewById(R.id.subscribe)
        val permissionLayout : ConstraintLayout = findViewById(R.id.permissions)
        val studentList: RecyclerView = findViewById(R.id.rvStudents)
        studentListAdapter?.let{
            studentList.adapter = it
        }
        studentList.layoutManager = LinearLayoutManager(this)
        subscribeLayout.visibility = if (hasPermissions) View.VISIBLE else View.GONE
        permissionLayout.visibility = if (!hasPermissions) View.VISIBLE else View.GONE
        map?.clear()
        drawPolyline()

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
        init()
    }

    private fun addPoint(location: LocationModel) {
        val latLng = LatLng(location.getLat(), location.getLong())
        if (pointsMap[location.getStudentID()] == null)
            pointsMap[location.getStudentID()] = ArrayList()
        pointsMap[location.getStudentID()]?.add(latLng)
    }

    private fun updateCameraToBounds() {
        val bounds = LatLngBounds.builder()
        pointsMap.values.forEach{
                latlngList ->
            latlngList.forEach{
                bounds.include(it)
            }
        }
        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    private fun updateCameraToStudent(studentID: Int) {
        val studentLatLngs = pointsMap[studentID]
        val bounds = LatLngBounds.builder()
        studentLatLngs?.forEach {
            bounds.include(it)
        }
        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    private fun drawPolyline() {
        for (student in pointsMap.keys) {
            val studentLocations = dbHelper.getLocationsForStudent(student)
            drawStudentPoints(student, studentLocations, 10000)
        }
    }

    private fun drawStudentPoints(studentID: Int, studentPoints: ArrayList<LocationModel>, interval : Int) {
        if (studentPoints.isEmpty())
            return

        val pointIter = studentPoints.iterator()
        var last : LocationModel = pointIter.next()
        var curr : LocationModel?
        var lastLatLng = LatLng(last.getLat(), last.getLong())
        var currLatLng: LatLng?
        var endMarker : Marker? = null
        map?.addMarker(MarkerOptions().position(lastLatLng).title("$studentID Start"))
        while (pointIter.hasNext()) {
            val drawBetween = ArrayList<LatLng>()
            curr = pointIter.next()

            currLatLng = LatLng(curr.getLat(), curr.getLong())

            drawBetween.add(lastLatLng)
            drawBetween.add(currLatLng)

            endMarker?.remove()
            endMarker = map?.addMarker(MarkerOptions().position(currLatLng).title("$studentID End"))

            val polylineOptions = PolylineOptions()
                .addAll(drawBetween)
                .color(Utility.studentIDToColor(studentID))
                .width(5f)
                .geodesic(true)
            val drawnLine = map?.addPolyline(polylineOptions)
            if (curr.getTimestamp() - last.getTimestamp() > interval) {
                drawnLine?.remove()
                map?.addMarker(MarkerOptions().position(lastLatLng).title("$studentID End"))
                map?.addMarker(MarkerOptions().position(currLatLng).title("$studentID Start"))
            }

            last = curr
        }
    }

    private fun updateStudent(studentID: Int) {
        val minSpeed = dbHelper.getMinSpeed(studentID)
        val maxSpeed = dbHelper.getMaxSpeed(studentID)
        val start = dbHelper.getStart(studentID)
        val end = dbHelper.getEnd(studentID)
        val new = StudentModel(studentID, minSpeed, maxSpeed, start, end)
        val curr = StudentModel(studentID)
        if (studentList.contains(curr)) {
            val index = studentList.indexOf(curr)
            studentList[index] = new
        } else {
            studentList.add(new)
        }
    }
}