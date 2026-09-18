package com.padil.stickly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.padil.stickly.ui.StickLyApp
import com.padil.stickly.ui.theme.StickLyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StickLyTheme {
                StickLyApp()
            }
        }
    }
}