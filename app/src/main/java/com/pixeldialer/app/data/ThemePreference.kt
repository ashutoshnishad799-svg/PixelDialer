package com.pixeldialer.app.data

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pixel_dialer_prefs")

/**
 * Theme id maps 1:1 to DialerPalette.id (see DialerColors.kt), plus one
 * special value: "auto" — which means "follow the system's light/dark
 * setting" rather than a fixed palette. That resolution happens where the
 * theme is actually applied (see PixelDialerTheme in Theme.kt), since
 * checking system dark-mode is a @Composable-only API and doesn't belong
 * in this DataStore layer.
 */
class ThemePreference(private val context: Context) {
    private val key = stringPreferencesKey("theme_id")

    val themeIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[key] ?: "gradient"
    }

    suspend fun setTheme(themeId: String) {
        context.dataStore.edit { it[key] = themeId }
    }
}

