package com.edu.minlish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.edu.minlish.MinLishApp
import com.edu.minlish.core.designsystem.theme.MinLishTheme

import com.edu.minlish.core.util.AudioPlayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Text-to-Speech Engine
        AudioPlayer.init(this)
        
        // Initialize App Settings
        com.edu.minlish.core.util.AppSettings.init(this)
        
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