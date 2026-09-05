package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
  primary = NeonCyan,
  onPrimary = ObsidianBlack,
  primaryContainer = CardSurfaceVariant,
  onPrimaryContainer = NeonCyan,
  secondary = NeonEmerald,
  onSecondary = ObsidianBlack,
  secondaryContainer = CardSurfaceVariant,
  onSecondaryContainer = NeonEmerald,
  tertiary = OpticPurple,
  onTertiary = ObsidianBlack,
  tertiaryContainer = CardSurfaceVariant,
  onTertiaryContainer = OpticPurple,
  background = ObsidianBlack,
  onBackground = TextPrimary,
  surface = SurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = CardSurface,
  onSurfaceVariant = TextSecondary,
  error = LaserCrimson,
  onError = TextPrimary,
  errorContainer = LaserCrimsonSubdued,
  onErrorContainer = TextPrimary,
  outline = CardBorder,
  outlineVariant = GridLine
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}
