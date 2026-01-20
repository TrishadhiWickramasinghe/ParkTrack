package com.example.car_park

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class DailySummaryAdapter(val items: List<Any> = emptyList()) : RecyclerView.Adapter<DailySummaryAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false) as android.widget.TextView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(val view: android.widget.TextView) : RecyclerView.ViewHolder(view)
}