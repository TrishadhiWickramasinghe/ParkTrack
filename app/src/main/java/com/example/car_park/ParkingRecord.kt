package com.example.car_park

data class ParkingRecord(
    val id: Int,
    val carNumber: String,
    val entryTime: String,
    val exitTime: String?,
    val duration: String?,
    val amount: Double?,
    val status: String
)
