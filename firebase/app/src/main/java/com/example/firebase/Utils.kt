package com.example.firebase

import com.google.firebase.auth.FirebaseAuth

object BindingUtils {
    @JvmStatic
    fun getSenderText(senderId: String): String {
        return if (senderId == FirebaseAuth.getInstance().currentUser?.uid) {
            "You"
        } else {
            "Partner"
        }
    }
}
