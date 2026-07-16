package com.baldae.letterlab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.baldae.letterlab.ui.LetterLabNavHost
import com.baldae.letterlab.ui.theme.LetterLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LetterLabTheme {
                LetterLabNavHost()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appContainer().musicManager.onForeground()
    }

    override fun onStop() {
        super.onStop()
        appContainer().musicManager.onBackground()
    }
}
