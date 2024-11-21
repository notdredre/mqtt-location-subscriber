package com.example.assignment2.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.assignment2.location.LocationModel

private const val DATABASE_NAME = "db"
private const val DATABASE_VERSION = 1

class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE Location(" +
                "locationID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "studentID INTEGER NOT NULL," +
                "latitude DOUBLE," +
                "longitude DOUBLE," +
                "velocity FLOAT," +
                "timestamp LONG)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS Location")
        onCreate(db)
    }

    fun drop() {
        val db = this.readableDatabase
        db.execSQL("DROP TABLE IF EXISTS Location")
        onCreate(db)
    }

    fun createLocation(studentID: Int, latitude: Double, longitude: Double, velocity: Float, timestamp: Long) {
        val values = ContentValues()

        values.put("studentID", studentID)
        values.put("latitude", latitude)
        values.put("longitude", longitude)
        values.put("velocity", velocity)
        values.put("timestamp", timestamp)

        val db = this.writableDatabase
        db.insert("Location", null, values)
        db.close()
    }

    fun getLocations() : ArrayList<LocationModel> {
        var results = ArrayList<LocationModel>()
        val db = this.readableDatabase
        val cursor = db.query("Location", null, null, null, null, null, null)
        with(cursor) {
            while(moveToNext()) {
                val studentID = getInt(getColumnIndexOrThrow("studentID"))
                val latitude = getDouble(getColumnIndexOrThrow("latitude"))
                val longitude = getDouble(getColumnIndexOrThrow("longitude"))
                val velocity = getFloat(getColumnIndexOrThrow("velocity"))
                val timestamp = getLong(getColumnIndexOrThrow("timestamp"))
                val location = LocationModel(studentID, latitude, longitude, velocity, timestamp)
                results.add(location)
            }
        }
        cursor.close()
        return results
    }

    fun getLocationsForStudent(studentID: Int) : ArrayList<LocationModel> {
        var results = ArrayList<LocationModel>()
        val db = this.readableDatabase
        val cursor = db.query("Location", null, "studentID = ?", arrayOf(studentID.toString()), null, null, null)
        with(cursor) {
            while(moveToNext()) {
                val latitude = getDouble(getColumnIndexOrThrow("latitude"))
                val longitude = getDouble(getColumnIndexOrThrow("longitude"))
                val velocity = getFloat(getColumnIndexOrThrow("velocity"))
                val timestamp = getLong(getColumnIndexOrThrow("timestamp"))
                val location = LocationModel(studentID, latitude, longitude, velocity, timestamp)
                results.add(location)
            }
        }
        cursor.close()
        return results
    }

    fun getRecentLocations(studentID: Int, cutoffInMinutes: Int) : ArrayList<LocationModel> {
        val cutoffMillis = cutoffInMinutes * 60000
        val now = System.currentTimeMillis()
        val results = ArrayList<LocationModel>()
        val db = this.readableDatabase
        val cursor = db.query("Location", null, "studentID = ?", arrayOf(studentID.toString()), null, null, null)
        with(cursor) {
            while(moveToNext()) {
                val latitude = getDouble(getColumnIndexOrThrow("latitude"))
                val longitude = getDouble(getColumnIndexOrThrow("longitude"))
                val velocity = getFloat(getColumnIndexOrThrow("velocity"))
                val timestamp = getLong(getColumnIndexOrThrow("timestamp"))
                val location = LocationModel(studentID, latitude, longitude, velocity, timestamp)
                if (now - timestamp <= cutoffMillis)
                    results.add(location)
            }
        }
        cursor.close()
        return results
    }

    fun getStudents() : ArrayList<Int> {
        val db = this.readableDatabase
        val results = ArrayList<Int>()
        val cursor = db.query(true, "Location", arrayOf("studentID"), null, null, "studentID", null, "studentID ASC", null)
        with(cursor) {
            while(moveToNext()) {
                val studentID = getInt(getColumnIndexOrThrow("studentID"))
                results.add(studentID)
            }
        }
        cursor.close()
        return results
    }

    fun getMinSpeed(studentID: Int) : Float {
        val db = this.readableDatabase
        val cursor = db.query("Location", arrayOf("velocity"), "studentID = ?", arrayOf(studentID.toString()), null, null, "velocity ASC")
        var result = 0f
        with(cursor) {
            cursor.moveToFirst()
            result = getFloat(getColumnIndexOrThrow("velocity"))
        }
        cursor.close()
        return result
    }

    fun getMaxSpeed(studentID: Int) : Float {
        val db = this.readableDatabase
        val cursor = db.query("Location", arrayOf("velocity"), "studentID = ?", arrayOf(studentID.toString()), null, null, "velocity DESC")
        var result = 0f
        with(cursor) {
            cursor.moveToFirst()
            result = getFloat(getColumnIndexOrThrow("velocity"))
        }
        cursor.close()
        return result
    }

    fun getStart(studentID: Int) : Long {
        val db = this.readableDatabase
        val cursor = db.query("Location", arrayOf("timestamp"), "studentID = ?", arrayOf(studentID.toString()), null, null, "timestamp ASC")
        var result : Long = 0
        with(cursor) {
            cursor.moveToFirst()
            result = getLong(getColumnIndexOrThrow("timestamp"))
        }
        cursor.close()
        return result
    }

    fun getEnd(studentID: Int) : Long {
        val db = this.readableDatabase
        val cursor = db.query("Location", arrayOf("timestamp"), "studentID = ?", arrayOf(studentID.toString()), null, null, "timestamp DESC")
        var result : Long = 0
        with(cursor) {
            cursor.moveToFirst()
            result = getLong(getColumnIndexOrThrow("timestamp"))
        }
        cursor.close()
        return result
    }

    fun getStartLocation(studentID: Int) : LocationModel? {
        var result : LocationModel? = null
        val db = this.readableDatabase
        val cursor = db.query("Location", null, "studentID = ?", arrayOf(studentID.toString()), null, null, "timestamp DESC")
        with(cursor) {
            while(moveToNext()) {
                val latitude = getDouble(getColumnIndexOrThrow("latitude"))
                val longitude = getDouble(getColumnIndexOrThrow("longitude"))
                val velocity = getFloat(getColumnIndexOrThrow("velocity"))
                val timestamp = getLong(getColumnIndexOrThrow("timestamp"))
                val location = LocationModel(studentID, latitude, longitude, velocity, timestamp)
                result = location
            }
        }
        cursor.close()
        return result
    }

    fun getEndLocation(studentID: Int) : LocationModel? {
        var result : LocationModel? = null
        val db = this.readableDatabase
        val cursor = db.query("Location", null, "studentID = ?", arrayOf(studentID.toString()), null, null, "timestamp ASC")
        with(cursor) {
            while(moveToNext()) {
                val latitude = getDouble(getColumnIndexOrThrow("latitude"))
                val longitude = getDouble(getColumnIndexOrThrow("longitude"))
                val velocity = getFloat(getColumnIndexOrThrow("velocity"))
                val timestamp = getLong(getColumnIndexOrThrow("timestamp"))
                val location = LocationModel(studentID, latitude, longitude, velocity, timestamp)
                result = location
            }
        }
        cursor.close()
        return result
    }
}