package com.inweb.browser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// iNWEB brand palette — teal accent family (Material 3 color roles).
//
// §49 contrast (PHASE10 §3.4, WCAG AA 4.5:1): teal-600 (0xFF00897B)
// measured only 4.22:1 as text on the light background and 4.34:1 for
// white onPrimary buttons — below AA. Teal-700 (0xFF00796B) measures
// 5.16:1 (text on background) and 5.28:1 (white on primary); the dark
// theme primary measures 9.8:1 (audit finding A-2, unchanged).
private val INWEB_PRIMARY = Color(0xFF00796B)
private val INWEB_ON_PRIMARY = Color(0xFFFFFFFF)
private val INWEB_PRIMARY_CONTAINER = Color(0xFFB2DFDB)
private val INWEB_ON_PRIMARY_CONTAINER = Color(0xFF00201C)
private val INWEB_SECONDARY = Color(0xFF4A635F)
private val INWEB_BACKGROUND = Color(0xFFFBFDFA)
private val INWEB_SURFACE = Color(0xFFFBFDFA)
private val INWEB_SURFACE_VARIANT = Color(0xFFDBE5E1)
private val INWEB_ON_SURFACE_VARIANT = Color(0xFF3F4945)

private val INWEB_PRIMARY_DARK = Color(0xFF4FDAC8)
private val INWEB_ON_PRIMARY_DARK = Color(0xFF003731)
private val INWEB_PRIMARY_CONTAINER_DARK = Color(0xFF005048)
private val INWEB_ON_PRIMARY_CONTAINER_DARK = Color(0xFF6FF7E3)
private val INWEB_BACKGROUND_DARK = Color(0xFF191C1B)
private val INWEB_SURFACE_DARK = Color(0xFF191C1B)
private val INWEB_SURFACE_VARIANT_DARK = Color(0xFF3F4945)
private val INWEB_ON_SURFACE_VARIANT_DARK = Color(0xFFBEC9C4)

private val LightColors = lightColorScheme(
    primary = INWEB_PRIMARY,
    onPrimary = INWEB_ON_PRIMARY,
    primaryContainer = INWEB_PRIMARY_CONTAINER,
    onPrimaryContainer = INWEB_ON_PRIMARY_CONTAINER,
    secondary = INWEB_SECONDARY,
    background = INWEB_BACKGROUND,
    surface = INWEB_SURFACE,
    surfaceVariant = INWEB_SURFACE_VARIANT,
    onSurfaceVariant = INWEB_ON_SURFACE_VARIANT,
)

private val DarkColors = darkColorScheme(
    primary = INWEB_PRIMARY_DARK,
    onPrimary = INWEB_ON_PRIMARY_DARK,
    primaryContainer = INWEB_PRIMARY_CONTAINER_DARK,
    onPrimaryContainer = INWEB_ON_PRIMARY_CONTAINER_DARK,
    background = INWEB_BACKGROUND_DARK,
    surface = INWEB_SURFACE_DARK,
    surfaceVariant = INWEB_SURFACE_VARIANT_DARK,
    onSurfaceVariant = INWEB_ON_SURFACE_VARIANT_DARK,
)

/** iNWEB Material 3 theme with light/dark support (MASTER-SPEC §22, §37). */
@Composable
fun iNWEBTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
