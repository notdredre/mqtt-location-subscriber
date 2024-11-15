package com.example.assignment2.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.sql.Timestamp

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
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS Location")
        onCreate(db)
    }

    fun createLocation(studentID: Int, latitude: Double, longitude: Double, velocity: Float) {
        val values = ContentValues()

        values.put("studentID", studentID)
        values.put("latitude", latitude)
        values.put("longitude", longitude)
        values.put("velocity", velocity)

        val db = this.writableDatabase
        db.insert("Location", null, values)
        db.close()
    }
}