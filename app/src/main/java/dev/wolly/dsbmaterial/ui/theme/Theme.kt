package dev.wolly.dsbmaterial.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Applies the M3 Expressive theme the same way the M3e_Sample_App showcase does:
 * [MaterialExpressiveTheme] with an expressive [MotionScheme], a seed-derived color scheme
 * (warm cream surfaces) and the real 30-style type scale. Under dynamic color (§3.3) the
 * scheme adapts to the wallpaper like the showcase.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DSBMaterialTheme(
    themeIndex: Int = 0,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    useCustomFont: Boolean = false,
    fontRond: Float = 0f,
    useExpressiveMotion: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val palette = SeedPalettes[themeIndex.coerceIn(SeedPalettes.indices)]
    var colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            else -> palette.scheme(darkTheme)
        }
    if (amoledMode && darkTheme) {
        colorScheme = colorScheme.asAmoled()
    }
    val motionScheme =
        if (useExpressiveMotion) MotionScheme.expressive() else MotionScheme.standard()
    val typography = buildTypography(
        useCustomFont = useCustomFont,
        rond = fontRond
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = motionScheme,
        typography = typography,
        shapes = Shapes,
        content = content
    )
}

private fun ColorScheme.asAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF171717),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF111111),
    surfaceContainer = Color(0xFF161616),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF232323)
)
