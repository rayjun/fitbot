package com.fitness.util

import android.content.Context

/**
 * Maps a string key (like "ex_benchpress_name") to its corresponding Android resource ID.
 * This is used to maintain compatibility after moving models to commonMain where R.string is not available.
 */
fun String.toResId(context: Context): Int {
    return context.resources.getIdentifier(this, "string", context.packageName)
}
