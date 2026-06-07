package com.edu.minlish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.edu.minlish.MinLishApp
import com.edu.minlish.core.designsystem.theme.MinLishTheme

import com.edu.minlish.core.util.AudioPlayer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Text-to-Speech Engine
        AudioPlayer.init(this)
        
        // Initialize App Settings & Cache
        com.edu.minlish.core.util.AppSettings.init(this)
        com.edu.minlish.core.util.VocabularyCache.init(this)
        
        // Pre-fetch user data if already logged in (Auto-login)
        val firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    com.edu.minlish.core.util.SessionDataManager.preFetchUserData(currentUser.uid)
                } catch (e: Exception) {
                    // Ignore background load failures
                }
            }
        }
        
        enableEdgeToEdge()
        setContent {
            MinLishTheme {
                MinLishApp()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        AudioPlayer.release()
    }
}