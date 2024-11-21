package com.example.assignment2.student

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment2.R
import com.example.assignment2.Utility

class StudentListAdapter(private val students: List<StudentModel>, private val studentListInterface: StudentListInterface) : RecyclerView.Adapter<StudentListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentID: TextView = itemView.findViewById(R.id.studentID)
        val speed: TextView = itemView.findViewById(R.id.speeds)
        val button: Button = itemView.findViewById(R.id.viewMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val student = students[position]
        holder.studentID.text = student.getStudentID().toString()
        holder.studentID.setTextColor(Utility.studentIDToColor(student.getStudentID()))
        val minSpeedKMH = String.format("%3.2f", student.getMinSpeed() * 3.6)
        val maxSpeedKMH = String.format("%3.2f", student.getMaxSpeed() * 3.6)
        holder.speed.text = "Min Speed: $minSpeedKMH km/h\nMax Speed: $maxSpeedKMH km/h"
        holder.button.setOnClickListener {
            studentListInterface.onStudentClicked(student.getStudentID())
        }

    }

    override fun getItemCount(): Int {
        return students.size
    }
}