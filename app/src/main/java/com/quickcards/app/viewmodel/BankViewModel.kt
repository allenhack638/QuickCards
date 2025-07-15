package com.quickcards.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quickcards.app.data.database.QuickCardsDatabase
import com.quickcards.app.data.model.Bank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = QuickCardsDatabase.getDatabase(application, viewModelScope)
    private val bankDao = database.bankDao()
    
    val allBanks: LiveData<List<Bank>> = bankDao.getAllBanks().asLiveData()
    
    fun insertBank(bank: Bank) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bankDao.insertBank(bank)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateBank(bank: Bank) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bankDao.updateBank(bank)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteBank(bank: Bank) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bankDao.deleteBank(bank)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteAllBanks() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                bankDao.deleteAllBanks()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Ensure default banks exist
    fun ensureDefaultBanksExist() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val existingBanks = bankDao.getAllBanksSync()
                if (existingBanks.isEmpty()) {
                    // Insert default banks if none exist
                    bankDao.insertBanks(Bank.getDefaultBanks())
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}