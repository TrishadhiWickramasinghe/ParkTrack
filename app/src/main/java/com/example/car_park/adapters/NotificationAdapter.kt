package com.example.car_park.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(
    private val notifications: MutableList<com.example.car_park.models.Notification>,
    private val onItemClick: (com.example.car_park.models.Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    
    inner class ViewHolder(private val binding: android.widget.LinearLayout) : RecyclerView.ViewHolder(binding) {
        fun bind(notification: com.example.car_park.models.Notification) {
            // Simplified binding - update as per your layout
            binding.setOnClickListener { onItemClick(notification) }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.widget.LinearLayout(parent.context)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position])
    }
    
    override fun getItemCount() = notifications.size
    
    fun updateList(newNotifications: List<com.example.car_park.models.Notification>) {
        notifications.clear()
        notifications.addAll(newNotifications)
        notifyDataSetChanged()
    }
}