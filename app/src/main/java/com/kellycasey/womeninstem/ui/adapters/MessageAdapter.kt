package com.kellycasey.womeninstem.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kellycasey.womeninstem.databinding.ItemMessageReceivedBinding
import com.kellycasey.womeninstem.databinding.ItemMessageSentBinding
import com.kellycasey.womeninstem.model.Message

class MessageAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Message>()
    private val TYPE_SENT = 1
    private val TYPE_RECEIVED = 2

    fun submitList(list: List<Message>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) =
        if (items[position].senderId == currentUserId) TYPE_SENT else TYPE_RECEIVED

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        if (viewType == TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentViewHolder(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedViewHolder(binding)
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        if (holder is SentViewHolder) holder.bind(message)
        else if (holder is ReceivedViewHolder) holder.bind(message)
    }

    override fun getItemCount() = items.size

    inner class SentViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.textMessageBody.text = msg.text
        }
    }

    inner class ReceivedViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.textMessageBody.text = msg.text
        }
    }
}
