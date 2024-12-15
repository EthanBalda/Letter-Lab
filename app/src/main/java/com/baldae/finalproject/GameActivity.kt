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
    private val totalLevels = 25

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

        val levelList = arrayOf("aabab",
            "aabab\naabab",
            "ababa\nbacab\nbbcbb",
            "aba\nbcb\naba",
            "abac\ncdab",
            "ccbcc\nbbabb\ncadac\nbcacb",
            "abce\ncbca",
            "bcb\neae\nbcb",
            "abbab\nabbab\naafbb\nbabba\nbabba",
            "abdb\ndbca\nabcf",
            "gaabcab\nbcbdaag",
            "aaab\nbaaa\nabaa\naaba\ngccg",
            "bac\nbbb\nghc\naca\nbbc",
            "bbcb\nbheb\nbcdb\neaae",
            "jcbcj\ncbabc\nbajab\ncbacb\njcbcj",
            "facaj\nedade\njacaf",
            "kcbc\nbcab\njeje\ncbck",
            "kdefk\njcbfk",
            "bdbcdbd\ndbdbbdb\nbdbadbd\ncbalabc\ndbdabdb\nbdbbdbd\ndbdcbdb",
            "ljjjaah\nekfgbba\naaaabbc",
            "dbdcbdb\nbdbbdbd\ndbdabdb\ncbamabc\nbdbadbd\ndbdbbdb\nbdbcdbd",
            "aabbc\nabcde\nbcdee\nbmebb\naaaaa",
            "gaaaaad\nkdaaaaa\nabafaba",
            "abcd\nefgh\njklm\nhgfe\ndcba",
            "ggaaagg\ndeeeeed\nhkhkhkh"
        )

        /* References for how 'f' should work
            5    0    5    0    5    0    5    0
                           a b b a b
                           a a f b b
                           b a b b a
                           b a b b a
                           a b b a b
                              /|\
                               ^
                               f
                              /|\
        b b a b a          a b b a b          b a b b a
        b b a b a          a b b a b          b a b b a
        a f b b a <- <f <- a a f b b -> f> -> b a a f b
        a b b a b          b a b b a          a b a b b
        a b b a b          b a b b a          a b a b b
                              \|/
                               f
                               V
                              \|/
                           b a b b a
                           a b b a b
                           a b b a b
                           a a f b b
                           b a b b a

         a a d b a          a _ d a a
         d b c f b -> <f -> d c f b _
         a b c b a          b c b a a

                  a a d _ b
         -> f> -> d _ b c f
                  a a b c b
         */

        //val levelList = arrayOf("bac\nbbb\nghc\naca\nbbc", "aca")
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
                                    'g' -> {
                                        showGGridVis(gridLayout, rows, columns, cTag)
                                    }
                                    'h' -> {
                                        showAllGridVis(gridLayout, rows, columns)
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
                                        if (text[0] != 'd' && text[0] != 'i') {
                                            val tmp = gridLayout.findViewWithTag<Button>(cTag).text
                                            gridLayout.findViewWithTag<Button>(cTag).text = text
                                            text = "" + tmp
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                        else if (text[0] == 'd'){
                                            gridLayout.findViewWithTag<Button>(cTag).text = "_"
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                    }
                                    'e' -> {
                                        if (text[0] != 'i') {
                                            gridLayout.findViewWithTag<Button>(cTag).text = text
                                            text = "_"
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                    }
                                    'f' -> {
                                        val dir : Int
                                        if (tag == cTag + 1) dir = 1
                                        else if (tag == cTag - 1) dir = -1
                                        else if (tag == cTag + columns) dir = -2
                                        else dir = 2
                                        cycleGridInDirection(gridLayout, rows, columns, dir)
                                        tog = false
                                        resetGridVis(gridLayout, rows, columns)
                                    }
                                    'g' -> {
                                        if (text[0] != '_') {
                                            val dir : Int
                                            if (tag as Int - cTag in 1..<columns) {
                                                dir = 1
                                            }
                                            else if (tag as Int - cTag < 0 && tag as Int - cTag > -columns) {
                                                dir = -1
                                            }
                                            else if (tag as Int - cTag >= columns) {
                                                dir = -2
                                            }
                                            else {
                                                dir = 2
                                            }
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, tag as Int, dir, false)
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                    }
                                    'h' -> {
                                        if (text[0] != 'i') {
                                            val tmp : Char
                                            if (gridLayout.findViewWithTag<Button>(cTag).text.length > 1) {
                                                if (gridLayout.findViewWithTag<Button>(cTag).text[1] != 'd') {
                                                    tmp = gridLayout.findViewWithTag<Button>(cTag).text[1]
                                                    gridLayout.findViewWithTag<Button>(cTag).text = "h$text"
                                                    if (text[0] == 'd')
                                                        text = "_"
                                                    else
                                                        text = tmp.toString()
                                                }
                                                else {
                                                    text = "_"
                                                }
                                            }
                                            else {
                                                gridLayout.findViewWithTag<Button>(cTag).text = "h$text"
                                                text = "_"
                                            }
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                    }
                                    'k' -> {
                                        if (text[0] != '_') {
                                            val dir : Int
                                            if (tag == cTag + 1) {
                                                dir = 1
                                            }
                                            else if (tag == cTag - 1) {
                                                dir = -1
                                            }
                                            else if (tag == cTag + columns) {
                                                dir = -2
                                            }
                                            else {
                                                dir = 2
                                            }
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, -1, dir, true)
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                    }
                                    'l' -> {
                                        //Mechanically, this is just a very unpicky 'k'. It always kicks its neighbors
                                        if (cTag % columns < columns - 2)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, -1, 1, true)
                                        if (cTag % columns > 1)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, -1, -1, true)
                                        if (cTag / columns > 1)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, -1, 2, true)
                                        if (cTag / columns < rows - 2)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, -1, -2, true)
                                        tog = false
                                        resetGridVis(gridLayout, rows, columns)
                                    }
                                    'm' -> {
                                        //Mechanically, this a very picky 'g'. It always grabs all four edges
                                        if (cTag % columns < columns - 2)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, cTag + columns - cTag % columns - 1, 1, false)
                                        if (cTag % columns > 1)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, cTag - cTag % columns, -1, false)
                                        if (cTag / columns > 1)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, cTag % columns, 2, false)
                                        if (cTag / columns < rows - 2)
                                            cycleGridAboutCoord(gridLayout, rows, columns, cTag, rows * (columns - 1) + cTag % columns, -2, false)
                                        tog = false
                                        resetGridVis(gridLayout, rows, columns)
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

    //Shows what letters the 'g' letter can grab. Must be same row or column as 'g'
    private fun showGGridVis(glayout : GridLayout, rows : Int, columns : Int, coord : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                if (i == coord / columns || j == coord % columns) {
                    val ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
                    glayout.findViewWithTag<Button>(i * columns + j).isEnabled = (ltr != '_' && ltr != 'i')
                }
            }
        }
    }

    //Shows all letters that can be grabbed. Used by letters with universal range, such as 'h'
    private fun showAllGridVis(glayout : GridLayout, rows : Int, columns : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                glayout.findViewWithTag<Button>(i * columns + j).isEnabled = true
            }
        }
    }

    //Hides all letters. Used to reset board to a state where the player must select an
    //initial letter to start swapping around.
    private fun toggleGridVis(glayout : GridLayout, rows : Int, columns : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                glayout.findViewWithTag<Button>(i * columns + j).isEnabled = false
            }
        }
    }

    //Moves every letter on the grid one space in a specified direction. Any letters
    //that would go off the grid instead loop back to the otherside.
    //Used by 'f'
    private fun cycleGridInDirection(glayout : GridLayout, rows : Int, columns : Int, direction : Int){
        //0 = no move, 1 = right, 2 = up, -1 = left, -2 = down
        if (direction == -1) {
            Toast.makeText(this@GameActivity, "Left", Toast.LENGTH_SHORT).show()
            for (i in 0..< rows) {
                val sltr = glayout.findViewWithTag<Button>(i * columns).text
                for (j in 0..< columns) {
                    if (j < columns - 1) {
                        if (glayout.findViewWithTag<Button>(i * columns + j + 1).text[0] == 'd')
                            glayout.findViewWithTag<Button>(i * columns + j).text = "_"
                        else if (glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'd' && glayout.findViewWithTag<Button>(i * columns + j + 1).text[0] != 'i')
                            glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>(i * columns + j + 1).text
                    }
                    else {
                        if (sltr[0] == 'd') {
                            glayout.findViewWithTag<Button>(i * columns + j).text = "_"
                        }
                        else if (glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'd' && glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(i * columns + j).text = sltr.toString()
                        }
                    }
                }
            }
        }
        else if (direction == 1) {
            Toast.makeText(this@GameActivity, "Right", Toast.LENGTH_SHORT).show()
            for (i in 0..< rows) {
                val sltr = glayout.findViewWithTag<Button>(i * columns + columns - 1).text
                for (j in (columns - 1) downTo 0) {
                    if (j > 0) {
                        //glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>(i * columns + j - 1).text
                        if (glayout.findViewWithTag<Button>(i * columns + j - 1).text[0] == 'd')
                            glayout.findViewWithTag<Button>(i * columns + j).text = "_"
                        else if (glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'd' && glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'i')
                            glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>(i * columns + j - 1).text
                    }
                    else {
                        if (sltr[0] == 'd') {
                            glayout.findViewWithTag<Button>(i * columns).text = "_"
                        }
                        else if (glayout.findViewWithTag<Button>(i * columns).text[0] != 'd' && glayout.findViewWithTag<Button>(i * columns).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(i * columns).text = sltr.toString()
                        }
                    }
                }
            }
        }
        else if (direction == 2) {
            Toast.makeText(this@GameActivity, "Up", Toast.LENGTH_SHORT).show()
            for (j in 0..< columns) {
                val sltr = glayout.findViewWithTag<Button>(j).text
                for (i in 0..< rows) {
                    if (i < rows - 1) {
                        //glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>((i + 1) * columns + j).text
                        if (glayout.findViewWithTag<Button>((i + 1) * columns + j).text[0] == 'd')
                            glayout.findViewWithTag<Button>(i * columns + j).text = "_"
                        else if (glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'd' && glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'i')
                            glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>((i + 1) * columns + j).text
                    }
                    else {
                        if (sltr[0] == 'd') {
                            glayout.findViewWithTag<Button>(i * columns + j).text = "_"
                        }
                        else if (glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'd' && glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(i * columns + j).text = sltr.toString()
                        }
                    }
                }
            }
        }
        else if (direction == -2) {
            Toast.makeText(this@GameActivity, "Down", Toast.LENGTH_SHORT).show()
            for (j in 0..< columns) {
                val sltr = glayout.findViewWithTag<Button>((rows - 1) * columns + j).text
                for (i in (rows - 1) downTo 0) {
                    if (i > 0) {
                        //glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>((i - 1) * columns + j).text
                        if (glayout.findViewWithTag<Button>((i - 1) * columns + j).text[0] == 'd')
                            glayout.findViewWithTag<Button>(i * columns + j).text = "_"
                        else if (glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'd' && glayout.findViewWithTag<Button>(i * columns + j).text[0] != 'i')
                            glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>((i - 1) * columns + j).text
                    }
                    else {
                        if (sltr[0] == 'd') {
                            glayout.findViewWithTag<Button>(j).text = "_"
                        }
                        else if (glayout.findViewWithTag<Button>(j).text[0] != 'd' && glayout.findViewWithTag<Button>(j).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(j).text = sltr.toString()
                        }
                    }
                }
            }
        }
    }

    //Cycles all letters in a single row or column towards or away from a specified letter's
    //position. 'k' uses this to move one letter to the grid's edge then move the rest closer
    //to it. 'g' will use this to do the exact opposite.
    //Also used by 'l' and 'm' which work similar to 'k' and 'g' respectively.
    //Note a special case where kicking or grabbing a 'd' deletes everything between the
    //destination and starting point.
    private fun cycleGridAboutCoord(glayout: GridLayout, rows : Int, columns : Int, coord: Int, goal: Int, direction: Int, inwards : Boolean){
        if (direction == 1) {
            var drow = false
            if (inwards) {
                var ltr = glayout.findViewWithTag<Button>(coord + 1).text
                if (ltr[0] == 'd') {
                    drow = true
                }
                for (i in columns - (coord % columns) - 1 downTo 1) {
                    if (drow) {
                        if (i == columns - (coord % columns) - 1) {
                            glayout.findViewWithTag<Button>(coord + i).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord + i).text[0] != 'i'){
                            glayout.findViewWithTag<Button>(coord + i).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord + i).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord + i).text = ltr.toString()
                            ltr = tmp
                        }
                        else  if (tmp[0] == 'd'){
                            ltr = "_"
                        }
                    }
                }
            }
            else {
                var ltr = glayout.findViewWithTag<Button>(goal).text
                if (ltr[0] == 'd') {
                    drow = true
                }
                for (i in 1.. goal - coord) {
                    if (drow) {
                        if (i == 1) {
                            glayout.findViewWithTag<Button>(coord + i).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord + i).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord + i).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord + i).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord + i).text = ltr.toString()
                            ltr = tmp
                        }
                        else if (tmp[0] == 'd') {
                            ltr = "_"
                        }
                    }
                }
            }
        }
        else if (direction == -1) {

            if (inwards) {
                var ltr = glayout.findViewWithTag<Button>(coord - 1).text
                var drow = false
                if (ltr[0] == 'd') {
                    drow = true
                }
                for (i in (coord % columns) downTo 1) {
                    if (drow) {
                        if (i == (coord % columns)) {
                            glayout.findViewWithTag<Button>(coord - i).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord - i).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord - i).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord - i).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord - i).text = ltr.toString()
                            ltr = tmp
                        }
                        else if (tmp[0] == 'd'){
                            ltr = "_"
                        }
                    }
                }
            }
            else {
                var ltr = glayout.findViewWithTag<Button>(goal).text
                var drow = false
                if (ltr[0] == 'd') {
                    drow = true
                }
                for (i in 1.. coord - goal) {
                    if (drow) {
                        if (i == 1) {
                            glayout.findViewWithTag<Button>(coord - i).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord - i).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord - i).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord - i).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord - i).text = ltr.toString()
                            ltr = tmp
                        }
                        else if (tmp[0] == 'd'){
                            ltr = "_"
                        }
                    }
                }
            }
        }
        else if (direction == 2) {

            if (inwards) {
                var ltr = glayout.findViewWithTag<Button>(coord - columns).text
                var dcol = false
                if (ltr[0] == 'd') {
                    dcol = true
                }
                for (i in (coord / columns) downTo 1) {
                    if (dcol) {
                        if (i == (coord / columns)) {
                            glayout.findViewWithTag<Button>(coord - i * columns).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord - i * columns).text[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord - i * columns).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord - i * columns).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord - i * columns).text = ltr.toString()
                            ltr = tmp
                        }
                        else if (tmp[0] != 'i'){
                            ltr = "_"
                        }
                    }
                }
            }
            else {
                var ltr = glayout.findViewWithTag<Button>(goal).text
                var dcol = false
                if (ltr[0] == 'd') {
                    dcol = true
                }
                for (i in 1..coord / columns - goal / columns) {
                    if (dcol) {
                        if (i == 1) {
                            glayout.findViewWithTag<Button>(coord - i * columns).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord - i * columns).text[0] != 'i'){
                            glayout.findViewWithTag<Button>(coord - i * columns).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord - i * columns).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord - i * columns).text = ltr.toString()
                            ltr = tmp
                        }
                        else if (tmp[0] == 'd') {
                            ltr = "_"
                        }
                    }
                }
            }
        }
        else if (direction == -2) {
            if (inwards) {
                var ltr = glayout.findViewWithTag<Button>(coord + columns).text
                var dcol = false
                if (ltr[0] == 'd') {
                    dcol = true
                }
                for (i in rows - (coord / columns) - 1 downTo 1) {
                    if (dcol) {
                        if (i == rows - (coord / columns) + 1) {
                            glayout.findViewWithTag<Button>(coord + i * columns).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord + i * columns).text[0] != 'i'){
                            glayout.findViewWithTag<Button>(coord + i * columns).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord + i * columns).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord + i * columns).text = ltr.toString()
                            ltr = tmp
                        }
                        else if (tmp[0] == 'd') {
                            ltr = "_"
                        }
                    }
                }
            }
            else {
                var ltr = glayout.findViewWithTag<Button>(goal).text
                var dcol = false
                if (ltr[0] == 'd') {
                    dcol = true
                }
                for (i in 1..goal / columns - coord / columns) {
                    if (dcol) {
                        if (i == 1) {
                            glayout.findViewWithTag<Button>(coord + i * columns).text = ltr.toString()
                        }
                        else if (glayout.findViewWithTag<Button>(coord + i * columns).text[0] != 'i'){
                            glayout.findViewWithTag<Button>(coord + i * columns).text = "_"
                        }
                    }
                    else {
                        val tmp = glayout.findViewWithTag<Button>(coord + i * columns).text
                        if (tmp[0] != 'd' && tmp[0] != 'i') {
                            glayout.findViewWithTag<Button>(coord + i * columns).text = ltr.toString()
                            ltr = tmp
                        }
                        else if (tmp[0] == 'd'){
                            ltr = "_"
                        }
                    }
                }
            }
        }
    }

    //Makes all interactable letters interactable
    //For reference, the only non-interactable letters are '_', 'a', 'd', and 'i'
    private fun resetGridVis(glayout : GridLayout, rows : Int, columns : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                val ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
                glayout.findViewWithTag<Button>(i * columns + j).isEnabled = (ltr != '_' && ltr != 'a' && ltr != 'd' && ltr != 'i')
            }
        }
    }

    private fun winCheck(glayout : GridLayout, rows : Int, columns : Int) : Boolean {
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
                        if (compH < ltr) {
                            upH = -1
                            compH = ltr
                        }
                        else if (compH > ltr) {
                            upH = 1
                            compH = ltr
                        }
                    }
                    else if (upH == -1) {
                        if (compH > ltr) {
                            //Toast.makeText(this@GameActivity, "up then down", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                    else{
                        if (compH < ltr) {
                            //Toast.makeText(this@GameActivity, "down then up", Toast.LENGTH_SHORT).show()
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
                        if (compH < ltr) {
                            upH = -1
                            compH = ltr
                        }
                        else if (compH > ltr) {
                            upH = 1
                            compH = ltr
                        }
                    }
                    else if (upH == -1) {
                        if (compH > ltr) {
                            //Toast.makeText(this@GameActivity, "up then down", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                    else{
                        if (compH < ltr) {
                            //Toast.makeText(this@GameActivity, "down then up", Toast.LENGTH_SHORT).show()
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