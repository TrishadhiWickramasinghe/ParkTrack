package com.example.car_park.models

data class PaymentOrder(
    val orderId: String = "",
    val amount: Long = 0,
    val currency: String = "INR",
    val userId: Long = 0,
    val parkingId: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class PaymentTransaction(
    val transactionId: String = "",
    val orderId: String = "",
    val userId: Long = 0,
    val amount: Double = 0.0,
    val status: String = "", // SUCCESS, FAILED, PENDING
    val paymentMethod: String = "razorpay",
    val timestamp: Long = System.currentTimeMillis(),
    val receiptUrl: String = ""
)

data class SubscriptionPlan(
    val planId: String = "",
    val name: String = "", // Basic, Premium, Enterprise
    val monthlyRate: Double = 0.0,
    val dailyLimit: Int = 0, // max parking mins per day
    val monthlyLimit: Int = 0, // max parking mins per month
    val discountPercentage: Double = 0.0,
    val features: List<String> = listOf()
)

data class VehicleCategory(
    val categoryId: String = "",
    val name: String = "", // 2-Wheeler, 4-Wheeler, etc
    val hourlyRate: Double = 0.0,
    val dailyCap: Double = 0.0,
    val monthlyRate: Double = 0.0
)
