package com.example.firebase

data class Notification(
    val senderId: String,
    val senderUsername: String,
    val message: String,
    val timestamp: Long
)
