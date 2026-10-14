package com.example.expensetracker.ui.ui


import com.example.expensetracker.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.example.expensetracker.data.BarData
import com.example.expensetracker.data.CurrencyManager
import kotlin.math.ceil

@Composable
fun BarChart(
    data: List<BarData>,
    currencyManager: CurrencyManager,
    modifier: Modifier = Modifier,
    barColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary
) {


    if (data.isEmpty()) {
      Box(modifier = Modifier.height(200.dp), contentAlignment = Alignment.Center) {
         Text(stringResource(R.string.no_spending_range), style = MaterialTheme.typography.bodyMedium)
      }
        return
    }

    val maxRaw = data.maxOf { it.value }
    val maxValue = if (maxRaw == 0f) 1f else ceil(maxRaw / 1000f) * 1000f

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val textColor = MaterialTheme.colorScheme.onSurface

    val gridLines = 4
    val yAxisPaddingDp = 40.dp
    val horizontalPaddingDp = 16.dp
    val labelAreaHeightDp = 20.dp
    val minBarWidthDp = 12.dp


    // Animate bars from 0 -> value
    val animatedHeights = remember(data) {
        data.map { Animatable(0f) }
    }

    LaunchedEffect(data) {
        selectedBarIndex = null
        animatedHeights.forEachIndexed { index, anim ->
            anim.snapTo(0f)
            anim.animateTo(data[index].value, tween(600, delayMillis = index * 50))
        }
    }



    BoxWithConstraints(modifier = modifier.height(220.dp)) {
        val canvasWidthDp = maxWidth - horizontalPaddingDp * 2 - yAxisPaddingDp
        val barCount = data.size
        val labelWidthDp = 32.dp
        val availableWidthDp = maxWidth - horizontalPaddingDp * 2 - yAxisPaddingDp
        val idealBarWidthDp = availableWidthDp / (barCount * 1.5f)
        val barWidthDp = idealBarWidthDp.coerceAtLeast(minBarWidthDp)
        val spacingDp = barWidthDp / 2f


        Column(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = horizontalPaddingDp)
                .pointerInput(barCount, barWidthDp, spacingDp) {
                    detectTapGestures { offset ->
                        val yAxisPaddingPx = yAxisPaddingDp.toPx()
                        val barWidthPx = barWidthDp.toPx()
                        val spacingPx = spacingDp.toPx()
                        val tappedX = offset.x - yAxisPaddingPx
                      if (tappedX > 0) {
                          val index = (tappedX / (barWidthPx + spacingPx)).toInt().coerceIn(0, barCount - 1)
                          selectedBarIndex = if (selectedBarIndex == index) null else index
                      }
                    }
                }
        ) {
            // 1. Draw Y-axis grid lines
            val canvasHeight = size.height
            val yAxisPaddingPx = yAxisPaddingDp.toPx()
            val canvasWidthPx = size.width
            val barWidthPx = barWidthDp.toPx()
            val spacingPx = spacingDp.toPx()
            val labelPaddingBottom = 20f
            val chartHeightPx = canvasHeight - labelAreaHeightDp.toPx()
            val yAxisPaint = android.graphics.Paint().apply {
                color = textColor.toArgb()
                textSize = 24f
                textAlign = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }

            repeat(gridLines + 1) { i ->
                val ratio = i / gridLines.toFloat()
                val y = chartHeightPx - (ratio * chartHeightPx)
                drawLine(
                    color = gridColor,
                    start = Offset(yAxisPaddingPx, y),
                    end = Offset(canvasWidthPx, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                // Y label
                drawContext.canvas.nativeCanvas.drawText(
                    currencyManager.format((maxValue * ratio).toDouble()).substringBefore("."),
                    yAxisPaddingPx - 8f,
                    y + 8f,
                    yAxisPaint
                )
            }

            // 2. Draw Bars
            data.forEachIndexed { index, bar ->
                val animatedValue = animatedHeights.getOrNull(index)?.value?: 0f
                val barHeight = (animatedValue / maxValue) * chartHeightPx
                val left = yAxisPaddingPx + index * (barWidthPx + spacingPx)
                val top = chartHeightPx - barHeight
                val isSelected = selectedBarIndex == index

                drawRoundRect(
                    color = if (isSelected) barColor.copy(alpha = 0.5f) else barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }

            // 3. X-axis labels - uses EXACT same math as Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(labelAreaHeightDp)
                    .padding(start = horizontalPaddingDp + yAxisPaddingDp, end = horizontalPaddingDp)
            ) {
                // 👈 This is the width Canvas actually draws in
                val drawableWidthDp = barCount * barWidthDp + (barCount - 1) * spacingDp

                data.forEachIndexed { index, bar ->
                    val barLeftDp = index * (barWidthDp + spacingDp) // No yAxisPadding here, Box already padded
                    val barCenterDp = barLeftDp + barWidthDp / 2f
                    val labelStartDp = barCenterDp - labelWidthDp / 2f

                    Text(
                        text = bar.label.take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Visible, // 👈 Don't clip to ...
                        modifier = Modifier
                            .width(labelWidthDp)
                            .absoluteOffset(x = labelStartDp)
                    )
                }
            }

    }
        // 👈 Tooltip that pops above the bar
        selectedBarIndex?.let { index ->
            val bar = data.getOrNull(index)?: return@let
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.inverseSurface
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currencyManager.format(bar.value.toDouble()),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface
                        )
                        if (bar.subLabel.isNotBlank()) {
                            Text(
                                text = bar.subLabel, // 23 - 29 Jun
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

    }
}