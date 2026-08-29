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

// Original launch composition tokens. These stay separate from the Material seed so the
// campaign opener can carry its own visual identity without changing other routes.
val LaunchBackground = Color(0xFF08152F)
val LaunchBackgroundMid = Color(0xFF122C4B)
val LaunchHorizon = Color(0xFF1D5A65)
val LaunchPanel = Color(0xE6152743)
val LaunchAccent = Color(0xFFFFC857)
val LaunchGlow = Color(0xFF6DE3D7)
val LaunchOnBackground = Color(0xFFF4F8FF)
val LaunchOnPanel = Color(0xFFF6F8FF)

// Semantic aliases for the original active-battle composition. Keeping these separate from
// launch call sites lets the battlefield evolve without changing the approved launch tokens.
val BattleBackground = LaunchBackground
val BattleFieldMid = LaunchBackgroundMid
val BattleHorizon = LaunchHorizon
val BattleHud = LaunchPanel
val BattleAction = LaunchAccent
val BattleBase = LaunchGlow
val BattleEnemy = IndigoPink40
val BattleOnBackground = LaunchOnBackground
val BattleOnHud = LaunchOnPanel

// Semantic tokens for the original campaign-selection shell. These remain isolated from
// launch and battle call sites so campaign composition can evolve without coupling routes.
val CampaignBackground = Color(0xFF0B1935)
val CampaignSurface = Color(0xE61A3150)
val CampaignAccent = Color(0xFFFFC857)
val CampaignSupport = Color(0xFF6DE3D7)
val CampaignOnBackground = Color(0xFFF4F8FF)
val CampaignOnSurface = Color(0xFFF6F8FF)
val CampaignDisabled = Color(0xFF8793A6)

// Semantic tokens for the original illustrated roster composition. These remain isolated
// from campaign route tokens so roster cards and controls can evolve independently.
val RosterBackground = Color(0xFF0A1730)
val RosterSurface = Color(0xE6172F4D)
val RosterCard = Color(0xFF21445D)
val RosterAccent = Color(0xFFFFCF5C)
val RosterSupport = Color(0xFF76E0D3)
val RosterOnBackground = Color(0xFFF4F8FF)
val RosterOnSurface = Color(0xFFF6F8FF)
val RosterDisabled = Color(0xFF8793A6)
val RosterRouteInactive = Color(0xFF31506A)

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
