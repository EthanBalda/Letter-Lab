package com.baldae.finalproject
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GameActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var level = 0
    private val totalLevels = 4

    private lateinit var backgroundImageView: ImageView
    private val unsplashApiKey = "4o2U9PpZ47LPEJuqLAA25ExeCyafbTJTq5QAPF_hbkk"

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_level)

        sharedPreferences = getSharedPreferences("GameProgress", MODE_PRIVATE)
        level = intent.getIntExtra("level", 0)
        backgroundImageView = findViewById(R.id.backgroundImageView)

        fetchRandomBackground("landscape")

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert) // Back arrow icon
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Navigate back to Level Select
        }

        val levelList = arrayOf("aabab", "aabab\naabab", "ababa\nbacab\nbbcbb", "aca")
        val generateButtonGrid: Button = findViewById(R.id.generateButtonGrid)
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)
        var tog = false
        var cBtn = '_'
        var cTag = -1


        generateButtonGrid.setOnClickListener() {
            //val level = levelInput.text.toString().toIntOrNull()
            generateButtonGrid.visibility = Button.GONE
            val levelLen = levelList[level].length
            var levelCode = ""

            if (levelLen <= 0) {
                Toast.makeText(this, "Enter a level", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            //val gridSize = levelLen
            var rows = 1
            var columns = 0

            for (i in 0 until levelLen) {
                if (levelList[level][i] == '\n') {
                    rows++
                }
                else if (rows == 1) {
                    columns++
                }

                if (levelList[level][i] != '\n') {
                    levelCode += levelList[level][i]
                }
            }
            Toast.makeText(this@GameActivity, levelCode, Toast.LENGTH_SHORT).show()
            gridLayout.removeAllViews()

            gridLayout.columnCount = columns

            for (i in 0 until rows) {
                for (j in 0 until columns) {
                    val button = Button(this).apply {
                        text = "" + levelCode[i * columns + j]
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = GridLayout.LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                            marginEnd = 8
                            bottomMargin = 8
                            tag = i * columns + j
                            if (levelCode[i * columns + j] == 'a')
                                isEnabled = false
                            else
                                isEnabled = true
                        }
                        setOnClickListener {
                            //Toast.makeText(this@GameActivity, "Button clicked", Toast.LENGTH_SHORT).show()
                            if (!tog) {
                                toggleGridVis(gridLayout, rows, columns)
                                cBtn = text[0]
                                cTag = tag as Int
                                gridLayout.findViewWithTag<Button>(cTag).isEnabled = true
                                tog = true
                                when(text[0]) {
                                    'b', 'e', 'f', 'k' -> {
                                        var nTag = cTag - 1
                                        if (nTag > -1 && nTag / columns == cTag / columns) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag - columns
                                        if (nTag > -1 && nTag / columns == cTag / columns - 1) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag + 1
                                        if (nTag < levelCode.length && nTag / columns == cTag / columns) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag + columns
                                        if (nTag < levelCode.length && nTag / columns == cTag / columns + 1) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                    }
                                    'c' -> {
                                        var nTag = cTag - 2
                                        if (nTag > -1 && nTag / columns == cTag / columns) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag - 2 * columns
                                        if (nTag > -1 && nTag / columns == cTag / columns - 2) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag + 2
                                        if (nTag < levelCode.length && nTag / columns == cTag / columns) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag + 2 * columns
                                        if (nTag < levelCode.length && nTag / columns == cTag / columns + 2) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                    }
                                    'j' -> {
                                        var nTag = cTag - 3
                                        if (nTag > -1 && nTag / columns == cTag / columns) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag - 3 * columns
                                        if (nTag > -1 && nTag / columns == cTag / columns - 3) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag + 3
                                        if (nTag < levelCode.length && nTag / columns == cTag / columns) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                        nTag = cTag + 3 * columns
                                        if (nTag < levelCode.length && nTag / columns == cTag / columns + 3) {
                                            gridLayout.findViewWithTag<Button>(nTag).isEnabled = true
                                        }
                                    }
                                }
                            }
                            else {
                                //toggleGridVis(gridLayout, rows, columns)
                                if (cTag == tag as Int) {
                                    tog = false
                                    resetGridVis(gridLayout, rows, columns)
                                    return@setOnClickListener
                                }
                                when(cBtn) {
                                    'b', 'c', 'j' -> {
                                        if (text[0] != 'd') {
                                            val tmp = gridLayout.findViewWithTag<Button>(cTag).text[0]
                                            gridLayout.findViewWithTag<Button>(cTag).text = "" + text[0]
                                            text = "" + tmp
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                    }
                                }
                                //Toast.makeText(this@GameActivity, "Checking Win", Toast.LENGTH_SHORT).show()
                                if (winCheck(gridLayout, rows, columns)) {
                                    Toast.makeText(this@GameActivity, "You Win!!!", Toast.LENGTH_SHORT).show()
                                    toggleGridVis(gridLayout, rows, columns)
                                    level++
                                    generateButtonGrid.visibility = Button.VISIBLE
                                    markLevelComplete()

                                }
                            }
                        }
                    }
                    gridLayout.addView(button)
                }
            }
        }


    }
    private fun fetchRandomBackground(keyword: String) {
        val call = ApiClient.backgroundApi.getRandomPhoto(unsplashApiKey, keyword)

        call.enqueue(object : Callback<UnsplashPhotoResponse> {
            override fun onResponse(call: Call<UnsplashPhotoResponse>, response: Response<UnsplashPhotoResponse>) {
                if (response.isSuccessful) {
                    val imageUrl = response.body()?.urls?.regular
                    if (imageUrl != null) {
                        Glide.with(this@GameActivity)
                            .load(imageUrl)
                            .into(backgroundImageView)
                    }
                } else {
                    Toast.makeText(this@GameActivity, "Failed to load background", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UnsplashPhotoResponse>, t: Throwable) {
                Toast.makeText(this@GameActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun markLevelComplete() {
        val editor = sharedPreferences.edit()
        editor.putInt("level_$level", 2) // Mark the current level as beaten

        // Unlock the next level if it exists

        if (level + 1 < totalLevels) {
            editor.putInt("level_${level}", 1) // Unlock the next level
        }

        editor.apply()
        Toast.makeText(this, "Level ${level} completed!", Toast.LENGTH_SHORT).show()
        finish() // Go back to the level select screen
    }


    fun toggleGridVis(glayout : GridLayout, rows : Int, columns : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                glayout.findViewWithTag<Button>(i * columns + j).isEnabled = false
            }
        }
    }

    fun resetGridVis(glayout : GridLayout, rows : Int, columns : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                val ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
                if (ltr != 'a' && ltr != 'd' && ltr != 'i')
                    glayout.findViewWithTag<Button>(i * columns + j).isEnabled = true
                else
                    glayout.findViewWithTag<Button>(i * columns + j).isEnabled = false
            }
        }
    }

    fun winCheck(glayout : GridLayout, rows : Int, columns : Int) : Boolean {
        //Horizontal
        for (i in 0 until rows) {
            var compH = '_'
            var upH = 0
            for (j in 0 until columns) {
                val ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
                if (compH == '_') {
                    compH = ltr
                }
                else if (ltr != '_') {
                    if (upH == 0) {
                        if (compH.compareTo(ltr) < 0) {
                            upH = -1
                            compH = ltr
                        }
                        else if (compH.compareTo(ltr) > 0) {
                            upH = 1
                            compH = ltr
                        }
                    }
                    else if (upH == -1) {
                        if (compH.compareTo(ltr) > 0) {
                            Toast.makeText(this@GameActivity, "up then down", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                    else{
                        if (compH.compareTo(ltr) < 0) {
                            Toast.makeText(this@GameActivity, "down then up", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                }
            }
        }
        //I am very aware this is the inefficient way to do this, but I wanted to finish the project
        //quickly
        //Vertical
        for (j in 0 until columns) {
            var compH = '_'
            var upH = 0
            for (i in 0 until rows) {
                val ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
                if (compH == '_') {
                    compH = ltr
                }
                else if (ltr != '_') {
                    if (upH == 0) {
                        if (compH.compareTo(ltr) < 0) {
                            upH = -1
                            compH = ltr
                        }
                        else if (compH.compareTo(ltr) > 0) {
                            upH = 1
                            compH = ltr
                        }
                    }
                    else if (upH == -1) {
                        if (compH.compareTo(ltr) > 0) {
                            Toast.makeText(this@GameActivity, "up then down", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                    else{
                        if (compH.compareTo(ltr) < 0) {
                            Toast.makeText(this@GameActivity, "down then up", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                }
            }
        }

        return true
    }
}