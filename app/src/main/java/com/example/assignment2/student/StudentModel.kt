package com.example.assignment2.student

data class StudentModel(private val studentID: Int, private val minSpeed: Float, private val maxSpeed: Float, private val start: Int, private val end: Int) {

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

    fun getStart() : Int {
        return start
    }

    fun getEnd()  : Int {
        return end
    }

    override fun equals(other: Any?): Boolean {
        if (other is StudentModel) {
            return this.studentID == other.studentID
        }
        return false
    }
}