package com.example.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var messagesAdapter: MessageAdapter
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private lateinit var chatId: String
    private lateinit var receiverId: String
    private val messagesList = mutableListOf<ChatMessage>()

    private var isActivityVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val profileImageView = findViewById<ImageView>(R.id.profileImageView)
        val userNameTextView = findViewById<TextView>(R.id.userNameTextView)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        chatId = intent.getStringExtra("chatId")!!
        receiverId = intent.getStringExtra("userId")!!
        val userName = intent.getStringExtra("userName")
        val profileImageUrl = intent.getStringExtra("profileImageUrl")

        userNameTextView.text = userName
        Glide.with(this).load(profileImageUrl).into(profileImageView)

        messagesAdapter = MessageAdapter(messagesList, currentUserId!!)
        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messagesAdapter

        loadMessages()

        sendButton.setOnClickListener {
            sendMessage()
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityVisible = true
    }

    override fun onPause() {
        super.onPause()
        isActivityVisible = false
    }

    private fun isChatActivityVisible(): Boolean {
        return isActivityVisible
    }
    private fun loadMessages() {
        db.collection("chats_collection").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    messagesList.clear()
                    for (document in snapshot.documents) {
                        val message = document.toObject(ChatMessage::class.java)
                        if (message != null) {
                            messagesList.add(message)
                            if (message.receiverId == currentUserId && !isChatActivityVisible()) {
                                // Fetch sender username and trigger local notification
                                fetchSenderUsername(message.senderId) { senderUsername ->
                                    sendNotification(senderUsername, message.message)
                                }
                            }
                        }
                    }
                    messagesAdapter.notifyDataSetChanged()
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }
            }
    }

    private fun fetchSenderUsername(senderId: String, callback: (String) -> Unit) {
        db.collection("users").document(senderId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val senderUsername = document.getString("username") ?: "Unknown Sender"
                    callback(senderUsername)
                } else {
                    callback("Unknown Sender")
                }
            }
            .addOnFailureListener {
                callback("Unknown Sender")
            }
    }


    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel_id",
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for default notifications"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(senderUsername: String, messageBody: String) {
        createNotificationChannel()

        val intent = Intent(this, ChatActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "default_channel_id")
            .setSmallIcon(R.drawable.notfication)
            .setContentTitle("Message from $senderUsername")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }


    private fun sendMessage() {
        val messageText = messageEditText.text.toString()
        if (messageText.isNotEmpty()) {
            val message = ChatMessage(
                senderId = currentUserId!!,
                receiverId = receiverId,
                message = messageText,
                timestamp = System.currentTimeMillis()
            )
            db.collection("chats_collection").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener {
                    messageEditText.text.clear()
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }
        }
    }
    private fun checkAndRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now send notifications
            } else {
                // Permission denied, handle appropriately
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

}
