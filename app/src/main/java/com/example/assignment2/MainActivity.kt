package com.example.assignment2

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment2.db.DBHelper
import com.example.assignment2.location.LocationManager
import com.example.assignment2.location.LocationModel
import com.example.assignment2.student.StudentListAdapter
import com.example.assignment2.student.StudentListInterface
import com.example.assignment2.student.StudentModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.gson.Gson
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import java.nio.charset.Charset

class MainActivity : AppCompatActivity(), OnMapReadyCallback, StudentListInterface {
    private var mqttClient : Mqtt5AsyncClient? = null
    private val brokerAddress : String = "broker-816036749.sundaebytestt.com"
    private val topic : String = "assignment/location"
    private var hasPermissions : Boolean = false
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            results : Map<String, Boolean> ->
        hasPermissions = true
        for (result in results.keys) {
            if (results[result] == false)
                hasPermissions = false
        }
    }

    private var map : GoogleMap? = null
    private var pointsMap : HashMap<Int, ArrayList<LatLng>> = HashMap()
    private var following : Boolean = false

    private val dbHelper: DBHelper = DBHelper(this)
    private val locationManager : LocationManager = LocationManager(dbHelper)

    private val studentList: ArrayList<StudentModel> = ArrayList()
    private var studentListAdapter: StudentListAdapter? = null
    private var focusedStudent: StudentModel? = null

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

        studentListAdapter = StudentListAdapter(studentList, this)
        mqttClient = Mqtt5Client.builder()
            .identifier("subscriber")
            .serverHost(brokerAddress)
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
                ?.callback { publish ->
                    runOnUiThread {
                        val received = String(publish.payloadAsBytes, Charset.defaultCharset())
                        val receivedLoc = Gson().fromJson(received, LocationModel::class.java)
                        dbHelper.createLocation(
                            receivedLoc.getStudentID(),
                            receivedLoc.getLat(),
                            receivedLoc.getLong(),
                            receivedLoc.getVelocity(),
                            receivedLoc.getTimestamp()
                        )
                        locationManager.populatePointsMap(pointsMap)
                        updateStudent(receivedLoc.getStudentID())
                        updateUI()
                        if (!following)
                            updateCameraToBounds()
                        else {
                            focusStudent(focusedStudent!!)
                            updateCameraToStudent(focusedStudent!!)
                        }
                    }
                }
                ?.send()
                ?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        Log.e("SUBSCRIBE", "COULD NOT SUBSCRIBE")
                    } else {
                        Log.i("SUBSCRIBE", "SUBSCRIPTION SUCCESSFUL")
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }



        updateUI()
    }

    private fun init() {
        locationManager.populatePointsMap(pointsMap)

        for (student in pointsMap.keys)
            updateStudent(student)
        updateUI()
    }

    @SuppressLint("DefaultLocale")
    private fun updateUI() {
        val subscribeLayout : ConstraintLayout = findViewById(R.id.subscribe)
        val permissionLayout : ConstraintLayout = findViewById(R.id.permissions)
        subscribeLayout.visibility = if (hasPermissions) View.VISIBLE else View.GONE
        permissionLayout.visibility = if (!hasPermissions) View.VISIBLE else View.GONE

        val studentList: RecyclerView = findViewById(R.id.rvStudents)
        studentListAdapter?.let{
            studentList.adapter = it
        }
        studentList.layoutManager = LinearLayoutManager(this)

        val mainTitle : TextView = findViewById(R.id.mainTitle)
        val unfocusedSubtitle : ConstraintLayout = findViewById(R.id.unfocused)
        val startDate : TextView = findViewById(R.id.startDate)
        val endDate : TextView = findViewById(R.id.endDate)
        val focusedSubtitle : ConstraintLayout = findViewById(R.id.focused)
        val speedDetail : ConstraintLayout = findViewById(R.id.speedDetail)
        val speeds : TextView = findViewById(R.id.speeds)
        map?.clear()
        if (following) {
            val mainText = "${focusedStudent!!.getStudentID()} Summary"
            mainTitle.text = mainText
            mainTitle.setTextColor(Utility.studentIDToColor(focusedStudent!!.getStudentID()))
            unfocusedSubtitle.visibility = View.GONE

            val startDateText = "Start Date:\n${focusedStudent!!.getStartDate()}"
            val endDateText = "End Date:\n${focusedStudent!!.getEndDate()}"
            startDate.text = startDateText
            endDate.text = endDateText

            val speedsText = "Min Speed: ${String.format("%3.2f", focusedStudent!!.getMinSpeed() * 3.6)} km/h\n" +
                    "Max Speed: ${String.format("%3.2f", focusedStudent!!.getMaxSpeed() * 3.6)} km/h\n" +
                    "Average Speed: ${String.format("%3.2f", focusedStudent!!.getAvgSpeed() * 3.6)} km/h"
            speeds.text = speedsText
            focusedSubtitle.visibility = View.VISIBLE
            studentList.visibility = View.GONE
            speedDetail.visibility = View.VISIBLE
            drawPolyline(focusedStudent!!)
        } else {
            val mainText = "Assignment 2 Subscriber"
            mainTitle.text = mainText
            unfocusedSubtitle.visibility = View.VISIBLE
            focusedSubtitle.visibility = View.GONE
            studentList.visibility = View.VISIBLE
            speedDetail.visibility = View.GONE
            drawPolylines()
        }
    }

    fun getPermissions(view: View) {
        requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0
        init()
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

    private fun updateCameraToStudent(student: StudentModel) {
        val studentLatLngs = pointsMap[student.getStudentID()]
        val bounds = LatLngBounds.builder()
        studentLatLngs?.forEach {
            bounds.include(it)
        }
        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    override fun onStudentClicked(studentID: Int) {
        focusStudent(studentList[studentList.indexOf(StudentModel(studentID))])
    }

    fun untrackStudent(view: View) {
        following = false
        updateCameraToBounds()
        focusedStudent = null
        updateUI()
    }

    private fun focusStudent(student: StudentModel) {
        following = true
        updateCameraToStudent(student)
        focusedStudent = studentList[studentList.indexOf(student)]
        updateUI()
    }

    private fun addMarkers(studentID: Int) {
        var locations: ArrayList<LocationModel>?
        if (!following) {
            locations = dbHelper.getRecentLocations(studentID, 5)
        } else {
            locations = dbHelper.getLocations()
        }

        if (locations.isEmpty())
            return

        val start = dbHelper.getStartLocation(studentID)
        val end = dbHelper.getEndLocation(studentID)

        map?.addMarker(MarkerOptions().position(start!!.toLatLng()).title("$studentID Start"))
        map?.addMarker(MarkerOptions().position(end!!.toLatLng()).title("$studentID End"))
    }

    private fun drawPolylines() {
        for (student in pointsMap.keys) {
            addMarkers(student)
            val studentLocations = dbHelper.getRecentLocations(student, 1)
            drawStudentPoints(student, studentLocations, 10000)
        }
    }

    private fun drawPolyline(student: StudentModel) {
        addMarkers(student.getStudentID())
        val studentLocations = dbHelper.getLocationsForStudent(student.getStudentID())
        drawStudentPoints(student.getStudentID(), studentLocations, 10000)
    }

    private fun drawStudentPoints(studentID: Int, studentPoints: ArrayList<LocationModel>, interval : Int) {
        if (studentPoints.isEmpty())
            return

        val pointIter = studentPoints.iterator()
        var last : LocationModel = pointIter.next()
        var curr : LocationModel?
        val lastLatLng = LatLng(last.getLat(), last.getLong())
        var currLatLng: LatLng?
        val drawBetween = ArrayList<LatLng>()
        drawBetween.add(lastLatLng)
        while (pointIter.hasNext()) {
            curr = pointIter.next()
            currLatLng = LatLng(curr.getLat(), curr.getLong())
            drawBetween.add(currLatLng)
            if (curr.getTimestamp() - last.getTimestamp() > interval) {
                drawBetween.remove(currLatLng)
                val polylineOptions = PolylineOptions()
                    .addAll(drawBetween)
                    .color(Utility.studentIDToColor(studentID))
                    .width(5f)
                    .geodesic(true)
                map?.addPolyline(polylineOptions)
                drawBetween.clear()
            }
            last = curr
        }
        val polylineOptions = PolylineOptions()
            .addAll(drawBetween)
            .color(Utility.studentIDToColor(studentID))
            .width(5f)
            .geodesic(true)
        map?.addPolyline(polylineOptions)
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