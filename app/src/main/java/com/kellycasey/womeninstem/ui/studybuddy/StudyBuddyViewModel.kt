package com.kellycasey.womeninstem.ui.studybuddy

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StudyBuddyViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is study buddy Fragment"
    }
    val text: LiveData<String> = _text
}