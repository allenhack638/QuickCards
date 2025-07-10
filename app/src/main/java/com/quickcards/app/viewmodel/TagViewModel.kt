package com.quickcards.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quickcards.app.data.database.QuickCardsDatabase
import com.quickcards.app.data.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TagViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = QuickCardsDatabase.getDatabase(application, viewModelScope)
    private val tagDao = database.tagDao()
    
    val allTags: LiveData<List<Tag>> = tagDao.getAllTags().asLiveData()
    
    fun insertTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tagDao.insertTag(tag)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tagDao.updateTag(tag)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tagDao.deleteTag(tag)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteAllTags() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                tagDao.deleteAllTags()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}