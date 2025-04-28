package com.kellycasey.womeninstem.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.kellycasey.womeninstem.databinding.ItemUserBinding
import com.kellycasey.womeninstem.model.User

class UserAdapter(
    private var userList: List<User>,
    private val onRequestClick: (String, String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textViewName.text = user.name
            binding.textViewSubject.text = user.subject
            binding.textViewUniversity.text = user.university

            // Check if there is a pending request (either outgoing or incoming) using User's method
            val isPending = user.isRequestPending()

            if (isPending) {
                binding.buttonRequestBuddy.text = "Request Pending"
                binding.buttonRequestBuddy.isEnabled = false
            } else {
                binding.buttonRequestBuddy.text = "Request Buddy"
                binding.buttonRequestBuddy.isEnabled = true
            }

            binding.buttonRequestBuddy.setOnClickListener {
                onRequestClick(user.id, "I'd like to be your study buddy!")
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    fun updateUsers(users: List<User>) {
        userList = users
        notifyDataSetChanged()
    }
}
