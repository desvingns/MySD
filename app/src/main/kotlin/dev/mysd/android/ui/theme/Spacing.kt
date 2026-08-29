package dev.mysd.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

object BattleMetrics {
    val minTouchTarget: Dp = 48.dp
    val edgeControlSize: Dp = 56.dp
    val hudInset: Dp = 16.dp
    val controlGap: Dp = 8.dp
}

object CampaignMetrics {
    val minTouchTarget: Dp = 48.dp
    val contentInset: Dp = 24.dp
    val cardPadding: Dp = 16.dp
    val sectionGap: Dp = 16.dp
    val bottomRouteHeight: Dp = 72.dp
}

object RosterMetrics {
    val minTouchTarget: Dp = 48.dp
    val contentInset: Dp = 24.dp
    val cardPadding: Dp = 16.dp
    val cardGap: Dp = 12.dp
    val routeHeight: Dp = 72.dp
    val routeItemMinWidth: Dp = 80.dp
}
