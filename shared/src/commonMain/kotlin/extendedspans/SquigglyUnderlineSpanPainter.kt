package extendedspans

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration.Companion.LineThrough
import androidx.compose.ui.text.style.TextDecoration.Companion.None
import androidx.compose.ui.text.style.TextDecoration.Companion.Underline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import extendedspans.internal.deserializeToColor
import extendedspans.internal.fastFirstOrNull
import extendedspans.internal.fastForEach
import extendedspans.internal.fastMapRange
import extendedspans.internal.serialize
import util.asPlatformPathRewind
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Draws squiggly underlines below text annotated using `SpanStyle(textDecoration = Underline)`.
 * Inspired from [Sam Ruston's BuzzKill app](https://twitter.com/saketme/status/1310073763019530242).
 *
 * ```
 *
 *       _....._                                     _....._         ▲
 *    ,="       "=.                               ,="       "=.   amplitude
 *  ,"             ".                           ,"             ".    │
 *,"                 ".,                     ,,"                 "., ▼
 *""""""""""|""""""""""|."""""""""|""""""""".|""""""""""|""""""""""|
 *                       ".               ."
 *                         "._         _,"
 *                            "-.....-"
 *◀─────────────── Wavelength ──────────────▶
 *
 * ```
 *
 * @param animator See [rememberSquigglyUnderlineAnimator].
 * @param bottomOffset Distance from a line's bottom coordinate.
 */
class SquigglyUnderlineSpanPainter(
    private val width: TextUnit = 2.sp,
    private val wavelength: TextUnit = 9.sp,
    private val amplitude: TextUnit = 1.sp,
    private val bottomOffset: TextUnit = 1.sp,
    private val animator: SquigglyUnderlineAnimator = SquigglyUnderlineAnimator.NoOp,
) : ExtendedSpanPainter() {
    private val path = Path()

    override fun decorate(
        span: SpanStyle,
        start: Int,
        end: Int,
        text: AnnotatedString,
        builder: AnnotatedString.Builder
    ): SpanStyle {
        val textDecoration = span.textDecoration
        return if (textDecoration == null || Underline !in textDecoration) {
            span
        } else {
            val textColor = text.spanStyles.fastFirstOrNull {
                // I don't think this predicate will work for text annotated with overlapping
                // multiple colors, but I'm not too interested in solving for that use case.
                it.start <= start && it.end >= end && it.item.color.isSpecified
            }?.item?.color ?: Color.Unspecified

            builder.addStringAnnotation(TAG, annotation = textColor.serialize(), start = start, end = end)
            span.copy(textDecoration = if (LineThrough in textDecoration) LineThrough else None)
        }
    }

    override fun drawInstructionsFor(layoutResult: TextLayoutResult): SpanDrawInstructions {
        val text = layoutResult.layoutInput.text
        val annotations = text.getStringAnnotations(TAG, start = 0, end = text.length)

        return SpanDrawInstructions {
            val pathStyle = Stroke(
                width = width.toPx(),
                join = StrokeJoin.Round,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.cornerPathEffect(radius = wavelength.toPx()), // For slightly smoother waves.
            )

            annotations.fastForEach { annotation ->
                val boxes = layoutResult.getBoundingBoxes(
                    startOffset = annotation.start,
                    endOffset = annotation.end
                )
                val textColor = annotation.item.deserializeToColor() ?: layoutResult.layoutInput.style.color
                boxes.fastForEach { box ->
                    path.asPlatformPathRewind()
                    path.buildSquigglesFor(box, density = this)
                    drawPath(
                        path = path,
                        color = textColor,
                        style = pathStyle
                    )
                }
            }
        }
    }

    /**
     * Maths copied from [squigglyspans](https://github.com/samruston/squigglyspans).
     */
    private fun Path.buildSquigglesFor(box: Rect, density: Density) = density.run {
        val lineStart = box.left + (width.toPx() / 2)
        val lineEnd = box.right - (width.toPx() / 2)
        val lineBottom = box.bottom + bottomOffset.toPx()

        val segmentWidth = wavelength.toPx() / SEGMENTS_PER_WAVELENGTH
        val numOfPoints = ceil((lineEnd - lineStart) / segmentWidth).toInt() + 1

        var pointX = lineStart
        fastMapRange(0, numOfPoints) { point ->
            val proportionOfWavelength = (pointX - lineStart) / wavelength.toPx()
            val radiansX = proportionOfWavelength * TWO_PI + (TWO_PI * animator.animationProgress.value)
            val offsetY = lineBottom + (sin(radiansX) * amplitude.toPx())

            when (point) {
                0 -> moveTo(pointX, offsetY)
                else -> lineTo(pointX, offsetY)
            }
            pointX = (pointX + segmentWidth).coerceAtMost(lineEnd)
        }
    }

    companion object {
        private const val TAG = "squiggly_underline_span"
        private const val SEGMENTS_PER_WAVELENGTH = 10
        private const val TWO_PI = 2 * PI.toFloat()
    }
}

@Composable
fun rememberSquigglyUnderlineAnimator(duration: Duration = 1.seconds): SquigglyUnderlineAnimator {
    val animationProgress = rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration.inWholeMilliseconds.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    return remember {
        SquigglyUnderlineAnimator(animationProgress)
    }
}

@Stable
class SquigglyUnderlineAnimator internal constructor(internal val animationProgress: State<Float>) {
    companion object {
        val NoOp = SquigglyUnderlineAnimator(animationProgress = mutableStateOf(0f))
    }
}
