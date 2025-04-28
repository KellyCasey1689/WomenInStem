package com.kellycasey.womeninstem.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kellycasey.womeninstem.R
import com.kellycasey.womeninstem.model.NewsItem
import java.text.SimpleDateFormat
import java.util.Locale

class NewsAdapter(private var newsList: List<NewsItem>) : RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTitle)
        val contentTextView: TextView = itemView.findViewById(R.id.textViewContent)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val newsItem = newsList[position]
        holder.titleTextView.text = newsItem.title
        holder.contentTextView.text = newsItem.content
        holder.dateTextView.text = newsItem.createdAt?.toDate()?.let { dateFormat.format(it) } ?: ""
    }

    override fun getItemCount() = newsList.size

    fun updateNews(newList: List<NewsItem>) {
        newsList = newList
        notifyDataSetChanged()
    }
}
