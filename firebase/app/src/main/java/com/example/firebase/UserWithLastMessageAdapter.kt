package com.example.firebase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
class UserWithLastMessageAdapter(
    private var userList: List<UserWithLastMessage>,
    private val onUserClick: (UserWithLastMessage) -> Unit
) : RecyclerView.Adapter<UserWithLastMessageAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImageView: ImageView = itemView.findViewById(R.id.profileImageView)
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_with_last_message, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.userNameTextView.text = user.username
        holder.lastMessageTextView.text = user.lastMessage
        Glide.with(holder.profileImageView.context).load(user.profileImageUrl).into(holder.profileImageView)
        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    override fun getItemCount(): Int = userList.size

    fun updateUserList(newUserList: List<UserWithLastMessage>) {
        userList = newUserList
        notifyDataSetChanged()
    }
}
