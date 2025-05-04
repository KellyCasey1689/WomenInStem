package com.kellycasey.womeninstem.ui.adapters

import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kellycasey.womeninstem.R
import com.kellycasey.womeninstem.model.Thread

class ThreadAdapter(
    private val currentUserId: String,
    private val currentUserName: String,
    private val onThreadClick: (String) -> Unit
) : RecyclerView.Adapter<ThreadAdapter.ThreadViewHolder>() {

    private val items = mutableListOf<Thread>()

    fun submitList(list: List<Thread>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_thread, parent, false)
        return ThreadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        // alternate background
        val bgRes = if (position % 2 == 0) R.color.color_surface else R.color.color_background
        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, bgRes))

        val thread = items[position]
        holder.bind(thread)
        holder.itemView.setOnClickListener { onThreadClick(thread.conversationId) }
    }

    override fun getItemCount(): Int = items.size

    inner class ThreadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.textUserName)
        private val previewView: TextView = itemView.findViewById(R.id.textLastMessage)
        private val timeView: TextView = itemView.findViewById(R.id.textTime)
        private val dot: View = itemView.findViewById(R.id.unreadIndicator)

        fun bind(thread: Thread) {
            // 1) Load the other party’s name
            FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(thread.conversationId)
                .get()
                .addOnSuccessListener { doc ->
                    val participants = doc.get("participants") as? List<String> ?: return@addOnSuccessListener
                    val otherId = participants.firstOrNull { it != currentUserId } ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(otherId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            nameView.text = userDoc.getString("name") ?: "Unknown"
                        }
                }

            // 2) Build last‐message preview with "Me" fallback
            val last = thread.lastMessage
            if (last != null) {
                // fall back to "Unknown" if blank
                val senderName = last.senderName.takeIf { it.isNotBlank() } ?: "Unknown"
                val displayName = if (senderName == currentUserName) "Me" else senderName

                previewView.text = "$displayName: ${last.text}"
                timeView.text = DateFormat.format("MMM dd, HH:mm", last.timestamp.toDate()).toString()
            } else {
                previewView.text = ""
                timeView.text = ""
            }

            // 3) Unread styling
            if (thread.unreadCount > 0) {
                dot.visibility = View.VISIBLE
                nameView.setTypeface(null, Typeface.BOLD)
                previewView.setTypeface(null, Typeface.BOLD)
            } else {
                dot.visibility = View.INVISIBLE
                nameView.setTypeface(null, Typeface.NORMAL)
                previewView.setTypeface(null, Typeface.NORMAL)
            }
        }
    }
}
