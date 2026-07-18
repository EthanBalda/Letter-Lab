package com.baldae.letterlab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single app-wide preferences store backing all save data. [SaveMigration]
 * runs before the first read, remapping v1 (25-level) saves onto the v2
 * campaign's level ids.
 */
val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "letterlab",
    produceMigrations = { listOf(SaveMigration()) },
)
