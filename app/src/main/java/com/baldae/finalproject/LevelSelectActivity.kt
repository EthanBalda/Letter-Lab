package com.baldae.finalproject
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class LevelSelectActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val totalLevels = 4 // Total number of levels available

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_level_select)

        sharedPreferences = getSharedPreferences("GameProgress", MODE_PRIVATE)
        val completedLevels = sharedPreferences.getInt("completedLevels", 0)
        sharedPreferences.edit().putInt("completedLevels", 0).apply()

        val levelContainer: LinearLayout = findViewById(R.id.levelContainer)

        // Create buttons dynamically for each level
        for (i in 0 until totalLevels) {
            val levelButton = Button(this).apply {
                text = "Level ${i + 1}"
                isEnabled = i <= completedLevels // Unlock the level if it's completed or the next one
                setOnClickListener {
                    val intent = Intent(this@LevelSelectActivity, GameActivity::class.java)
                    intent.putExtra("level", i) // Pass the selected level index
                    startActivity(intent)
                }
            }
            levelContainer.addView(levelButton)
        }
    }
}


