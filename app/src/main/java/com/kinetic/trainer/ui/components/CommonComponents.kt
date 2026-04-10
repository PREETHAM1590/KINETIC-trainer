package com.kinetic.trainer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetic.trainer.domain.InsightSeverity
import com.kinetic.trainer.domain.TrainerInsight
import com.kinetic.trainer.ui.theme.Background
import com.kinetic.trainer.ui.theme.Lime
import com.kinetic.trainer.ui.theme.Surface1
import com.kinetic.trainer.ui.theme.Surface2
import com.kinetic.trainer.ui.theme.TextMuted
import com.kinetic.trainer.ui.theme.TextPrimary
import com.kinetic.trainer.ui.theme.Urgent
import com.kinetic.trainer.ui.theme.Warning

@Composable
fun KineticCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Surface1,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        content()
    }
}

@Composable
fun LimeBadge(text: String) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Lime)
    ) {
        Text(
            text = text,
            color = Background,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StartButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Lime,
            contentColor = Background
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
}

@Composable
fun ScreenHeadline(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            color = Lime,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            letterSpacing = (-1).sp
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 16.dp,
    trackColor: Color = Surface1,
    progressColor: Color = Lime,
    animationDurationMs: Int = 1200,
    content: @Composable () -> Unit = {}
) {
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing),
        label = "progressRing"
    )
    LaunchedEffect(progress) { targetProgress = progress }

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(strokePx, cap = StrokeCap.Round), size = Size(size.toPx(), size.toPx()))
            drawArc(color = progressColor, startAngle = -90f, sweepAngle = 360f * animatedProgress, useCenter = false,
                style = Stroke(strokePx, cap = StrokeCap.Round), size = Size(size.toPx(), size.toPx()))
        }
        content()
    }
}

@Composable
fun TrainerInsightBanner(
    insight: TrainerInsight,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bannerColor = when (insight.severity) {
        InsightSeverity.URGENT -> Urgent.copy(alpha = 0.15f)
        InsightSeverity.WARNING -> Warning.copy(alpha = 0.15f)
        InsightSeverity.INFO -> Lime.copy(alpha = 0.12f)
    }
    val accentColor = when (insight.severity) {
        InsightSeverity.URGENT -> Urgent
        InsightSeverity.WARNING -> Warning
        InsightSeverity.INFO -> Lime
    }

    KineticCard(modifier = modifier.fillMaxWidth(), containerColor = bannerColor) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (insight.severity != InsightSeverity.INFO) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.message,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                if (insight.actionLabel != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = onActionClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(text = insight.actionLabel, color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ClientAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val initial = name.firstOrNull()?.uppercaseChar() ?: '?'
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Surface2)
    ) {
        Text(
            text = initial.toString(),
            color = Lime,
            fontSize = (size.value * 0.4f).sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun SeverityBadge(severity: InsightSeverity) {
    val (label, color) = when (severity) {
        InsightSeverity.URGENT -> "URGENT" to Urgent
        InsightSeverity.WARNING -> "WARNING" to Warning
        InsightSeverity.INFO -> "INFO" to Lime
    }
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
