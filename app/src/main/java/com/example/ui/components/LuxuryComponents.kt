package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CardSurfaceHigh
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ClaudeTerracotta
import com.example.ui.theme.ClaudeTerracottaSoft
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldBorderStrong
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GraphiteDivider
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MatteGold
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.SoftWhite
import com.example.ui.theme.SuccessContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory
import com.example.ui.viewmodel.InAppAlert
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.luxuryPressScale(
    interactionSource: MutableInteractionSource
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Custom geometric architectural emblem for Claude Control Center.
 * Distinct charcoal disc with matte-gold perimeter ring and refined terracotta-gold radial hub.
 */
@Composable
fun ClaudeControlMark(
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f

        drawCircle(
            color = ElevatedObsidian,
            radius = radius,
            center = center
        )
        drawCircle(
            color = GoldPrimary.copy(alpha = 0.55f),
            radius = radius - 1.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        val rayCount = 8
        val innerR = radius * 0.20f
        val outerR = radius * 0.58f
        for (i in 0 until rayCount) {
            val angleRad = (i * (360f / rayCount) - 90f) * (PI / 180f)
            val start = Offset(
                x = center.x + (innerR * cos(angleRad)).toFloat(),
                y = center.y + (innerR * sin(angleRad)).toFloat()
            )
            val end = Offset(
                x = center.x + (outerR * cos(angleRad)).toFloat(),
                y = center.y + (outerR * sin(angleRad)).toFloat()
            )
            drawLine(
                color = if (i % 2 == 0) ClaudeTerracotta else ChampagneGold,
                start = start,
                end = end,
                strokeWidth = (radius * 0.13f).coerceAtLeast(1.4.dp.toPx()),
                cap = StrokeCap.Round
            )
        }

        drawCircle(
            color = GoldPrimary,
            radius = radius * 0.15f,
            center = center
        )
    }
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = GraphiteDivider
    )
}

@Composable
fun GoldDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        GraphiteDivider.copy(alpha = 0.4f),
                        GoldHairline,
                        GraphiteDivider.copy(alpha = 0.4f)
                    )
                )
            )
    )
}

