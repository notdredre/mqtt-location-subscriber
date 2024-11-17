package com.example.assignment2.location

import com.example.assignment2.db.DBHelper
import com.google.android.gms.maps.model.LatLng

class LocationManager(private val dbHelper: DBHelper) {

    fun populatePointsMap(pointsMap: HashMap<Int, ArrayList<LatLng>>) {
        val students = dbHelper.getStudents()
        students.forEach { student ->
            val studentLatLng = ArrayList<LatLng>()
            val studentLocations = dbHelper.getLocationsForStudent(student)
            studentLocations.forEach { location ->
                val latLng = LatLng(location.getLat(), location.getLong())
                studentLatLng.add(latLng)
            }
            pointsMap[student] = studentLatLng
        }
    }
}