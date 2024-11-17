package com.example.assignment2

import android.graphics.Color
import android.os.Build

class Utility {
    companion object {
        fun studentIDToColor(studentID: Int) : Int {
            val studentIDColor = studentID.toString().substring(3)
            val studentRed : Float = studentIDColor.substring(0,2).toInt() / 99f
            val studentGreen : Float = studentIDColor.substring(2,4).toInt() / 99f
            val studentBlue : Float = studentIDColor.substring(4,6).toInt() / 99f
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Color.argb(1f, studentRed, studentGreen, studentBlue)
            } else {
                return studentIDColor.toInt()
            }
        }
    }
}