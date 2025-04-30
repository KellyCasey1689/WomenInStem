package com.kellycasey.womeninstem.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary            = PrimaryPink,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryPinkVariant,
    secondary          = SecondaryMauve,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryMauveVariant,
    background         = BackgroundWhite,
    onBackground       = OnBackground,
    surface            = SurfaceWhite,
    onSurface          = OnBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary            = PrimaryPinkVariant,
    onPrimary          = OnPrimary,
    secondary          = SecondaryMauveVariant,
    onSecondary        = OnSecondary,
    background         = CharcoalGrey,
    onBackground       = BackgroundWhite,
    surface            = CharcoalGrey,
    onSurface          = BackgroundWhite,
)

@Composable
fun WomenInStemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    // Set status bar to match
    (context as? Activity)?.window?.statusBarColor = colorScheme.primaryContainer.toArgb()

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = Typography,
        content      = content
    )
}