@Composable
fun LuxuryObsidianCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (highlighted) GoldBorderStrong else GraphiteDivider
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(VelvetCharcoal)
            .border(width = 1.dp, color = borderColor, shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GoldPillBadge(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color = CardSurfaceHigh,
    contentColor: Color = ChampagneGold,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = BorderStroke(0.75.dp, contentColor.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Clean architectural diagram of message routing between Control Center and Claude.
 */
@Composable
fun MessageFlowDiagram(
    isPaidApiMode: Boolean,
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "flow_packet")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "packet_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(ObsidianBlack)
            .border(1.dp, GraphiteDivider, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Text(
            text = "נתיב העברת ההודעה אל Claude",
            style = MaterialTheme.typography.labelMedium,
            color = ChampagneGold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height / 2f
                val startX = 24.dp.toPx()
                val endX = size.width - 24.dp.toPx()
                val midX = size.width / 2f

                drawLine(
                    color = GraphiteDivider,
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                if (isOnline) {
                    val packetX = endX - (endX - startX) * progress
                    drawCircle(
                        color = ClaudeTerracotta.copy(alpha = 0.28f),
                        radius = 7.dp.toPx(),
                        center = Offset(packetX, y)
                    )
                    drawCircle(
                        color = ChampagneGold,
                        radius = 3.5.dp.toPx(),
                        center = Offset(packetX, y)
                    )
                }

                val nodeColors = listOf(
                    GoldPrimary,
                    if (isPaidApiMode) EmeraldGoldSuccess else ClaudeTerracotta,
                    ChampagneGold
                )
                listOf(endX, midX, startX).forEachIndexed { idx, x ->
                    drawCircle(
                        color = ObsidianBlack,
                        radius = 8.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = nodeColors[idx],
                        radius = 8.dp.toPx(),
                        center = Offset(x, y),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = nodeColors[idx],
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1. הכנה במרכז",
                style = MaterialTheme.typography.labelSmall,
                color = SoftWhite
            )
            Text(
                text = if (isPaidApiMode) "2. API מאובטח" else "2. חשבון Claude חינמי",
                style = MaterialTheme.typography.labelSmall,
                color = if (isPaidApiMode) EmeraldGoldSuccess else ClaudeTerracotta
            )
            Text(
                text = "3. תגובה והתראה",
                style = MaterialTheme.typography.labelSmall,
                color = ChampagneGold
            )
        }
    }
}

data class DailyChartPoint(
    val label: String,
    val preparedCount: Int,
    val sentCount: Int,
    val repliesCount: Int
)

/**
 * Custom Canvas bar chart displaying real daily prepared, sent, and reply counts.
 */
@Composable
fun DailyActivityBarChart(
    points: List<DailyChartPoint>,
    modifier: Modifier = Modifier
) {
    val maxVal = (points.maxOfOrNull { maxOf(it.preparedCount, it.sentCount, it.repliesCount) } ?: 1)
        .coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            if (points.isEmpty()) return@Canvas
            val groupWidth = size.width / points.size
            val barWidth = (groupWidth * 0.22f).coerceIn(6.dp.toPx(), 16.dp.toPx())
            val spacing = 3.dp.toPx()

            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { frac ->
                val y = size.height * (1f - frac)
                drawLine(
                    color = GraphiteDivider,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            points.forEachIndexed { index, point ->
                val centerX = groupWidth * index + groupWidth / 2f
                val totalGroupW = barWidth * 3 + spacing * 2
                val startX = centerX - totalGroupW / 2f

                val values = listOf(
                    point.preparedCount to MutedWarmGray,
                    point.sentCount to GoldPrimary,
                    point.repliesCount to ClaudeTerracotta
                )
                values.forEachIndexed { barIdx, (value, color) ->
                    val ratio = value.toFloat() / maxVal.toFloat()
                    val barHeight = (size.height * ratio).coerceAtLeast(if (value > 0) 6.dp.toPx() else 2.dp.toPx())
                    val x = startX + barIdx * (barWidth + spacing)
                    val y = size.height - barHeight
                    drawRoundRect(
                        color = if (value > 0) color else color.copy(alpha = 0.18f),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedWarmGray,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendDot(color = MutedWarmGray, label = "הוכנו")
            LegendDot(color = GoldPrimary, label = "נשלחו")
            LegendDot(color = ClaudeTerracotta, label = "תשובות")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SoftWhite
        )
    }
}

@Composable
fun ClaudeThinkingIndicator(
    label: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "claude_thinking")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_rotation"
    )

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(VelvetCharcoal)
            .border(1.dp, GraphiteDivider, MaterialTheme.shapes.small)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            ClaudeTerracotta,
                            ChampagneGold,
                            Color.Transparent
                        )
                    ),
                    startAngle = rotation,
                    sweepAngle = 220f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            ClaudeControlMark(size = 20.dp)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = ChampagneGold
        )
    }
}

/**
 * Clean Markdown block renderer tailored for Claude responses:
 * headings, bullet & numbered lists, markdown tables, fenced code blocks (LTR with copy), and inline bold/code.
 */
@Composable
fun FormattedMarkdownContent(
    text: String,
    textColor: Color = WarmIvory,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }
    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            blocks.forEach { block ->
                when (block) {
                    is MarkdownBlock.CodeBlock -> CodeBlockCard(
                        language = block.language,
                        code = block.code
                    )
                    is MarkdownBlock.TableBlock -> MarkdownTableView(
                        headers = block.headers,
                        rows = block.rows
                    )
                    is MarkdownBlock.HeadingBlock -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                        Text(
                            text = parseInlineMarkdown(block.text, ChampagneGold),
                            style = style.copy(textDirection = TextDirection.Content),
                            color = ChampagneGold
                        )
                    }
                    is MarkdownBlock.BulletItem -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyLarge,
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = parseInlineMarkdown(block.text, textColor),
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    is MarkdownBlock.NumberedItem -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${block.number}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = parseInlineMarkdown(block.text, textColor),
                                style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    is MarkdownBlock.QuoteBlock -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .background(VelvetCharcoal, MaterialTheme.shapes.extraSmall)
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(ClaudeTerracotta)
                            )
                            Text(
                                text = parseInlineMarkdown(block.text, SoftWhite),
                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content),
                                color = SoftWhite,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    is MarkdownBlock.ParagraphBlock -> {
                        Text(
                            text = parseInlineMarkdown(block.text, textColor),
                            style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Content),
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockCard(
    language: String,
    code: String
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(ObsidianBlack)
            .border(1.dp, GraphiteDivider, MaterialTheme.shapes.small)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardSurfaceHigh)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language.ifBlank { "קוד" },
                style = MaterialTheme.typography.labelSmall,
                color = MutedWarmGray
            )
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(code))
                    Toast.makeText(context, "הקוד הועתק", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "העתק קוד",
                    tint = ChampagneGold,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "העתק קוד",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChampagneGold
                )
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        lineHeight = 20.sp,
                        textDirection = TextDirection.Ltr
                    ),
                    color = SoftWhite
                )
            }
        }
    }
}

