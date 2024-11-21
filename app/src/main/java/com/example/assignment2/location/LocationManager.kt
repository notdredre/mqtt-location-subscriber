package com.example.assignment2.location

import com.example.assignment2.db.DBHelper
import com.google.android.gms.maps.model.LatLng

class LocationManager(private val dbHelper: DBHelper) {

    fun populatePointsMap(pointsMap: HashMap<Int, ArrayList<LatLng>>) {
        val students = dbHelper.getStudents()
        students.forEach { student ->
            val studentLatLng = ArrayList<LatLng>()
            val studentLocations = dbHelper.getRecentLocations(student, 5)
            studentLocations.forEach { location ->
                val latLng = LatLng(location.getLat(), location.getLong())
                studentLatLng.add(latLng)
            }
            if (studentLatLng.isNotEmpty())
                pointsMap[student] = studentLatLng
        }
    }
}