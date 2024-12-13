package com.baldae.finalproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button

import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val playButton: Button = findViewById(R.id.playButton)
        playButton.setOnClickListener {
            val intent = Intent(this, LevelSelectActivity::class.java)
            startActivity(intent)
        }

        // Dictionary button
        val dictionaryButton: Button = findViewById(R.id.dictionaryButton)
        dictionaryButton.setOnClickListener {
            val intent = Intent(this, DictionaryActivity::class.java)
            startActivity(intent)
        }

        // Quit button closes the app
        val quitButton: Button = findViewById(R.id.quitButton)
        quitButton.setOnClickListener {
            finishAffinity() // Closes the app
        }
    }
}