package com.example.firebase

data class UserWithLastMessage(
    val uid: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    var lastMessage: String = ""
)
