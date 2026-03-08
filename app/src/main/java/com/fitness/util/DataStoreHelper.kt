package com.fitness.util

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore by preferencesDataStore(name = "fitness_settings")
