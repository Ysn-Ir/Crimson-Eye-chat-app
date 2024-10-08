package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchActivity : AppCompatActivity() {

    private lateinit var userRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var userAdapter: UserWithLastMessageAdapter
    private val userList = mutableListOf<UserWithLastMessage>()
    private val filteredUserList = mutableListOf<UserWithLastMessage>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        val logoutButton = findViewById<Button>(R.id.logout)

        userRecyclerView = findViewById(R.id.userRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)

        userAdapter = UserWithLastMessageAdapter(filteredUserList) { user ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("userId", user.uid)
                putExtra("userName", user.username)
                putExtra("profileImageUrl", user.profileImageUrl)
                putExtra("chatId", generateChatId(user.uid))
            }
            startActivity(intent)
        }
        userRecyclerView.layoutManager = LinearLayoutManager(this)
        userRecyclerView.adapter = userAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUserList(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadAllUsers()
        logoutButton.setOnClickListener(){
            signOut()
        }
    }

    private fun loadAllUsers() {
        db.collection("users_collection")
            .get()
            .addOnSuccessListener { result ->
                userList.clear()
                for (document in result.documents) {
                    val user = document.toObject(UserWithLastMessage::class.java)
                    if (user != null) {
                        loadLastMessage(user)
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching users: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterUserList(query: String) {
        filteredUserList.clear()
        for (user in userList) {
            if (user.username.contains(query, ignoreCase = true)) {
                filteredUserList.add(user)
            }
        }
        userAdapter.notifyDataSetChanged()
    }

    private fun loadLastMessage(user: UserWithLastMessage) {
        val chatId = generateChatId(user.uid)
        db.collection("chats_collection").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val message = result.documents[0].toObject(ChatMessage::class.java)
                    user.lastMessage = message?.message ?: ""
                }
                userList.add(user)
                filterUserList(searchEditText.text.toString())  // Update filtered list
            }
            .addOnFailureListener {
                userList.add(user)
                filterUserList(searchEditText.text.toString())  // Update filtered list
            }
    }

    private fun generateChatId(partnerId: String): String {
        return if (currentUserId!! < partnerId) {
            "$currentUserId-$partnerId"
        } else {
            "$partnerId-$currentUserId"
        }
    }
    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getChatId(userId: String): String {
        // Implement logic to retrieve chat ID for the given userId
        return "chatIdPlaceholder" // Replace with actual chat ID retrieval logic
    }
}
