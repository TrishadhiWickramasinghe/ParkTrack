package com.example.car_park.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.car_park.databinding.ItemMonthlySessionBinding
import com.example.car_park.models.ParkingRecord

/**
 * RecyclerView adapter for displaying monthly parking sessions
 */
class MonthlySessionAdapter(
    private val records: MutableList<ParkingRecord> = mutableListOf(),
    private val onItemClick: (ParkingRecord) -> Unit
) : RecyclerView.Adapter<MonthlySessionAdapter.ViewHolder>() {
    
    inner class ViewHolder(private val binding: ItemMonthlySessionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(record: ParkingRecord) {
            with(binding) {
                tvDate.text = record.getFormattedDate()
                tvVehicleNumber.text = record.vehicleNumber
                tvEntryTime.text = record.getFormattedEntryTimeOnly()
                tvExitTime.text = record.getFormattedExitTimeOnly()
                tvDuration.text = record.getFormattedDuration()
                tvCharges.text = record.getFormattedCharges()
                
                root.setOnClickListener {
                    onItemClick(record)
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMonthlySessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }
    
    override fun getItemCount(): Int = records.size
    
    /**
     * Update the adapter with new records
     */
    fun updateRecords(newRecords: List<ParkingRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }
    
    /**
     * Clear all records
     */
    fun clear() {
        records.clear()
        notifyDataSetChanged()
    }
}
