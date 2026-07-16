package com.baldae.letterlab.data

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Loads the level catalog from `assets/levels.json`. Levels live in data, not
 * code, so new worlds and levels can be added without touching the engine.
 */
class LevelRepository(private val context: Context) {

    val catalog: LevelCatalog by lazy {
        val text = context.assets.open(LEVELS_ASSET).bufferedReader().use { it.readText() }
        parse(text)
    }

    companion object {
        const val LEVELS_ASSET = "levels.json"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): LevelCatalog = json.decodeFromString<LevelCatalog>(text)
    }
}
