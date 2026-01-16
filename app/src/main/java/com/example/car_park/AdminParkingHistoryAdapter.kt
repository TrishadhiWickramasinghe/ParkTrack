package com.example.car_park

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class AdminParkingHistoryAdapter(private val onItemClick: (AdminParkingRecord) -> Unit) : 
    ListAdapter<AdminParkingRecord, AdminParkingHistoryAdapter.ViewHolder>(AdminParkingRecordDiffCallback()) {
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // TODO: Initialize views from layout
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(View(parent.context))
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = getItem(position)
        holder.itemView.setOnClickListener { onItemClick(record) }
        // TODO: Bind data to views
    }
}

class AdminParkingRecordDiffCallback : DiffUtil.ItemCallback<AdminParkingRecord>() {
    override fun areItemsTheSame(oldItem: AdminParkingRecord, newItem: AdminParkingRecord): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AdminParkingRecord, newItem: AdminParkingRecord): Boolean {
        return oldItem == newItem
    }
}

// Data class for admin parking records
data class AdminParkingRecord(
    val id: Int,
    val driverName: String,
    val carNumber: String,
    val entryTime: String,
    val exitTime: String?,
    val duration: Int?,
    val amount: Double?,
    val status: String,
    val phone: String?
)
