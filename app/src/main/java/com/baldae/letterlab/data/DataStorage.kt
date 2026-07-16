package com.baldae.letterlab.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single app-wide preferences store backing all save data. */
val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(name = "letterlab")
