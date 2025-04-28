package com.kellycasey.womeninstem.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kellycasey.womeninstem.model.NewsItem

class HomeViewModel : ViewModel() {

    private val _newsList = MutableLiveData<List<NewsItem>>()
    val newsList: LiveData<List<NewsItem>> = _newsList

    private val db = FirebaseFirestore.getInstance("womeninstem-db")

    init {
        fetchNews()
    }

    private fun fetchNews() {
        db.collection("newsfeed")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val items = result.map { document ->
                    document.toObject(NewsItem::class.java).copy(id = document.id)
                }
                _newsList.value = items
            }
            .addOnFailureListener { exception ->
                _newsList.value = emptyList() // (you can log exception.message here too)
            }
    }
}
