package com.theayushyadav11.MessEase.Models

data class MessLeave(
    val id: String = "",
    val uid: String = "",
    val userName: String = "",
    val type: String = "", // "FULL_DAY", "MEAL_SKIP", "EMERGENCY"
    val date: String = "",
    val meal: String = "", // "Breakfast", "Lunch", "Dinner" (only for MEAL_SKIP)
    val status: String = "PENDING", // "APPROVED", "PENDING_APPROVAL", "APPROVED_EXCEPTION", "REJECTED"
    val exceptionCase: Boolean = false,
    val timestamp: Long = 0
)
