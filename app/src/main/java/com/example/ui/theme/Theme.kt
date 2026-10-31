package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
  primary = DarkEmeraldPrimary,
  onPrimary = DarkEmeraldOnPrimary,
  primaryContainer = DarkEmeraldPrimaryContainer,
  onPrimaryContainer = DarkEmeraldOnPrimaryContainer,
  secondary = DarkGoldSecondary,
  onSecondary = DarkGoldOnSecondary,
  secondaryContainer = DarkGoldSecondaryContainer,
  onSecondaryContainer = DarkGoldOnSecondaryContainer,
  tertiary = SageTertiary,
  onTertiary = SageOnTertiary,
  tertiaryContainer = Color(0xFF1B4D3E),
  onTertiaryContainer = Color(0xFFD6F0E0),
  background = DarkBackground,
  onBackground = DarkOnSurface,
  surface = DarkSurface,
  onSurface = DarkOnSurface,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
  primary = EmeraldPrimary,
  onPrimary = EmeraldOnPrimary,
  primaryContainer = EmeraldPrimaryContainer,
  onPrimaryContainer = EmeraldOnPrimaryContainer,
  secondary = GoldSecondary,
  onSecondary = GoldOnSecondary,
  secondaryContainer = GoldSecondaryContainer,
  onSecondaryContainer = GoldOnSecondaryContainer,
  tertiary = SageTertiary,
  onTertiary = SageOnTertiary,
  tertiaryContainer = SageTertiaryContainer,
  onTertiaryContainer = SageOnTertiaryContainer,
  background = ParchmentBackground,
  onBackground = ParchmentOnSurface,
  surface = ParchmentSurface,
  onSurface = ParchmentOnSurface,
  surfaceVariant = ParchmentSurfaceVariant,
  onSurfaceVariant = ParchmentOnSurfaceVariant
)

@Composable
fun QuranAppTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep distinct Islamic emerald-gold branding consistent
  autoScaling: Boolean = true,
  content: @Composable () -> Unit,
) {
  val configuration = LocalConfiguration.current
  val screenWidthDp = configuration.screenWidthDp

  val scaleFactor = remember(autoScaling, screenWidthDp) {
    if (autoScaling) {
      when {
        screenWidthDp < 360 -> 0.85f
        screenWidthDp < 400 -> 0.92f
        screenWidthDp < 600 -> 0.98f
        screenWidthDp < 840 -> 1.04f
        else -> 1.08f
      }
    } else {
      1.0f
    }
  }

  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val responsiveTypography = remember(scaleFactor) {
    getResponsiveTypography(scaleFactor)
  }

  CompositionLocalProvider(
    LocalAutoScalingEnabled provides autoScaling,
    LocalResponsiveScaleFactor provides scaleFactor
  ) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = responsiveTypography,
      content = content
    )
  }
}
