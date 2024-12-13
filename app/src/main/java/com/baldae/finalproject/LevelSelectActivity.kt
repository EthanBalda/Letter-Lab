package com.baldae.finalproject

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LevelSelectActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val levelList = arrayOf("aabab", "aabab\naabab", "ababa\nbacab\nbbcbb", "aca")
    private val totalLevels = levelList.size

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        sharedPreferences = getSharedPreferences("GameProgress", MODE_PRIVATE)

        // Initialize level states if needed
        initializeLevelStates()

        val levelContainer: LinearLayout = findViewById(R.id.levelContainer)
        displayLevelButtons(levelContainer)
    }

    private fun initializeLevelStates() {
        val editor = sharedPreferences.edit()
        if (!sharedPreferences.contains("level_0")) {
            for (i in 0 until totalLevels) {
                editor.putInt("level_$i", if (i == 0) 1 else 0) // Unlock the first level, lock others
            }
            editor.apply()
        }
    }

    private fun displayLevelButtons(levelContainer: LinearLayout) {
        levelContainer.removeAllViews() // Clear any existing views

        for (i in 0 until totalLevels) {
            val levelState = sharedPreferences.getInt("level_$i", 0)

            val levelButton = Button(this).apply {
                text = if (levelState == 2) "Level ${i + 1} ✓" else "Level ${i + 1}"
                isEnabled = levelState != 0 // Enable if unlocked or beaten

                if (levelState == 0) {
                    alpha = 0.5f // Dim locked levels
                }

                setOnClickListener {
                    if (isEnabled) {
                        val intent = Intent(this@LevelSelectActivity, GameActivity::class.java)
                        intent.putExtra("level", i)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@LevelSelectActivity, "Level ${i + 1} is locked!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            levelContainer.addView(levelButton)
        }
    }

    override fun onResume() {
        super.onResume()
        val levelContainer: LinearLayout = findViewById(R.id.levelContainer)
        displayLevelButtons(levelContainer) // Refresh buttons on returning to this activity
    }
}
