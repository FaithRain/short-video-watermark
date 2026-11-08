package com.orange.nowatermark.ui.glass

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState

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
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.30f), Color.Transparent, Color.White.copy(alpha = 0.06f))
                    )
                )
        )
    }
}

/** 主按钮：蓝色玻璃，按下缩放。 */
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
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF2E8BFF), Color(0xFF0A6CFF))
                )
            )
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.6f), Color.White.copy(0.1f))), shape)
            .shadow(if (pressed) 6.dp else 18.dp, shape, spotColor = Color(0x550A6CFF))
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
