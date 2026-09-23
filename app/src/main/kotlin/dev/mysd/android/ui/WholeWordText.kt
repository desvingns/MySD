package dev.mysd.android.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints

/**
 * Text that never splits a single word across lines.
 *
 * A Compose Dialog window supplies its own density, so its content follows the system font scale
 * even where an Activity-local font override is active. At a large system font one long word (for
 * example "Незавершённый" in headlineSmall on a 320dp display) can be wider than a compact dialog
 * and is then broken mid-word. The first of [style] and [fallbackStyles] in which every word fits
 * the measured width is used. The preferred style is kept whenever it fits, and ordinary wrapping
 * between words is unchanged. [modifier] is applied to the text node itself, so test tags and
 * semantics stay on the node that exposes the text.
 */
@Composable
fun WholeWordText(
    text: String,
    style: TextStyle,
    fallbackStyles: List<TextStyle>,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints {
        val availableWidth = constraints.maxWidth
        val chosen = remember(text, style, fallbackStyles, availableWidth, measurer) {
            val words = text.split(Whitespace).filter { it.isNotEmpty() }
            (listOf(style) + fallbackStyles).firstOrNull { candidate ->
                availableWidth == Constraints.Infinity || words.all { word ->
                    measurer.measure(word, candidate, softWrap = false, maxLines = 1).size.width <= availableWidth
                }
            } ?: fallbackStyles.lastOrNull() ?: style
        }
        Text(text = text, modifier = modifier, color = color, textAlign = textAlign, style = chosen)
    }
}

private val Whitespace = Regex("\\s+")
