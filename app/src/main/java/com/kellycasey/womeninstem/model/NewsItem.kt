package com.kellycasey.womeninstem.model
import com.google.firebase.Timestamp

data class NewsItem(
    val content: String = "",
    val createdAt: Timestamp? = null,
    val title: String = "",
    val id: String = "" // <-- We'll set this manually after fetching
)
