package com.baldae.finalproject

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
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

        //val levelInput: EditText = findViewById(R.id.levelInput)
        /* levelInput XML Code:
                <EditText
                android:id="@+id/levelInput"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Select Level"
                android:inputType="number"
                />
         */
        val levelList = arrayOf("aabab",
                                "aabab\naabab",
                                "ababa\nbacab\nbbcbb",
                                "aca")
        val generateButtonGrid: Button = findViewById(R.id.generateButtonGrid)
        val gridLayout: GridLayout = findViewById(R.id.gridLayout)
        var tog = false
        var cBtn = '_'
        var cTag = -1
        var level = 0

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

                                    }
                                    'h' -> {

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
                                }
                            }
                        }
                    }
                    gridLayout.addView(button)
                }
            }
        }


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
                if (ltr != '_' && ltr != 'a' && ltr != 'd' && ltr != 'i')
                    glayout.findViewWithTag<Button>(i * columns + j).isEnabled = true
                else
                    glayout.findViewWithTag<Button>(i * columns + j).isEnabled = false
            }
        }
    }

    fun gridVisG(glayout : GridLayout, rows : Int, columns : Int){

    }

    fun allGridVis(glayout : GridLayout, rows : Int, columns : Int) {
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                val ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
                if (ltr != '_' && ltr != 'i')
                    glayout.findViewWithTag<Button>(i * columns + j).isEnabled = true
            }
        }
    }

    fun winCheck(glayout : GridLayout, rows : Int, columns : Int) : Boolean {
        //Horizontal
        var ltr = '_'
        for (i in 0 until rows) {
            var compH = '_'
            var upH = 0
            for (j in 0 until columns) {
                ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
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
                ltr = glayout.findViewWithTag<Button>(i * columns + j).text[0]
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