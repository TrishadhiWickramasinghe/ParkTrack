package com.example.car_park

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.car_park.databinding.ItemRateBinding

class RatesAdapter(
    private val onEditClick: (ParkingRate, Int) -> Unit = { _, _ -> },
    private val onDeleteClick: (ParkingRate, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RatesAdapter.ViewHolder>() {

    private val items = mutableListOf<ParkingRate>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun submitList(list: List<ParkingRate>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ItemRateBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rate: ParkingRate) {
            binding.root.setOnClickListener {
                onEditClick(rate, adapterPosition)
            }
        }
    }
}
