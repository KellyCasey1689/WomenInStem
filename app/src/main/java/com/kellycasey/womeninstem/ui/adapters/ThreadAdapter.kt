package com.kellycasey.womeninstem.ui.adapters

import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.kellycasey.womeninstem.R
import com.kellycasey.womeninstem.model.Thread

class ThreadAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<ThreadAdapter.ThreadViewHolder>() {

    private val items = mutableListOf<Thread>()

    fun submitList(list: List<Thread>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_thread, parent, false)
        return ThreadViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
        val context = holder.itemView.context
        val backgroundRes = if (position % 2 == 0) R.color.color_surface else R.color.color_background
        holder.itemView.setBackgroundColor(ContextCompat.getColor(context, backgroundRes))
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ThreadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textUserName: TextView = itemView.findViewById(R.id.textUserName)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val textLastMessage: TextView = itemView.findViewById(R.id.textLastMessage)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)

        fun bind(thread: Thread) {
            val conversationId = thread.conversationId

            FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener { doc ->
                    val participants = doc.get("participants") as? List<String> ?: return@addOnSuccessListener
                    val otherUserId = participants.firstOrNull { it != currentUserId } ?: return@addOnSuccessListener

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(otherUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "Unknown"
                            textUserName.text = name
                        }
                }

            val preview = thread.lastMessage?.let {
                "${it.senderName}: ${it.text}"
            } ?: ""

            textLastMessage.text = preview
            textTime.text = thread.lastMessage?.timestamp?.toDate()?.let {
                DateFormat.format("MMM dd, HH:mm", it).toString()
            } ?: ""

            if (thread.unreadCount > 0) {
                textUserName.setTypeface(null, Typeface.BOLD)
                textLastMessage.setTypeface(null, Typeface.BOLD)
                unreadIndicator.visibility = View.VISIBLE
            } else {
                textUserName.setTypeface(null, Typeface.NORMAL)
                textLastMessage.setTypeface(null, Typeface.NORMAL)
                unreadIndicator.visibility = View.INVISIBLE
            }
        }
    }
}