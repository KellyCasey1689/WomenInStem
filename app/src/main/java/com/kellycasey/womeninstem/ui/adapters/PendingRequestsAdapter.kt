package com.kellycasey.womeninstem.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kellycasey.womeninstem.databinding.ItemIncomingRequestBinding
import com.kellycasey.womeninstem.ui.studybuddy.IncomingRequestWithUser

class PendingRequestsAdapter(
    private var items: List<IncomingRequestWithUser>,
    private val onItemClick: (IncomingRequestWithUser) -> Unit
) : RecyclerView.Adapter<PendingRequestsAdapter.PendingViewHolder>() {

    inner class PendingViewHolder(
        private val binding: ItemIncomingRequestBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IncomingRequestWithUser) {
            binding.textViewRequesterName.text = item.user.name
            binding.textViewMessage.text = item.request.message
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingViewHolder {
        val binding = ItemIncomingRequestBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PendingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateRequests(newItems: List<IncomingRequestWithUser>) {
        items = newItems
        notifyDataSetChanged()
    }
}
