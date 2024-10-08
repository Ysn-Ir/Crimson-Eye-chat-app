package com.example.firebase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserAdapter(
    private var usersList: List<User>,
    private val onClickListener: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = usersList[position]
        holder.bind(user)
        holder.itemView.setOnClickListener { onClickListener(user) }
    }

    override fun getItemCount(): Int = usersList.size

    fun submitList(newUsers: List<User>) {
        usersList = newUsers
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)

        fun bind(user: User) {
            usernameTextView.text = user.username
            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .circleCrop()
                .into(profileImage)
        }
    }
}
