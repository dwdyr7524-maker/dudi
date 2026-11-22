package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.ActionLogEntity
import com.example.data.local.ActionType
import com.example.data.local.ConnectionMode
import com.example.data.remote.AnthropicCostEstimator
import com.example.ui.components.DailyActivityBarChart
import com.example.ui.components.DailyChartPoint
import com.example.ui.components.GoldDivider
import com.example.ui.components.GoldPillBadge
import com.example.ui.components.HebrewDateFormatter
import com.example.ui.components.LuxuryObsidianCard
import com.example.ui.theme.CardSurfaceHigh
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.EmeraldGoldSuccess
import com.example.ui.theme.GoldHairline
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.InfoBlueAccent
import com.example.ui.theme.MutedWarmGray
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SoftGoldContainer
import com.example.ui.theme.VelvetCharcoal
import com.example.ui.theme.WarmIvory
import com.example.ui.viewmodel.DashboardStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    stats: DashboardStats,
    actionLogs: List<ActionLogEntity>,
    selectedModel: String = "",
    monthlyBudgetUsd: Float = 0f,
    onUpdateMonthlyBudgetUsd: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dailyPoints = remember(actionLogs) { buildLast7DaysChart(actionLogs) }
    val monthlyStats = remember(actionLogs) { buildMonthlyBreakdown(actionLogs) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize()
        ) {
            Surface(color = VelvetCharcoal) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = ChampagneGold
                        )
                        Column {
                            Text(
                                text = "סטטיסטיקות ופעילות",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ChampagneGold
                            )
                            Text(
                                text = "נתונים אמיתיים מתוך יומן הפעולות המקומי",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedWarmGray
                            )
                        }
                    }
                    GoldDivider()
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary Totals
                item {
                    LuxuryObsidianCard(highlighted = true) {
                        Text(
                            text = "סיכום פעילות כולל",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatTile("הודעות שהוכנו", stats.preparedCount.toString(), InfoBlueAccent, Modifier.weight(1f))
                            StatTile("נשלחו בפועל", stats.sentCount.toString(), GoldPrimary, Modifier.weight(1f))
                            StatTile("תשובות שהתקבלו", stats.repliesCount.toString(), EmeraldGoldSuccess, Modifier.weight(1f))
                            StatTile("משימות שהושלמו", stats.completedTasksCount.toString(), ChampagneGold, Modifier.weight(1f))
                        }
                    }
                }

                // Daily Activity Bar Chart (last 7 days)
                item {
                    LuxuryObsidianCard {
                        Text(
                            text = "גרף פעילות יומי (7 ימים אחרונים)",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DailyActivityBarChart(points = dailyPoints)
                    }
                }

                // API Token Usage + Monthly Estimated Cost & Budget Cap
                item {
                    LuxuryObsidianCard {
                        Text(
                            text = "שימוש ואומדן עלויות ב-Anthropic API",
                            style = MaterialTheme.typography.titleMedium,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (stats.totalInputTokens == 0 && stats.totalOutputTokens == 0) {
                            Text(
                                text = "טרם נעשה שימוש ב-API בתשלום. העבודה מתבצעת במצב חינמי ללא עלויות.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldGoldSuccess
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatTile("סה״כ אסימוני קלט", stats.totalInputTokens.toString(), InfoBlueAccent, Modifier.weight(1f))
                                StatTile("סה״כ אסימוני פלט", stats.totalOutputTokens.toString(), ChampagneGold, Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                StatTile(
                                    "אסימונים החודש",
                                    "${stats.monthlyInputTokens + stats.monthlyOutputTokens}",
                                    WarmIvory,
                                    Modifier.weight(1f)
                                )
                                StatTile(
                                    "אומדן חודשי",
                                    AnthropicCostEstimator.formatUsd(stats.estimatedMonthlyCostUsd),
                                    GoldPrimary,
                                    Modifier.weight(1f)
                                )
                            }
                            if (selectedModel.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "האומדן מחושב לפי תעריפי המודל הנוכחי ($selectedModel) ומתבסס על יומן האסימונים המקומי.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedWarmGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        GoldDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "תקרת התראה חודשית (אופציונלי)",
                            style = MaterialTheme.typography.labelLarge,
                            color = ChampagneGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0f to "ללא הגבלה",
                                5f to "$5",
                                15f to "$15",
                                30f to "$30"
                            ).forEach { (cap, label) ->
                                val selected = kotlin.math.abs(monthlyBudgetUsd - cap) < 0.05f
                                FilterChip(
                                    selected = selected,
                                    onClick = { onUpdateMonthlyBudgetUsd(cap) },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SoftGoldContainer,
                                        selectedLabelColor = ChampagneGold,
                                        containerColor = ObsidianBlack,
                                        labelColor = MutedWarmGray
                                    )
                                )
                            }
                        }

                        if (monthlyBudgetUsd > 0f) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val ratio = (stats.estimatedMonthlyCostUsd / monthlyBudgetUsd.toDouble())
                                .toFloat()
                                .coerceIn(0f, 1f)
                            val pct = ((stats.estimatedMonthlyCostUsd / monthlyBudgetUsd.toDouble()) * 100.0).toInt()
                            val progressColor = when {
                                pct >= 100 -> CrimsonError
                                pct >= 80 -> ChampagneGold
                                else -> EmeraldGoldSuccess
                            }
                            LinearProgressIndicator(
                                progress = { ratio },
                                color = progressColor,
                                trackColor = CardSurfaceHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ניצול משוער החודש: ${AnthropicCostEstimator.formatUsd(stats.estimatedMonthlyCostUsd)} מתוך $${monthlyBudgetUsd.toInt()} ($pct%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = progressColor
                            )
                        }
                    }
                }

                // Monthly Breakdown
                if (monthlyStats.isNotEmpty()) {
                    item {
                        LuxuryObsidianCard {
                            Text(
                                text = "סטטיסטיקות חודשיות",
                                style = MaterialTheme.typography.titleMedium,
                                color = ChampagneGold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                monthlyStats.forEach { monthRow ->
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = ObsidianBlack.copy(alpha = 0.65f),
                                        border = BorderStroke(0.75.dp, GoldHairline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = monthRow.monthLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = ChampagneGold
                                            )
                                            Text(
                                                text = "הוכנו: ${monthRow.prepared} · נשלחו: ${monthRow.sent} · תשובות: ${monthRow.replies}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = WarmIvory
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action History Log
                item {
                    Text(
                        text = "היסטוריית פעולות (${actionLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = ChampagneGold
                    )
                }

                if (actionLogs.isEmpty()) {
                    item {
                        LuxuryObsidianCard {
                            Text(
                                text = "עדיין אין פעולות מתועדות. הכן או שלח הודעה ראשונה כדי לראות היסטוריה וגרפים.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedWarmGray
                            )
                        }
                    }
                } else {
                    items(actionLogs.take(40), key = { it.id }) { log ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = VelvetCharcoal,
                            border = BorderStroke(0.75.dp, GoldHairline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GoldPillBadge(
                                            text = mapActionTypeToHebrew(log.actionType),
                                            contentColor = when (log.actionType) {
                                                ActionType.FAILED -> CrimsonError
                                                ActionType.REPLY_RECEIVED -> EmeraldGoldSuccess
                                                ActionType.SENT -> ChampagneGold
                                                else -> InfoBlueAccent
                                            }
                                        )
                                        GoldPillBadge(
                                            text = if (log.mode == ConnectionMode.PAID_API) "API בתשלום" else "חינמי",
                                            contentColor = MutedWarmGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = log.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WarmIvory
                                    )
                                }
                                Text(
                                    text = HebrewDateFormatter.formatDateTime(log.timestamp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedWarmGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(min = 120.dp),
        shape = MaterialTheme.shapes.medium,
        color = ObsidianBlack.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, GoldHairline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MutedWarmGray
            )
        }
    }
}

private data class MonthlyRow(
    val monthLabel: String,
    val prepared: Int,
    val sent: Int,
    val replies: Int
)

private fun buildLast7DaysChart(logs: List<ActionLogEntity>): List<DailyChartPoint> {
    val cal = Calendar.getInstance()
    val dayKeyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFmt = SimpleDateFormat("dd/MM", Locale("he", "IL"))

    val days = (6 downTo 0).map { offset ->
        val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
        dayKeyFmt.format(c.time) to displayFmt.format(c.time)
    }

    val grouped = logs.groupBy { dayKeyFmt.format(Date(it.timestamp)) }
    return days.map { (key, label) ->
        val dayLogs = grouped[key].orEmpty()
        DailyChartPoint(
            label = label,
            preparedCount = dayLogs.count { it.actionType == ActionType.PREPARED },
            sentCount = dayLogs.count { it.actionType == ActionType.SENT },
            repliesCount = dayLogs.count { it.actionType == ActionType.REPLY_RECEIVED }
        )
    }
}

private fun buildMonthlyBreakdown(logs: List<ActionLogEntity>): List<MonthlyRow> {
    val fmt = SimpleDateFormat("MM/yyyy", Locale("he", "IL"))
    return logs
        .groupBy { fmt.format(Date(it.timestamp)) }
        .entries
        .map { (month, items) ->
            MonthlyRow(
                monthLabel = month,
                prepared = items.count { it.actionType == ActionType.PREPARED },
                sent = items.count { it.actionType == ActionType.SENT },
                replies = items.count { it.actionType == ActionType.REPLY_RECEIVED }
            )
        }
}

private fun mapActionTypeToHebrew(type: String): String {
    return when (type) {
        ActionType.PREPARED -> "הוכנה"
        ActionType.SENT -> "נשלחה"
        ActionType.REPLY_RECEIVED -> "תשובה"
        ActionType.FAILED -> "שגיאה"
        ActionType.REMINDER_FIRED -> "תזכורת"
        ActionType.TASK_COMPLETED -> "משימה"
        else -> type
    }
}
