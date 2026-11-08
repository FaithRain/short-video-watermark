package com.orange.nowatermark.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlin.math.hypot

/** 雾面渐变底 + 缓慢漂浮的低饱和光斑。 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora-phase"
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFEDF3FE), Color(0xFFF2ECFF), Color(0xFFFFF3E9))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x998FC8FF), Color.Transparent),
                    center = Offset(size.width * (0.1f + 0.08f * t), size.height * 0.08f),
                    radius = size.minDimension * 0.75f
                ),
                radius = size.minDimension * 0.75f,
                center = Offset(size.width * (0.1f + 0.08f * t), size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x99D9C7FF), Color.Transparent),
                    center = Offset(size.width * (0.95f - 0.1f * t), size.height * 0.42f),
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = Offset(size.width * (0.95f - 0.1f * t), size.height * 0.42f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x88FFD9B8), Color.Transparent),
                    center = Offset(size.width * 0.3f, size.height * (0.88f - 0.06f * t)),
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.3f, size.height * (0.88f - 0.06f * t))
            )
        }
        content()
    }
}

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White.copy(alpha = 0.42f),
    borderWidth: Dp = 1.dp,
    elevation: Dp = 16.dp
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color(0x1A33507A),
        spotColor = Color(0x2233507A)
    )
    .clip(shape)
    .background(tint)
    .border(
        width = borderWidth,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.95f),
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.55f)
            )
        ),
        shape = shape
    )

/** 玻璃面板，带一层顶部内高光。 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White.copy(alpha = 0.42f),
    elevation: Dp = 16.dp,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier.glass(shape, tint, elevation = elevation)) {
        Column(modifier = Modifier.padding(padding)) { content() }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.30f), Color.Transparent, Color.White.copy(alpha = 0.06f))
                    )
                )
        )
    }
}

/** 主按钮：白色液态玻璃，按下有体积收缩与手指落点涟漪。 */
@Composable
fun GlassPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = MutableInteractionSource()
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(18.dp)
    val ink = Color(0xFF0B1220)
    Box(
        modifier = modifier
            .height(54.dp)
            .shadow(
                elevation = if (pressed) 6.dp else 16.dp,
                shape = shape,
                ambientColor = Color(0x2233507A),
                spotColor = Color(0x3333507A)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (pressed) 0.74f else 0.82f),
                        Color.White.copy(alpha = if (pressed) 0.42f else 0.52f)
                    )
                )
            )
            .border(
                1.2.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 1f),
                        Color.White.copy(alpha = 0.50f),
                        Color(0xFF8FA9C9).copy(alpha = 0.50f)
                    )
                ),
                shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // 顶部高光
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.55f), Color.Transparent, Color.Transparent),
                        startY = 0f,
                        endY = 54f * 2f
                    )
                )
        )
        Box(
            Modifier
                .matchParentSize()
                .pressRipple(interaction, color = Color(0xFF0A6CFF), maxAlpha = 0.14f)
        )
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF0A6CFF)
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text,
                color = ink,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

/** 按下时从手指落点扩散的涟漪；半径覆盖到最远角。 */
@Composable
fun Modifier.pressRipple(
    interaction: MutableInteractionSource,
    color: Color = Color.White,
    maxAlpha: Float = 0.30f
): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Unspecified) }
    val progress = remember { Animatable(0f) }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) {
        if (pressed) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(560, easing = LinearEasing))
        } else {
            progress.animateTo(0f, tween(220))
        }
    }
    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.pressed }
                    if (change != null) origin = change.position
                }
            }
        }
        .drawWithContent {
            drawContent()
            if (progress.value > 0f) {
                val o = if (origin.isSpecified) origin else Offset(size.width / 2f, size.height / 2f)
                val maxR = listOf(
                    Offset(0f, 0f),
                    Offset(size.width, 0f),
                    Offset(0f, size.height),
                    Offset(size.width, size.height)
                ).maxOf { (it - o).getDistance() }
                drawCircle(
                    color = color.copy(alpha = maxAlpha * (1f - progress.value)),
                    radius = maxR * progress.value,
                    center = o
                )
            }
        }
}
