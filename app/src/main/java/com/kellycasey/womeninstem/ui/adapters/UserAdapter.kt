package com.kellycasey.womeninstem.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kellycasey.womeninstem.databinding.ItemUserBinding
import com.kellycasey.womeninstem.model.User
import com.google.firebase.auth.FirebaseAuth

class UserAdapter(
    private var userList: List<User>,
    private val onRequestClick: (String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textViewName.text = user.name
            binding.textViewSubject.text = user.subject
            binding.textViewUniversity.text = user.university

            // Get current user ID
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            // 1) Check if already study buddies
            val isBuddy = currentUserId != null && user.studyBuddies.contains(currentUserId)

            // 2) Check if there is a pending request
            val isPending = user.isRequestPending()

            when {
                isBuddy -> {
                    binding.buttonRequestBuddy.text = "Request Accepted"
                    binding.buttonRequestBuddy.isEnabled = false
                }
                isPending -> {
                    binding.buttonRequestBuddy.text = "Request Pending"
                    binding.buttonRequestBuddy.isEnabled = false
                }
                else -> {
                    binding.buttonRequestBuddy.text = "Request Buddy"
                    binding.buttonRequestBuddy.isEnabled = true
                }
            }

            binding.buttonRequestBuddy.setOnClickListener {
                onRequestClick(user.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size

    fun updateUsers(users: List<User>) {
        userList = users
        notifyDataSetChanged()
    }
}
