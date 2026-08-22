package com.pixeldialer.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A single palette describing everything the UI needs, so screens never
 * hardcode colors — they always pull from LocalDialerPalette.current.
 */
data class DialerPalette(
    val id: String,
    val displayName: String,
    val isDark: Boolean,
    val background: Brush,
    val solidBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val navBackground: Color,
    val accent: Color,
    val accentSoft: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val danger: Color,
    val searchBackground: Color,
    val avatarBackground: Color,
    val callGreen: Color,
    val swatchStart: Color,
    val swatchEnd: Color
)

/** 1. Gradient — teal/mint, matches the reference screenshot */
val GradientPalette = DialerPalette(
    id = "gradient",
    displayName = "Gradient",
    isDark = false,
    background = Brush.verticalGradient(
        colors = listOf(Color(0xFFE8FBF5), Color(0xFFB9EDE0), Color(0xFF7FD8C9), Color(0xFF52C4B0))
    ),
    solidBackground = Color(0xFFB9EDE0),
    cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.92f),
    cardBorder = Color(0xFFFFFFFF).copy(alpha = 0.6f),
    navBackground = Color(0xFFFFFFFF).copy(alpha = 0.88f),
    accent = Color(0xFF0F8A7C),
    accentSoft = Color(0xFFDCF5EE),
    textPrimary = Color(0xFF0B2E28),
    textSecondary = Color(0xFF5B7C76),
    danger = Color(0xFFE0442E),
    searchBackground = Color(0xFFFFFFFF).copy(alpha = 0.95f),
    avatarBackground = Color(0xFFCFEFE6),
    callGreen = Color(0xFF34C759),
    swatchStart = Color(0xFFB9EDE0),
    swatchEnd = Color(0xFF0F8A7C)
)

/** 2. Midnight — pure black solid, iPhone-style dark mode */
val MidnightPalette = DialerPalette(
    id = "midnight",
    displayName = "Midnight",
    isDark = true,
    background = Brush.verticalGradient(colors = listOf(Color(0xFF0B0B0F), Color(0xFF0B0B0F))),
    solidBackground = Color(0xFF0B0B0F),
    cardBackground = Color(0xFF1C1C22),
    cardBorder = Color(0xFF2A2A32),
    navBackground = Color(0xFF17171C),
    accent = Color(0xFF32D74B),
    accentSoft = Color(0xFF173321),
    textPrimary = Color(0xFFF5F5F7),
    textSecondary = Color(0xFF8E8E96),
    danger = Color(0xFFFF453A),
    searchBackground = Color(0xFF1C1C22),
    avatarBackground = Color(0xFF2A2A32),
    callGreen = Color(0xFF34C759),
    swatchStart = Color(0xFF1C1C22),
    swatchEnd = Color(0xFF32D74B)
)

/** 3. Ocean Blue — matches the uploaded reference icon vibe */
val OceanPalette = DialerPalette(
    id = "ocean",
    displayName = "Ocean Blue",
    isDark = false,
    background = Brush.verticalGradient(
        colors = listOf(Color(0xFFE4F3FF), Color(0xFFBFE0FF), Color(0xFF7FBFFA), Color(0xFF4A7FE8))
    ),
    solidBackground = Color(0xFFBFE0FF),
    cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.94f),
    cardBorder = Color(0xFFFFFFFF).copy(alpha = 0.65f),
    navBackground = Color(0xFFFFFFFF).copy(alpha = 0.9f),
    accent = Color(0xFF1E6FE8),
    accentSoft = Color(0xFFDCEBFF),
    textPrimary = Color(0xFF0B1E42),
    textSecondary = Color(0xFF5A6E93),
    danger = Color(0xFFE0442E),
    searchBackground = Color(0xFFFFFFFF).copy(alpha = 0.96f),
    avatarBackground = Color(0xFFD3E7FF),
    callGreen = Color(0xFF34C759),
    swatchStart = Color(0xFF6FC7FA),
    swatchEnd = Color(0xFF2C56E8)
)

