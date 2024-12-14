package com.baldae.finalproject
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar

class GameActivity : ComponentActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private var level = 0

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_level)

        sharedPreferences = getSharedPreferences("GameProgress", MODE_PRIVATE)
        level = intent.getIntExtra("level", 0)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert) // Back arrow icon
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Navigate back to Level Select
        }

        /*val levelList = arrayOf("aabab",
                                "aabab\naabab",
                                "ababa\nbacab\nbbcbb",
                                "aba\nbcb\naba",
                                "abce\ncbca",
                                "abbab\nabbab\naafbb\naabbb\naabbb")*/
        /*
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
         */

        val levelList = arrayOf("abbab\nabbab\naafbb\nbabba\nbabba", "aca")
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
                                    'b', 'e', 'f', 'k', 'l', 'm' -> {
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
                                        else {
                                            gridLayout.findViewWithTag<Button>(cTag).text = "_"
                                            tog = false
                                            resetGridVis(gridLayout, rows, columns)
                                        }
                                    }
                                    'e' -> {
                                        gridLayout.findViewWithTag<Button>(cTag).text = text
                                        text = "_"
                                        tog = false
                                        resetGridVis(gridLayout, rows, columns)
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

                                    }
                                    'h' -> {

                                    }
                                    'k' -> {

                                    }
                                    'l' -> {

                                    }
                                    'm' -> {

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

    private fun markLevelComplete() {
        val completedLevels = sharedPreferences.getInt("completedLevels", 0)
        if (level == completedLevels) {
            sharedPreferences.edit().putInt("completedLevels", completedLevels + 1).apply()
            Toast.makeText(this, "Level ${level + 1} completed!", Toast.LENGTH_SHORT).show()
            finish() // Go back to the level select screen
        }
    }

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

    private fun showAllGridVis(glayout : GridLayout, rows : Int, columns : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                glayout.findViewWithTag<Button>(i * columns + j).isEnabled = true
            }
        }
    }

    private fun toggleGridVis(glayout : GridLayout, rows : Int, columns : Int){
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                glayout.findViewWithTag<Button>(i * columns + j).isEnabled = false
            }
        }
    }

    private fun cycleGridInDirection(glayout : GridLayout, rows : Int, columns : Int, direction : Int){
        //0 = no move, 1 = right, 2 = up, -1 = left, -2 = down
        if (direction == -1) {
            Toast.makeText(this@GameActivity, "Left", Toast.LENGTH_SHORT).show()
            for (i in 0..< rows) {
                val sltr = glayout.findViewWithTag<Button>(i * columns).text[0]
                for (j in 0..< columns) {
                    if (j < columns - 1)
                        glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>(i * columns + j + 1).text
                    else
                        glayout.findViewWithTag<Button>(i * columns + j).text = sltr.toString()
                }
            }
        }
        else if (direction == 1) {
            Toast.makeText(this@GameActivity, "Right", Toast.LENGTH_SHORT).show()
            for (i in 0..< rows) {
                val sltr = glayout.findViewWithTag<Button>(i * columns + columns - 1).text[0]
                for (j in (columns - 1) downTo 0) {
                    if (j > 0)
                        glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>(i * columns + j - 1).text
                    else
                        glayout.findViewWithTag<Button>(i * columns).text = sltr.toString()
                }
            }
        }
        else if (direction == 2) {
            Toast.makeText(this@GameActivity, "Up", Toast.LENGTH_SHORT).show()
            for (j in 0..< columns) {
                val sltr = glayout.findViewWithTag<Button>(j).text[0]
                for (i in 0..< rows) {
                    if (i < rows - 1)
                        glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>((i + 1) * columns + j).text
                    else
                        glayout.findViewWithTag<Button>(i * columns + j).text = sltr.toString()
                }
            }
        }
        else if (direction == -2) {
            Toast.makeText(this@GameActivity, "Down", Toast.LENGTH_SHORT).show()
            for (j in 0..< columns) {
                val sltr = glayout.findViewWithTag<Button>(rows * (columns - 1) + j).text[0]
                for (i in (rows - 1) downTo 0) {
                    if (i > 0)
                        glayout.findViewWithTag<Button>(i * columns + j).text = glayout.findViewWithTag<Button>((i - 1) * columns + j).text
                    else
                        glayout.findViewWithTag<Button>(j).text = sltr.toString()
                }
            }
        }
    }

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
                            Toast.makeText(this@GameActivity, "up then down", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                    else{
                        if (compH < ltr) {
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
                            Toast.makeText(this@GameActivity, "up then down", Toast.LENGTH_SHORT).show()
                            return false
                        }
                        else {
                            compH = ltr
                        }
                    }
                    else{
                        if (compH < ltr) {
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