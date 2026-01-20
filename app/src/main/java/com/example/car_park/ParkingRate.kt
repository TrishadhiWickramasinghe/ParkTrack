package com.example.car_park

data class ParkingRate(
    val id: Int,
    val name: String,
    val rate: Double,
    val description: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val vehicleTypes: List<String> = emptyList(),
    val daysOfWeek: List<String> = emptyList(),
    val dailyMax: Double = 0.0,
    val isActive: Boolean = true,
    val type: String = ""
)