@Composable
private fun MarkdownTableView(
    headers: List<String>,
    rows: List<List<String>>
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .border(1.dp, GraphiteDivider, MaterialTheme.shapes.small)
            .background(VelvetCharcoal)
            .horizontalScroll(scrollState)
    ) {
        if (headers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .background(CardSurfaceHigh)
                    .padding(vertical = 8.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                headers.forEach { cell ->
                    Text(
                        text = cell.trim(),
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampagneGold,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(128.dp)
                    )
                }
            }
            HairlineDivider()
        }
        rows.forEachIndexed { idx, row ->
            Row(
                modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val cellCount = maxOf(headers.size, row.size)
                for (colIdx in 0 until cellCount) {
                    val value = row.getOrElse(colIdx) { "" }.trim()
                    Text(
                        text = parseInlineMarkdown(value, WarmIvory),
                        style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Content),
                        color = WarmIvory,
                        modifier = Modifier.width(128.dp)
                    )
                }
            }
            if (idx < rows.lastIndex) {
                HairlineDivider()
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class HeadingBlock(val level: Int, val text: String) : MarkdownBlock
    data class ParagraphBlock(val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock
    data class QuoteBlock(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val lines = raw.replace("\r\n", "\n").split("\n")
    val result = mutableListOf<MarkdownBlock>()
    val paragraphBuffer = mutableListOf<String>()
    var i = 0

    fun flushParagraph() {
        if (paragraphBuffer.isNotEmpty()) {
            result.add(MarkdownBlock.ParagraphBlock(paragraphBuffer.joinToString("\n")))
            paragraphBuffer.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            flushParagraph()
            val lang = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            result.add(MarkdownBlock.CodeBlock(lang, codeLines.joinToString("\n")))
            i++
            continue
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            i++
            continue
        }

        // Markdown table detection: header line with pipes followed by separator line with ---
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && i + 1 < lines.size) {
            val sep = lines[i + 1].trim()
            if (sep.startsWith("|") && sep.contains("---")) {
                flushParagraph()
                val headers = trimmed.trim('|').split("|").map { it.trim() }
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (!rowLine.startsWith("|")) break
                    rows.add(rowLine.trim('|').split("|").map { it.trim() })
                    i++
                }
                result.add(MarkdownBlock.TableBlock(headers, rows))
                continue
            }
        }

        when {
            trimmed.startsWith("### ") -> {
                flushParagraph()
                result.add(MarkdownBlock.HeadingBlock(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                flushParagraph()
                result.add(MarkdownBlock.HeadingBlock(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                flushParagraph()
                result.add(MarkdownBlock.HeadingBlock(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("> ") -> {
                flushParagraph()
                result.add(MarkdownBlock.QuoteBlock(trimmed.removePrefix("> ").trim()))
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                flushParagraph()
                result.add(MarkdownBlock.BulletItem(trimmed.drop(2).trim()))
            }
            trimmed.matches(Regex("""^\d+\.\s+.+""")) -> {
                flushParagraph()
                val dotIdx = trimmed.indexOf('.')
                val number = trimmed.substring(0, dotIdx)
                val body = trimmed.substring(dotIdx + 1).trim()
                result.add(MarkdownBlock.NumberedItem(number, body))
            }
            else -> {
                paragraphBuffer.add(line)
            }
        }
        i++
    }
    flushParagraph()
    return result
}

private fun parseInlineMarkdown(text: String, baseColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < text.length) {
            when {
                text.startsWith("**", idx) -> {
                    val end = text.indexOf("**", idx + 2)
                    if (end > idx + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = ChampagneGold)) {
                            append(text.substring(idx + 2, end))
                        }
                        idx = end + 2
                    } else {
                        append("**")
                        idx += 2
                    }
                }
                text[idx] == '`' -> {
                    val end = text.indexOf('`', idx + 1)
                    if (end > idx + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = JetBrainsMonoFamily,
                                background = CardSurfaceHigh,
                                color = ChampagneGold
                            )
                        ) {
                            append(" ${text.substring(idx + 1, end)} ")
                        }
                        idx = end + 1
                    } else {
                        append('`')
                        idx++
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = baseColor)) {
                        append(text[idx])
                    }
                    idx++
                }
            }
        }
    }
}

@Composable
fun OfflineWarningBanner(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CrimsonContainer)
                .border(1.dp, CrimsonError.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = CrimsonError,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.offline_banner),
                style = MaterialTheme.typography.bodySmall,
                color = WarmIvory
            )
        }
    }
}

@Composable
fun InAppNotificationBanner(
    alert: InAppAlert?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        if (alert != null) {
            val container = if (alert.isError) CrimsonContainer else SuccessContainer
            val accent = if (alert.isError) CrimsonError else EmeraldGoldSuccess
            val icon = if (alert.isError) Icons.Default.ErrorOutline else Icons.Default.CheckCircle

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.small,
                color = container,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = WarmIvory
                        )
                        if (alert.message.isNotBlank()) {
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "סגור התראה",
                            tint = MutedWarmGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplashRevealOverlay(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }
    var animateIn by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.90f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "splash_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        animateIn = true
        delay(850)
        visible = false
        delay(240)
        onFinished()
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(240)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(contentAlpha)
                    .padding(32.dp)
            ) {
                ClaudeControlMark(size = 84.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = ChampagneGold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "מרכז בקרה חכם בעברית · סביבת עבודה מקצועית",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedWarmGray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, GoldPrimary, ClaudeTerracotta, Color.Transparent)
                            )
                        )
                )
            }
        }
    }
}

object HebrewDateFormatter {
    private val formatter = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale("he", "IL"))
    private val timeOnly = SimpleDateFormat("HH:mm", Locale("he", "IL"))
    private val dayMonth = SimpleDateFormat("dd/MM", Locale("he", "IL"))

    fun formatDateTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return formatter.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return timeOnly.format(Date(timestamp))
    }

    fun formatDayMonth(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        return dayMonth.format(Date(timestamp))
    }
}
