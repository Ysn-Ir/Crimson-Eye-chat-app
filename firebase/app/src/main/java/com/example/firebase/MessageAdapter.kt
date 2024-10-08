package com.example.firebase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messagesList: List<ChatMessage>, private val currentUserId: String) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messagesList[position]
        holder.bind(message, currentUserId)
    }

    override fun getItemCount(): Int = messagesList.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)

        fun bind(message: ChatMessage, currentUserId: String) {
            messageTextView.text = message.message
            if (message.senderId == currentUserId) {
                // Apply your styling for sent messages
                itemView.setBackgroundResource(R.drawable.message_background_sent)
            } else {
                // Apply your styling for received messages
                itemView.setBackgroundResource(R.drawable.message_background_received)
            }
        }
    }
}
