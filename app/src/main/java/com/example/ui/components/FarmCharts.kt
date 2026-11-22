package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.tr

data class DualBarEntry(
    val label: String,
    val primaryValue: Double,
    val secondaryValue: Double
)

data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun DualBarComparisonChart(
    title: String,
    primaryLabel: String,
    secondaryLabel: String,
    entries: List<DualBarEntry>,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.error,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendDot(color = primaryColor, label = primaryLabel)
                    LegendDot(color = secondaryColor, label = secondaryLabel)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (entries.isEmpty()) {
                Text(
                    tr("No financial data yet", "এখনও কোনো আর্থিক তথ্য নেই"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = (entries.maxOfOrNull { maxOf(it.primaryValue, it.secondaryValue) } ?: 1.0)
                    .coerceAtLeast(1.0)
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    val count = entries.size
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.28f).coerceIn(10f, 36f)
                    val gap = barWidth * 0.25f

                    for (i in 0..3) {
                        val y = size.height * (i / 3f)
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                    }

                    entries.forEachIndexed { idx, item ->
                        val centerX = slotWidth * idx + slotWidth / 2f
                        val h1 = ((item.primaryValue / maxVal) * (size.height - 8f)).toFloat().coerceAtLeast(4f)
                        val h2 = ((item.secondaryValue / maxVal) * (size.height - 8f)).toFloat().coerceAtLeast(4f)

                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(centerX - barWidth - gap / 2f, size.height - h1),
                            size = Size(barWidth, h1),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        drawRoundRect(
                            color = secondaryColor,
                            topLeft = Offset(centerX + gap / 2f, size.height - h2),
                            size = Size(barWidth, h2),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    entries.forEach { item ->
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmoothAreaLineChart(
    title: String,
    points: List<Pair<String, Double>>,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (points.isEmpty()) {
                Text(
                    tr("No trend data recorded yet", "এখনও কোনো ট্রেন্ড তথ্য সংরক্ষিত হয়নি"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxVal = (points.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
                val minVal = (points.minOfOrNull { it.second } ?: 0.0).coerceAtMost(0.0)
                val range = (maxVal - minVal).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                ) {
                    val stepX = if (points.size > 1) size.width / (points.size - 1) else size.width
                    val coords = points.mapIndexed { idx, (_, v) ->
                        val x = if (points.size == 1) size.width / 2f else idx * stepX
                        val normY = ((v - minVal) / range).toFloat()
                        val y = size.height - (normY * (size.height - 16f)) - 8f
                        Offset(x, y)
                    }
                    if (coords.size >= 2) {
                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            coords.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(coords.last().x, size.height)
                            lineTo(coords.first().x, size.height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 5f, cap = StrokeCap.Round)
                        )
                    }
                    coords.forEach { pt ->
                        drawCircle(color = lineColor, radius = 7f, center = pt)
                        drawCircle(color = Color.White, radius = 3.5f, center = pt)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach { (label, _) ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownDonut(
    title: String,
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            val total = slices.sumOf { it.value }
            if (slices.isEmpty() || total <= 0.0) {
                Text(
                    tr("Add records to see breakdown", "বিশ্লেষণ দেখতে তথ্য যোগ করুন"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Canvas(modifier = Modifier.size(104.dp)) {
                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweep = ((slice.value / total) * 360f).toFloat()
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                style = Stroke(width = 24f, cap = StrokeCap.Butt)
                            )
                            startAngle += sweep
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        slices.take(5).forEach { slice ->
                            val pct = ((slice.value / total) * 100).toInt()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendDot(color = slice.color, label = slice.label)
                                Text(
                                    "${slice.value.toInt()} ($pct%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
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
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(modifier = Modifier.size(8.dp)) { drawCircle(color = color) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
