package com.example.car_park.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.car_park.databinding.ItemParkingHistoryBinding
import com.example.car_park.models.ParkingRecord

/**
 * RecyclerView adapter for displaying parking history records
 */
class ParkingHistoryAdapter(
    private val records: MutableList<ParkingRecord> = mutableListOf(),
    private val onItemClick: (ParkingRecord) -> Unit
) : RecyclerView.Adapter<ParkingHistoryAdapter.ViewHolder>() {
    
    inner class ViewHolder(private val binding: ItemParkingHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(record: ParkingRecord) {
            with(binding) {
                tvCarNumber.text = record.vehicleNumber
                tvEntryTime.text = record.getFormattedEntryTime()
                tvExitTime.text = record.getFormattedExitTime()
                tvDuration.text = record.getFormattedDuration()
                tvAmount.text = record.getFormattedCharges()
                
                root.setOnClickListener {
                    onItemClick(record)
                }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParkingHistoryBinding.inflate(
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
     * Get all current records in the adapter
     */
    fun getRecords(): List<ParkingRecord> = records.toList()
    
    /**
     * Update the adapter with new records
     */
    fun updateRecords(newRecords: List<ParkingRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }
    
    /**
     * Add records to the adapter (for pagination)
     */
    fun addRecords(newRecords: List<ParkingRecord>) {
        val initialSize = records.size
        records.addAll(newRecords)
        notifyItemRangeInserted(initialSize, newRecords.size)
    }
    
    /**
     * Clear all records
     */
    fun clear() {
        records.clear()
        notifyDataSetChanged()
    }
}
