package com.example.car_park.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.car_park.R
import com.example.car_park.databinding.ItemPaymentRecordBinding
import com.example.car_park.models.PaymentRecord

class PaymentHistoryAdapter(
    private val payments: MutableList<PaymentRecord>,
    private val onItemClick: (PaymentRecord) -> Unit
) : RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder>() {
    
    inner class ViewHolder(private val binding: ItemPaymentRecordBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: PaymentRecord) {
            binding.apply {
                tvVehicle.text = payment.vehicleNumber
                tvDate.text = payment.date
                tvCharges.text = "₹${String.format("%.2f", payment.charges)}"
                
                // Status color
                val statusColor = if (payment.status == "completed") {
                    R.color.dark_green
                } else {
                    R.color.warning_orange
                }
                tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, statusColor))
                tvStatus.text = payment.status.uppercase()
                
                // Time info
                tvTimeInfo.text = "${payment.entryTime} → ${payment.exitTime}"
                
                root.setOnClickListener { onItemClick(payment) }
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentRecordBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(payments[position])
    }
    
    override fun getItemCount() = payments.size
    
    fun updateList(newPayments: List<PaymentRecord>) {
        payments.clear()
        payments.addAll(newPayments)
        notifyDataSetChanged()
    }
}
