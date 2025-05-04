package com.kellycasey.womeninstem.ui.adapters

import android.graphics.Typeface
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
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
        val bgRes = if (position % 2 == 0) R.color.color_surface else R.color.color_background
        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, bgRes))

        val thread = items[position]
        holder.bind(thread)
        holder.itemView.setOnClickListener { onThreadClick(thread.conversationId) }
    }

    override fun getItemCount(): Int = items.size

    inner class ThreadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val conversationNameView: TextView = itemView.findViewById(R.id.textConversationName)
        private val previewView: TextView = itemView.findViewById(R.id.textLastMessage)
        private val timeView: TextView = itemView.findViewById(R.id.textTime)
        private val dot: View = itemView.findViewById(R.id.unreadIndicator)

        fun bind(thread: Thread) {
            // Line 1: Conversation name (bold title)
            conversationNameView.text = thread.conversationName

            // Line 2: Message preview
            val last = thread.lastMessage
            if (last != null) {
                val senderName = last.senderName.takeIf { it.isNotBlank() } ?: "Unknown"
                val displayName = if (senderName == currentUserName) "Me" else senderName
                previewView.text = "$displayName: ${last.text}"
                timeView.text = DateFormat.format("MMM dd, HH:mm", last.timestamp.toDate()).toString()
            } else {
                previewView.text = ""
                timeView.text = ""
            }

            // Dot for unread
            dot.visibility = if (thread.unreadCount > 0) View.VISIBLE else View.INVISIBLE

            // Bold message preview if unread
            val typeface = if (thread.unreadCount > 0) Typeface.BOLD else Typeface.NORMAL
            previewView.setTypeface(null, typeface)
        }
    }
}
