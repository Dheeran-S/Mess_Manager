package com.theayushyadav11.MessEase.ui.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.theayushyadav11.MessEase.Models.MessLeave
import com.theayushyadav11.MessEase.R
import java.text.SimpleDateFormat
import java.util.*

class MessLeaveAdapter(
    private var leaves: List<MessLeave>
) : RecyclerView.Adapter<MessLeaveAdapter.LeaveViewHolder>() {

    class LeaveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvMeal: TextView = itemView.findViewById(R.id.tvMeal)
        val tvException: TextView = itemView.findViewById(R.id.tvException)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leave_item_layout, parent, false)
        return LeaveViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val leave = leaves[position]
        
        // User name
        holder.tvUserName.text = leave.userName.ifEmpty { "Unknown User" }
        
        // Status with color coding
        holder.tvStatus.text = when (leave.status) {
            "PENDING_APPROVAL" -> "PENDING"
            "APPROVED" -> "APPROVED"
            "DENIED" -> "DENIED"
            "REJECTED" -> "REJECTED"
            else -> leave.status
        }
        
        // Status background color
        val statusColor = when (leave.status) {
            "PENDING_APPROVAL" -> Color.parseColor("#FF9800") // Orange
            "APPROVED" -> Color.parseColor("#4CAF50") // Green
            "DENIED", "REJECTED" -> Color.parseColor("#F44336") // Red
            else -> Color.parseColor("#9E9E9E") // Gray
        }
        holder.tvStatus.setBackgroundColor(statusColor)
        
        // Date
        holder.tvDate.text = leave.date
        
        // Type
        holder.tvType.text = when (leave.type) {
            "FULL_DAY" -> "Full Day"
            "MEAL_SKIP" -> "Meal Skip"
            "EMERGENCY" -> "Emergency"
            else -> leave.type
        }
        
        // Meal (only for MEAL_SKIP)
        if (leave.type == "MEAL_SKIP" && leave.meal.isNotEmpty()) {
            holder.tvMeal.visibility = View.VISIBLE
            holder.tvMeal.text = "Meal: ${leave.meal}"
        } else {
            holder.tvMeal.visibility = View.GONE
        }
        
        // Exception badge
        if (leave.exceptionCase) {
            holder.tvException.visibility = View.VISIBLE
        } else {
            holder.tvException.visibility = View.GONE
        }
        
        // Timestamp
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val date = Date(leave.timestamp)
        holder.tvTimestamp.text = "Requested on: ${sdf.format(date)}"
    }

    override fun getItemCount(): Int = leaves.size

    fun updateLeaves(newLeaves: List<MessLeave>) {
        leaves = newLeaves
        notifyDataSetChanged()
    }
    
    fun filterByStatus(status: String) {
        // This will be handled in the fragment
    }
}
