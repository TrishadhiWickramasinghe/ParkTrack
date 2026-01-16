package com.example.car_park

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.car_park.databinding.ItemParkingHistoryBinding

class ParkingHistoryAdapter(
    private val onItemClick: (ParkingRecord) -> Unit
) : ListAdapter<ParkingRecord, ParkingHistoryAdapter.ViewHolder>(DiffCallback()) {
    
    class ViewHolder(private val binding: ItemParkingHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(record: ParkingRecord, onItemClick: (ParkingRecord) -> Unit) {
            binding.tvCarNumber.text = record.carNumber
            binding.tvEntryTime.text = record.entryTime
            binding.tvExitTime.text = record.exitTime ?: "In Progress"
            binding.tvDuration.text = "${record.duration ?: "0"} min"
            binding.tvAmount.text = "₹${"%.2f".format(record.amount ?: 0.0)}"
            binding.tvStatus.text = record.status
            
            binding.root.setOnClickListener {
                onItemClick(record)
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
        holder.bind(getItem(position), onItemClick)
    }
    
    private class DiffCallback : DiffUtil.ItemCallback<ParkingRecord>() {
        override fun areItemsTheSame(oldItem: ParkingRecord, newItem: ParkingRecord): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ParkingRecord, newItem: ParkingRecord): Boolean {
            return oldItem == newItem
        }
    }
}
