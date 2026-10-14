package com.example.expensetracker.ui.ui


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensetracker.R
import com.example.expensetracker.data.CurrencyManager
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalUuidApi::class)
@Composable
fun SpendingPieChart(
    data: List<Pair<String, Double>>,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier
) {
    val totalSum = data.sumOf { it.second }
    if (data.isEmpty() || totalSum <= 0.0 ) {
        Column(modifier = modifier.fillMaxWidth().height(240.dp),verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.no_data), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.setup_hint),
                style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val categoryColors = remember {
        listOf(
            Color(0xFF4CAF50),
            Color(0xFFFFC107),
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF2196F3),
            Color(0xFF009688),
        )
    }
    val sortedData = data
        .filter {it.first.isNotBlank() && it.second > 0.0  }
        .toList()
        .sortedByDescending { it.second }
    val total = sortedData.sumOf { it.second }
    val colors = sortedData.mapIndexed { index, _ -> categoryColors[index % categoryColors.size] }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }


    val model = remember(data) {  data.map { it.second.toFloat() }}


    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(240.dp),
                contentAlignment = Alignment.Center
            ){
                val angles = remember(data, totalSum) {
                    var currentAngle = -90f
                    data.map { pair ->
                        val sweep = if (totalSum > 0) (pair.second / total * 360f).toFloat() else 0f
                        val start = currentAngle
                        currentAngle += sweep
                        start to sweep
                    }
                }

                Canvas(
                    Modifier.fillMaxSize().pointerInput(data, selectedIndex) {
                        detectTapGestures {offset ->
                            val center =  Offset(size.width / 2f, size.height / 2f)
                            val radius = minOf(size.width, size.height) / 2 * 0.8f
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (distance <= radius) {
                                var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble()))
                                angle = if (angle < -90) angle + 360 else angle

                                angles.forEachIndexed { index, (start, sweep) ->
                                    if (angle >= start && angle <= start + sweep) {
                                        selectedIndex = if (selectedIndex == index) null else index
                                    }
                                }
                            }

                        }
                    }
                ) {
                    val canvasSize = minOf(size.width, size.height)
                    val radius = canvasSize / 2 * 0.8f
                    val strokeWidth = 0f

                    var startAngle = -90f
                    sortedData.forEachIndexed { index, pair ->
                        val sweepAngle = if (total > 0.0) (pair.second / total * 360f).toFloat() else 0f
                        if (sweepAngle <= 0f) return@forEachIndexed
                        val isSelectedIndex = selectedIndex == index
                        val sliceRadius = if (isSelectedIndex) radius * 1.1f else radius

                        drawArc(
                            color =  colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = Size(sliceRadius * 2, sliceRadius * 2),
                            topLeft = Offset(center.x - sliceRadius, center.y - sliceRadius)
                        )
                        startAngle += sweepAngle
                    }

                    drawCircle(
                        color = Color.White,
                        radius = radius * 0.5f,
                        center = center
                    )

                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (selectedIndex != null) {
                        val item = data[selectedIndex!!]
                        Text(item.first, style = MaterialTheme.typography.labelLarge)
                        Text(
                            currencyManager.format(item.second),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                            )
                    } else {
                        Text("Total", style = MaterialTheme.typography.labelLarge)
                        Text(
                            currencyManager.format(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold

                        )
                    }

                   sortedData.forEachIndexed { index, (category, amount) ->
                       val percentage = if (total > 0) (amount / total * 100) else 0.0
                       Row(
                           modifier = Modifier.fillMaxWidth(),
                           horizontalArrangement = Arrangement.SpaceBetween,
                           verticalAlignment = Alignment.CenterVertically
                       ) {
                           Box(
                               modifier = Modifier.size(12.dp).clip(CircleShape)
                                   .background(colors[index], CircleShape)
                           )
                           //category name
                           Text(
                               text = category,
                               style = MaterialTheme.typography.bodyMedium,
                               modifier = Modifier.weight(1f)
                           )
                           //amount
                           Text(
                               text = currencyManager.format(amount),
                               style = MaterialTheme.typography.bodyMedium,
                               fontWeight = FontWeight.Medium
                           )

                           Spacer(Modifier.width(8.dp))

                           //percentage
                           Text(
                               text = "${"%.0f".format(percentage)}%",
                               style = MaterialTheme.typography.bodyMedium,
                               color = MaterialTheme.colorScheme.onSurfaceVariant
                           )

                       }
                   }

                }

            }


        }
    }
}




