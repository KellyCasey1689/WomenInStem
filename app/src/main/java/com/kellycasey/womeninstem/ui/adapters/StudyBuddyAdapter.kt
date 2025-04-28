package com.kellycasey.womeninstem.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kellycasey.womeninstem.databinding.ItemStudyBuddyBinding
import com.kellycasey.womeninstem.model.User

class StudyBuddyAdapter(
    private var buddies: List<User>,
    private val onRemoveClick: (User) -> Unit
) : RecyclerView.Adapter<StudyBuddyAdapter.StudyBuddyViewHolder>() {

    inner class StudyBuddyViewHolder(
        private val binding: ItemStudyBuddyBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.textViewName.text = user.name
            binding.textViewSubject.text = user.subject
            binding.buttonRemove.setOnClickListener {
                onRemoveClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudyBuddyViewHolder {
        val binding = ItemStudyBuddyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudyBuddyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudyBuddyViewHolder, position: Int) {
        holder.bind(buddies[position])
    }

    override fun getItemCount(): Int = buddies.size

    fun updateBuddies(newBuddies: List<User>) {
        buddies = newBuddies
        notifyDataSetChanged()
    }
}
