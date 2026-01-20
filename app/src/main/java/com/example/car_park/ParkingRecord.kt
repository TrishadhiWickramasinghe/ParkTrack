package com.example.car_park

data class ParkingRecord(
    val id: Int,
    val carNumber: String,
    val entryTime: String,
    val exitTime: String?,
    val duration: Int?,
    val amount: Double?,
    val status: String,
    val driverName: String? = null,
    val phone: String? = null
)
