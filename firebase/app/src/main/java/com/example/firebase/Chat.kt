package com.example.firebase

data class Chat(
    val chatId: String = "",
    val partnerId: String = "",
    val lastMessage: String = "",
    val timestamp: Long = 0
)
