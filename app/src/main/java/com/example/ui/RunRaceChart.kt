package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OverSummary
import com.example.ui.theme.Body
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Running score at the end of an over (over 0 = before the first ball). */
private data class RacePoint(val over: Float, val runs: Int, val wickets: Int, val wicketsThisOver: Int)

private fun racePoints(overs: List<OverSummary>): List<RacePoint> {
    val pts = mutableListOf(RacePoint(0f, 0, 0, 0))
    var runs = 0
    var wkts = 0
    overs.forEach { o ->
        runs += o.runs
        wkts += o.wickets
        val legal = o.labels.count { !it.endsWith("wd") && !it.endsWith("nb") }
        val x = if (o.isComplete) (o.index + 1).toFloat() else o.index + legal.coerceAtMost(6) / 6f
        pts += RacePoint(x, runs, wkts, o.wickets)
    }
    return pts
}

private fun niceStep(max: Int): Int =
    listOf(5, 10, 20, 25, 50, 100).firstOrNull { max / it <= 4 } ?: 200

/**
 * Run race ("worm"): both innings' running totals over by over on one set of axes,
 * with the target, wickets as red marks, and a readout the scorer can scrub with a finger.
 */
@Composable
fun RunRaceChart(
    firstName: String,
    secondName: String,
    first: List<OverSummary>,
    second: List<OverSummary>,
    totalOvers: Int,
    target: Int?
) {
    val a = remember(first) { racePoints(first) }
    val b = remember(second) { racePoints(second) }
    val lastOver = maxOf(a.size, b.size) - 1
    val xMax = maxOf(totalOvers, ceil(maxOf(a.last().over, b.last().over)).toInt(), 1)
    val top = maxOf(a.last().runs, b.last().runs, target ?: 0, 6)
    val step = niceStep(top)
    val yMax = ((top + step - 1) / step) * step

    var selected by remember(lastOver) { mutableIntStateOf(lastOver.coerceAtLeast(1)) }
    val reduce = rememberReduceMotion()
    val draw = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(900, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f))) }

    val measurer = rememberTextMeasurer()
    val axisStyle = TextStyle(fontFamily = Body, fontSize = 11.sp, color = Lsl.Fade)
    val targetStyle = TextStyle(fontFamily = Body, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Lsl.Gold)

    /** Score at the end of over [over]; an innings that already finished shows its final score. */
    fun at(points: List<RacePoint>, over: Int): Pair<RacePoint, Boolean>? = when {
        points.size <= 1 -> null                                  // not batted yet
        over < points.size -> points[over] to (over == points.size - 1 && points[over].over % 1f != 0f)
        else -> points.last() to true
    }

    Column {
        // Readout for the selected over
        val pa = at(a, selected)
        val pb = at(b, selected)
        val bothDone = (pa?.second ?: true) && (pb?.second ?: true) && (pa != null || pb != null)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    selected == 0 -> "Before the first ball"
                    bothDone -> "At the finish"
                    else -> "After over $selected"
                },
                style = MaterialTheme.typography.titleSmall,
                color = Lsl.Chalk,
                modifier = Modifier.weight(1f)
            )
            val lead = if (pa != null && pb != null) pa.first.runs - pb.first.runs else null
            if (lead != null && selected > 0) {
                Text(
                    when {
                        lead > 0 -> "$firstName ahead by $lead"
                        lead < 0 -> "$secondName ahead by ${-lead}"
                        else -> "Level"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Lsl.Mist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            ReadoutCell(firstName, Lsl.Flame, pa)
            ReadoutCell(secondName, Lsl.Sky, pb)
        }
        Spacer(Modifier.height(12.dp))

        val description = buildString {
            append("Run race. $firstName ${a.last().runs} for ${a.last().wickets}")
            if (b.size > 1) append(", $secondName ${b.last().runs} for ${b.last().wickets}")
            if (target != null) append(", target $target")
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(190.dp)
                .semantics { contentDescription = description }
                .pointerInput(xMax, lastOver) {
                    val left = 34.dp.toPx()
                    fun pick(x: Float) {
                        val w = size.width - left - 8.dp.toPx()
                        selected = (((x - left) / w) * xMax).roundToInt().coerceIn(0, lastOver)
                    }
                    detectTapGestures { pick(it.x) }
                }
                .pointerInput(xMax, lastOver) {
                    val left = 34.dp.toPx()
                    detectHorizontalDragGestures { change, _ ->
                        val w = size.width - left - 8.dp.toPx()
                        selected = (((change.position.x - left) / w) * xMax).roundToInt().coerceIn(0, lastOver)
                    }
                }
        ) {
            val left = 34.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val topY = 8.dp.toPx()
            val bottom = size.height - 22.dp.toPx()
            fun x(over: Float) = left + (right - left) * over / xMax
            fun y(runs: Int) = bottom - (bottom - topY) * runs / yMax

            // Horizontal grid + run labels
            var g = 0
            while (g <= yMax) {
                val yy = y(g)
                drawLine(Lsl.Line, Offset(left, yy), Offset(right, yy), strokeWidth = if (g == 0) 1.5f else 1f)
                val t = measurer.measure("$g", axisStyle)
                drawText(t, topLeft = Offset(left - t.size.width - 6.dp.toPx(), yy - t.size.height / 2))
                g += step
            }
            // Over labels along the bottom
            val xStep = when { xMax <= 8 -> 1; xMax <= 16 -> 2; xMax <= 30 -> 5; else -> 10 }
            for (o in 0..xMax step xStep) {
                val t = measurer.measure("$o", axisStyle)
                drawText(t, topLeft = Offset(x(o.toFloat()) - t.size.width / 2, bottom + 5.dp.toPx()))
            }

            // Target line
            if (target != null) {
                val ty = y(target)
                drawLine(
                    Lsl.Gold.copy(alpha = 0.8f), Offset(left, ty), Offset(right, ty),
                    strokeWidth = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                val t = measurer.measure("Target $target", targetStyle)
                drawText(t, topLeft = Offset(right - t.size.width, ty - t.size.height - 2.dp.toPx()))
            }

            // Selection guide
            val sx = x(maxOf(a.getOrNull(selected)?.over ?: 0f, b.getOrNull(selected)?.over ?: 0f))
            drawLine(Lsl.Chalk.copy(alpha = 0.25f), Offset(sx, topY), Offset(sx, bottom), strokeWidth = 1.dp.toPx())

            fun drawRace(points: List<RacePoint>, color: Color) {
                if (points.size < 2) return
                val path = Path()
                val reveal = points.last().over * draw.value
                points.forEachIndexed { i, p ->
                    val px = x(p.over); val py = y(p.runs)
                    if (i == 0) path.moveTo(px, py)
                    else {
                        val prev = points[i - 1]
                        if (p.over <= reveal) path.lineTo(px, py)
                        else if (prev.over < reveal) {
                            val f = (reveal - prev.over) / (p.over - prev.over)
                            path.lineTo(x(prev.over + (p.over - prev.over) * f), y(prev.runs) + (py - y(prev.runs)) * f)
                        }
                    }
                }
                drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                points.forEachIndexed { i, p ->
                    if (i == 0 || p.over > reveal) return@forEachIndexed
                    val c = Offset(x(p.over), y(p.runs))
                    val isSel = i == selected
                    drawCircle(Lsl.Deck, radius = (if (isSel) 6 else 4).dp.toPx(), center = c)
                    drawCircle(color, radius = (if (isSel) 6 else 4).dp.toPx(), center = c, style = Stroke(2.dp.toPx()))
                    // Wickets in this over: red marks stacked above the point
                    repeat(p.wicketsThisOver.coerceAtMost(4)) { w ->
                        drawCircle(Lsl.Tape, radius = 3.5.dp.toPx(), center = Offset(c.x, c.y - (11 + w * 8).dp.toPx()))
                    }
                }
            }
            drawRace(a, Lsl.Flame)
            drawRace(b, Lsl.Sky)
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendLine(Lsl.Flame, firstName)
            Spacer(Modifier.width(14.dp))
            LegendLine(Lsl.Sky, secondName)
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(8.dp).clip(CircleShape).background(Lsl.Tape))
            Text(" wicket", style = MaterialTheme.typography.labelSmall, color = Lsl.Mist)
        }
        Text(
            "Drag across the chart to see any over.",
            style = MaterialTheme.typography.labelSmall,
            color = Lsl.Fade,
            modifier = Modifier.padding(top = 6.dp)
        )

        // One plain-language fact: the over that changed the match most
        val best = (first.map { it to firstName } + second.map { it to secondName }).maxByOrNull { it.first.runs }
        if (best != null && best.first.runs > 0) {
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Lsl.DeckRaised).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Best over", style = MaterialTheme.typography.labelMedium, color = Lsl.Mist, modifier = Modifier.weight(1f))
                Text(
                    "${best.first.runs} runs, over ${best.first.index + 1}, ${best.second}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Lsl.Chalk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReadoutCell(name: String, color: Color, p: Pair<RacePoint, Boolean>?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.labelLarge, color = Lsl.Mist, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(
            if (p == null) "yet to bat" else "${p.first.runs}/${p.first.wickets}",
            fontFamily = if (p == null) Body else Scoreboard,
            fontWeight = FontWeight.Bold,
            fontSize = if (p == null) 13.sp else 20.sp,
            color = if (p == null) Lsl.Fade else Lsl.Chalk
        )
        if (p != null && p.second) {
            val o = p.first.over
            Text(
                " in ${o.toInt()}.${((o % 1f) * 6).roundToInt()} ov",
                style = MaterialTheme.typography.labelSmall,
                color = Lsl.Fade,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun LegendLine(color: Color, label: String) {
    Box(Modifier.width(14.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(color))
    Text(" $label", style = MaterialTheme.typography.labelSmall, color = Lsl.Mist, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
}
