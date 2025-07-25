package com.quickcards.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.quickcards.app.data.database.QuickCardsDatabase
import com.quickcards.app.ui.base.BaseActivity
import com.quickcards.app.ui.screens.MainScreen
import com.quickcards.app.ui.theme.QuickCardsTheme
import com.quickcards.app.viewmodel.BankViewModel

class MainActivity : BaseActivity() {
    
    private lateinit var database: QuickCardsDatabase
    private lateinit var bankViewModel: BankViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize database and view models
        database = QuickCardsDatabase.getDatabase(this, lifecycleScope)
        bankViewModel = BankViewModel(application)
        
        // Ensure default banks exist
        bankViewModel.ensureDefaultBanksExist()
        
        // Show main content directly
        showMainContent()
    }
    
    private fun showMainContent() {
        setContent {
            QuickCardsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}