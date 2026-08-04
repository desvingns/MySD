package dev.mysd.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Default Material 3 indigo seed; replace only after an accepted visual-fit decision.
private val Indigo40 = Color(0xFF6750A4)
private val Indigo80 = Color(0xFFD0BCFF)
private val IndigoGrey40 = Color(0xFF625B71)
private val IndigoGrey80 = Color(0xFFCCC2DC)
private val IndigoPink40 = Color(0xFF7D5260)
private val IndigoPink80 = Color(0xFFEFB8C8)

val LightColorScheme = lightColorScheme(
    primary = Indigo40,
    secondary = IndigoGrey40,
    tertiary = IndigoPink40,
)

val DarkColorScheme = darkColorScheme(
    primary = Indigo80,
    secondary = IndigoGrey80,
    tertiary = IndigoPink80,
)
