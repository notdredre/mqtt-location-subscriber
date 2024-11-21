package com.example.assignment2.student

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
data class StudentModel(private val studentID: Int, private val minSpeed: Float, private val maxSpeed: Float, private val start: Long, private val end: Long) {
    private val avgSpeed : Float = (minSpeed + maxSpeed) / 2
    private val startDate = SimpleDateFormat("dd-MM-yyyy").format(Date(start))
    private val endDate = SimpleDateFormat("dd-MM-yyyy").format(Date(end))
    constructor(studentID: Int) : this(studentID, 0f, 0f, 0, 0)
    fun getStudentID() : Int {
        return studentID
    }

    fun getMinSpeed() : Float {
        return minSpeed
    }

    fun getMaxSpeed() : Float {
        return maxSpeed
    }

    fun getAvgSpeed() : Float {
        return avgSpeed
    }

    fun getStart() : Long {
        return start
    }

    fun getEnd()  : Long {
        return end
    }

    fun getStartDate() : String {
        return startDate
    }

    fun getEndDate(): String {
        return endDate
    }

    override fun equals(other: Any?): Boolean {
        if (other is StudentModel) {
            return this.studentID == other.studentID
        }
        return false
    }
}