/** 4. Sunset — warm coral/orange gradient */
val SunsetPalette = DialerPalette(
    id = "sunset",
    displayName = "Sunset",
    isDark = false,
    background = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF3E4), Color(0xFFFFD9B8), Color(0xFFFFA98C), Color(0xFFF06B5E))
    ),
    solidBackground = Color(0xFFFFD9B8),
    cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.93f),
    cardBorder = Color(0xFFFFFFFF).copy(alpha = 0.6f),
    navBackground = Color(0xFFFFFFFF).copy(alpha = 0.88f),
    accent = Color(0xFFE0523C),
    accentSoft = Color(0xFFFFE4DA),
    textPrimary = Color(0xFF3A1A12),
    textSecondary = Color(0xFF8A6154),
    danger = Color(0xFFD32E1F),
    searchBackground = Color(0xFFFFFFFF).copy(alpha = 0.95f),
    avatarBackground = Color(0xFFFFE0CF),
    callGreen = Color(0xFF34C759),
    swatchStart = Color(0xFFFFC08C),
    swatchEnd = Color(0xFFE0523C)
)

/** 5. Violet — deep purple solid dark theme, alternative to Midnight */
val VioletPalette = DialerPalette(
    id = "violet",
    displayName = "Violet",
    isDark = true,
    background = Brush.verticalGradient(colors = listOf(Color(0xFF120D1F), Color(0xFF120D1F))),
    solidBackground = Color(0xFF120D1F),
    cardBackground = Color(0xFF211A34),
    cardBorder = Color(0xFF352A54),
    navBackground = Color(0xFF1A1428),
    accent = Color(0xFFB18CFF),
    accentSoft = Color(0xFF2E2350),
    textPrimary = Color(0xFFF3EEFF),
    textSecondary = Color(0xFF9C90BF),
    danger = Color(0xFFFF6178),
    searchBackground = Color(0xFF211A34),
    avatarBackground = Color(0xFF352A54),
    callGreen = Color(0xFF34C759),
    swatchStart = Color(0xFF211A34),
    swatchEnd = Color(0xFFB18CFF)
)

/** 6. Rose Gold — soft pink/cream light theme */
val RoseGoldPalette = DialerPalette(
    id = "rosegold",
    displayName = "Rose Gold",
    isDark = false,
    background = Brush.verticalGradient(
        colors = listOf(Color(0xFFFFF6F3), Color(0xFFFFE1DA), Color(0xFFFFC2C2), Color(0xFFE89A9A))
    ),
    solidBackground = Color(0xFFFFE1DA),
    cardBackground = Color(0xFFFFFFFF).copy(alpha = 0.93f),
    cardBorder = Color(0xFFFFFFFF).copy(alpha = 0.6f),
    navBackground = Color(0xFFFFFFFF).copy(alpha = 0.88f),
    accent = Color(0xFFC96B6B),
    accentSoft = Color(0xFFFFE9E5),
    textPrimary = Color(0xFF3D1F1F),
    textSecondary = Color(0xFF8F6C6C),
    danger = Color(0xFFCB3B3B),
    searchBackground = Color(0xFFFFFFFF).copy(alpha = 0.95f),
    avatarBackground = Color(0xFFFFDCD5),
    callGreen = Color(0xFF34C759),
    swatchStart = Color(0xFFFFC9C0),
    swatchEnd = Color(0xFFC96B6B)
)

/** 7. Dark Mode — pure AMOLED black, zero-gradient, battery-saving OLED style */
val DarkModePalette = DialerPalette(
    id = "darkmode",
    displayName = "Dark Mode",
    isDark = true,
    background = Brush.verticalGradient(colors = listOf(Color(0xFF000000), Color(0xFF000000))),
    solidBackground = Color(0xFF000000),
    cardBackground = Color(0xFF121212),
    cardBorder = Color(0xFF232323),
    navBackground = Color(0xFF0A0A0A),
    accent = Color(0xFF4FA8FF),
    accentSoft = Color(0xFF12233A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9A9A9A),
    danger = Color(0xFFFF5449),
    searchBackground = Color(0xFF121212),
    avatarBackground = Color(0xFF232323),
    callGreen = Color(0xFF34C759),
    swatchStart = Color(0xFF000000),
    swatchEnd = Color(0xFF4FA8FF)
)

val AllPalettes = listOf(
    GradientPalette, OceanPalette, SunsetPalette, RoseGoldPalette, MidnightPalette, VioletPalette, DarkModePalette
)

fun paletteById(id: String): DialerPalette = AllPalettes.find { it.id == id } ?: GradientPalette

