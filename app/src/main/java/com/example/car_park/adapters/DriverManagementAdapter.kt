package com.example.car_park.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DriverManagementAdapter(
    private val drivers: MutableList<com.example.car_park.models.DriverInfo>,
    private val onItemClick: (com.example.car_park.models.DriverInfo) -> Unit
) : RecyclerView.Adapter<DriverManagementAdapter.ViewHolder>() {
    
    inner class ViewHolder(val binding: android.widget.LinearLayout) : RecyclerView.ViewHolder(binding) {
        fun bind(driver: com.example.car_park.models.DriverInfo) {
            binding.setOnClickListener { onItemClick(driver) }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = android.widget.LinearLayout(parent.context)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(drivers[position])
    }
    
    override fun getItemCount() = drivers.size
    
    fun updateList(newDrivers: List<com.example.car_park.models.DriverInfo>) {
        drivers.clear()
        drivers.addAll(newDrivers)
        notifyDataSetChanged()
    }
}