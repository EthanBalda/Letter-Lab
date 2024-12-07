package com.baldae.finalproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar

class GameActivity : ComponentActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_level)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert) // Back arrow icon
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Navigate back to Main Menu
        }

        val levelInput: EditText = findViewById(R.id.levelInput)
        val generateButtonGrid: Button = findViewById(R.id.generateButtonGrid)
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)

        generateButtonGrid.setOnClickListener {
            val level = levelInput.text.toString().toIntOrNull()

            if (level == null || level <= 0) {
                Toast.makeText(this, "Enter a level", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gridSize = level
            val rows = gridSize
            val columns = gridSize

            gridLayout.removeAllViews()

            gridLayout.columnCount = columns

            for (i in 0 until rows) {
                for (j in 0 until columns) {
                    val button = Button(this).apply {
                        text = "R${i + 1}C${j + 1}"
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            marginEnd = 8
                            bottomMargin = 8
                        }
                    }
                    gridLayout.addView(button)
                }
            }
        }
    }
}