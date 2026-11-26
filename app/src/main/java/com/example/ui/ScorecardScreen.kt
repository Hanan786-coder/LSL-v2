package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScorecardScreen(
    viewModel: LocoViewModel,
    onBackClick: () -> Unit
) {
    val m by viewModel.activeMatch.collectAsState()
    ProvideSquads(viewModel, m) { ScorecardContent(viewModel, onBackClick) }
}

@Composable
private fun ScorecardContent(
    viewModel: LocoViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val match by viewModel.activeMatch.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val m = match

    if (m == null) {
        LslScreen(title = "Scorecard", onBack = onBackClick) { inner ->
            EmptyState(
                Icons.Default.Leaderboard,
                "This match was removed",
                "Open another match from history.",
                Modifier.padding(inner)
            )
        }
        return
    }

    val firstId = viewModel.battingTeamId(m, 1)
    val secondId = viewModel.battingTeamId(m, 2)
    val firstName = viewModel.teamName(m, firstId)
    val secondName = viewModel.teamName(m, secondId)
    val balls = remember(m.ballsJson) { CricketScorer.parseBalls(m.ballsJson) }
    val first = remember(balls, teams) { CricketScorer.calculateInningsScorecard(balls, 1, viewModel.rosterFor(firstId)) }
    val second = remember(balls, teams) { CricketScorer.calculateInningsScorecard(balls, 2, viewModel.rosterFor(secondId)) }

    var tab by rememberSaveable { mutableIntStateOf(0) }

    LslScreen(
        title = "$firstName v $secondName",
        subtitle = if (m.status == "ONGOING") "Live, ${m.overs} overs" else "${m.overs} overs, ${SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(m.date))}",
        onBack = onBackClick,
        actions = {
            IconButton(onClick = {
                val text = summaryText(m, firstName, secondName, first, second)
                copyToClipboard(context, text)
            }) { Icon(Icons.Default.ContentCopy, "Copy summary", tint = Lsl.Mist) }
            IconButton(onClick = { shareText(context, summaryText(m, firstName, secondName, first, second)) }) {
                Icon(Icons.Default.Share, "Share summary", tint = Lsl.Flame)
            }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SegmentedTabs(
                labels = listOf("Summary", firstName, secondName),
                selected = tab,
                onSelect = { tab = it }
            )
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(260)) { it / 6 * dir } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(200)) { -it / 6 * dir } + fadeOut(tween(140)))
                },
                label = "card-tab",
                modifier = Modifier.fillMaxSize()
            ) { t ->
                when (t) {
                    0 -> SummaryTab(m, firstName, secondName, first, second) { shareText(context, summaryText(m, firstName, secondName, first, second)) }
                    1 -> InningsTab(firstName, first)
                    else -> InningsTab(secondName, second)
                }
            }
        }
    }
}

@Composable
fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Lsl.Deck)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, l ->
            val bg by animateColorAsState(if (i == selected) Lsl.DeckRaised else Color.Transparent, tween(180), label = "seg")
            val fg by animateColorAsState(if (i == selected) Lsl.Chalk else Lsl.Mist, tween(180), label = "seg-fg")
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .pressable(pressedScale = 0.97f) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(l, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Summary
// ---------------------------------------------------------------------------

@Composable
private fun SummaryTab(
    match: MatchEntity,
    firstName: String,
    secondName: String,
    first: InningsScorecard,
    second: InningsScorecard,
    onShare: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    ) {
        item { ResultBanner(match) }

        item {
            Panel {
                InningsLine(firstName, first, match.winnerTeamId == match.battingFirstTeamId && match.status == "COMPLETED")
                Spacer(Modifier.height(10.dp))
                val secondWon = match.status == "COMPLETED" && match.winnerTeamId != null && match.winnerTeamId != match.battingFirstTeamId
                InningsLine(secondName, second, secondWon)
            }
        }

        if (first.overs.isNotEmpty()) {
            item {
                Panel {
                    SectionTitle("Run race")
                    Spacer(Modifier.height(10.dp))
                    RunRaceChart(
                        firstName = firstName,
                        secondName = secondName,
                        first = first.overs,
                        second = second.overs,
                        totalOvers = match.overs,
                        target = match.targetRuns.takeIf { it > 0 }
                    )
                }
            }
        }

        item {
            Panel {
                SectionTitle("Top performers")
                Spacer(Modifier.height(8.dp))
                val bats = (first.batsmen.values + second.batsmen.values).filter { it.hasBatted }.sortedByDescending { it.runs }.take(3)
                val bowls = (first.bowlers.values + second.bowlers.values)
                    .sortedWith(compareByDescending<BowlerLiveStats> { it.wickets }.thenBy { it.runsConceded }).take(3)
                if (bats.isEmpty() && bowls.isEmpty()) {
                    Text("Nothing bowled yet.", color = Lsl.Mist, style = MaterialTheme.typography.bodyMedium)
                }
                bats.forEach { b ->
                    PerformerRow(b.name, "${b.runs}", "(${b.ballsFaced})${if (!b.isOut) " not out" else ""}", Icons.Default.SportsCricket)
                }
                if (bats.isNotEmpty() && bowls.isNotEmpty()) HorizontalDivider(color = Lsl.Line, modifier = Modifier.padding(vertical = 6.dp))
                bowls.forEach { b ->
                    PerformerRow(b.name, "${b.wickets}/${b.runsConceded}", "(${b.oversString} ov)", Icons.Default.SportsBaseball)
                }
            }
        }

        item {
            QuietButton(
                text = "Share with the group",
                icon = Icons.Default.Share,
                tint = Lsl.Flame,
                height = 52.dp,
                onClick = onShare,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LegendDot(c: Color) = Box(Modifier.size(8.dp).clip(CircleShape).background(c))

@Composable
private fun ResultBanner(match: MatchEntity) {
    val done = match.status == "COMPLETED"
    val pop = remember { Animatable(if (done) 0.6f else 1f) }
    LaunchedEffect(done) { if (done) pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (done) Lsl.Gold.copy(alpha = 0.14f) else Lsl.Deck)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            if (done) Icons.Default.EmojiEvents else Icons.Default.SportsCricket,
            contentDescription = null,
            tint = if (done) Lsl.Gold else Lsl.Flame,
            modifier = Modifier.size(40.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (done) match.resultText else "Match in progress",
            style = MaterialTheme.typography.headlineSmall,
            color = Lsl.Chalk,
            textAlign = TextAlign.Center
        )
        if (match.topPerformer.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Player of the match: ${match.topPerformer}", style = MaterialTheme.typography.bodyMedium, color = Lsl.Mist, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InningsLine(name: String, card: InningsScorecard, won: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Monogram(name, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = if (won) Lsl.Chalk else Lsl.Mist, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("RR ${card.runRate.fmt()}  Extras ${card.totalExtrasSum}", style = MaterialTheme.typography.labelSmall, color = Lsl.Fade)
        }
        CountUpText(
            value = card.totalRuns,
            style = androidx.compose.ui.text.TextStyle(fontFamily = Scoreboard, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
            color = if (won) Lsl.Chalk else Lsl.Mist,
            format = { "$it/${card.totalWickets}" }
        )
        Text(" (${card.totalOversString})", style = MaterialTheme.typography.labelMedium, color = Lsl.Fade)
    }
}

@Composable
private fun PerformerRow(name: String, figure: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Lsl.Fade, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(figure, fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Lsl.Chalk)
        Text(" $detail", style = MaterialTheme.typography.labelSmall, color = Lsl.Mist)
    }
}

// ---------------------------------------------------------------------------
// Innings detail
// ---------------------------------------------------------------------------

@Composable
private fun InningsTab(teamName: String, card: InningsScorecard) {
    val batted = card.batsmen.values.filter { it.hasBatted }.sortedBy { it.battingOrder }
    val yetToBat = card.batsmen.values.filter { !it.hasBatted }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    ) {
        item {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${card.totalRuns}/${card.totalWickets}", fontFamily = Scoreboard, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, color = Lsl.Chalk)
                Text("  ${card.totalOversString} overs", style = MaterialTheme.typography.titleSmall, color = Lsl.Mist, modifier = Modifier.padding(bottom = 8.dp))
                Spacer(Modifier.weight(1f))
                Text("RR ${card.runRate.fmt()}", style = MaterialTheme.typography.labelLarge, color = Lsl.Mist, modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        // Batting
        item {
            Panel(contentPadding = PaddingValues(vertical = 8.dp)) {
                TableHeader("Batter", listOf("R", "B", "4s", "6s", "SR"), BatCols)
                if (batted.isEmpty()) {
                    Text("$teamName haven't batted yet.", color = Lsl.Mist, modifier = Modifier.padding(16.dp))
                }
                batted.forEach { b ->
                    TableRow(
                        title = b.name,
                        subtitle = if (b.isOut) dismissalText(b) else "not out",
                        subtitleColor = if (b.isOut) Lsl.Fade else Lsl.Flame,
                        values = listOf("${b.runs}", "${b.ballsFaced}", "${b.fours}", "${b.sixes}", b.strikeRate.fmt(0)),
                        widths = BatCols,
                        emphasizeFirst = true
                    )
                }
                HorizontalDivider(color = Lsl.Line, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text("Extras", style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, modifier = Modifier.weight(1f))
                    Text(
                        "${card.totalExtrasSum}  (wd ${card.extras["WD"]}, nb ${card.extras["NB"]}, b ${card.extras["BYE"]}, lb ${card.extras["LB"]})",
                        style = MaterialTheme.typography.labelMedium,
                        color = Lsl.Mist
                    )
                }
                if (yetToBat.isNotEmpty()) {
                    Text(
                        "Yet to bat: ${yetToBat.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Lsl.Fade,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Bowling
        item {
            Panel(contentPadding = PaddingValues(vertical = 8.dp)) {
                TableHeader("Bowler", listOf("O", "R", "W", "Econ", "Ext"), BowlCols)
                if (card.bowlers.isEmpty()) {
                    Text("No balls bowled yet.", color = Lsl.Mist, modifier = Modifier.padding(16.dp))
                }
                card.bowlers.values.forEach { b ->
                    TableRow(
                        title = b.name,
                        subtitle = null,
                        values = listOf(b.oversString, "${b.runsConceded}", "${b.wickets}", b.economy.fmt(1), "${b.wides + b.noBalls}"),
                        widths = BowlCols,
                        emphasizeIndex = 2
                    )
                }
            }
        }

        // Over by over
        if (card.overs.isNotEmpty()) {
            item { SectionTitle("Over by over", Modifier.padding(top = 4.dp)) }
            items(card.overs.reversed(), key = { "o${it.index}" }) { o ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Lsl.Deck)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.width(64.dp)) {
                        Text("Over ${o.index + 1}", style = MaterialTheme.typography.labelLarge, color = Lsl.Chalk)
                        Text(o.bowler, style = MaterialTheme.typography.labelSmall, color = Lsl.Mist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        o.labels.take(9).forEach { BallChip(it, size = 26.dp) }
                    }
                    Text("${o.runs}", fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Lsl.Chalk)
                }
            }
        }
    }
}

private fun dismissalText(b: BatsmanLiveStats): String {
    val by = b.bowlerName
    val f = b.fielderName
    return when (b.outType) {
        "BOWLED" -> "b $by"
        "CAUGHT" -> when {
            f == null -> "caught b $by"
            f == by -> "c & b $by"
            else -> "c $f b $by"
        }
        "LBW" -> "lbw b $by"
        "STUMPED" -> if (f != null) "st $f b $by" else "st b $by"
        "HIT_WICKET" -> "hit wicket b $by"
        "RUN_OUT" -> if (f != null) "run out ($f)" else "run out"
        else -> "out"
    }
}

private val BatCols = listOf(34.dp, 32.dp, 30.dp, 30.dp, 42.dp)
private val BowlCols = listOf(38.dp, 34.dp, 28.dp, 50.dp, 36.dp)

@Composable
private fun TableHeader(first: String, cols: List<String>, widths: List<androidx.compose.ui.unit.Dp>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text(first, style = MaterialTheme.typography.labelMedium, color = Lsl.Mist, modifier = Modifier.weight(1f))
        cols.forEachIndexed { i, c ->
            Text(c, style = MaterialTheme.typography.labelMedium, color = Lsl.Mist, textAlign = TextAlign.End, modifier = Modifier.width(widths[i]))
        }
    }
}

@Composable
private fun TableRow(
    title: String,
    subtitle: String?,
    values: List<String>,
    widths: List<androidx.compose.ui.unit.Dp>,
    subtitleColor: Color = Lsl.Fade,
    emphasizeFirst: Boolean = false,
    emphasizeIndex: Int = if (emphasizeFirst) 0 else -1
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(decoratedName(title, LocalSquadLookup.current(title)), style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = subtitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        values.forEachIndexed { i, v ->
            Text(
                v,
                fontFamily = if (i == emphasizeIndex) Scoreboard else null,
                fontWeight = if (i == emphasizeIndex) FontWeight.Bold else FontWeight.Normal,
                fontSize = if (i == emphasizeIndex) 18.sp else 14.sp,
                color = if (i == emphasizeIndex) Lsl.Chalk else Lsl.Mist,
                textAlign = TextAlign.End,
                modifier = Modifier.width(widths[i])
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sharing
// ---------------------------------------------------------------------------

private fun summaryText(
    match: MatchEntity,
    firstName: String,
    secondName: String,
    first: InningsScorecard,
    second: InningsScorecard
): String {
    fun topBat(c: InningsScorecard) = c.batsmen.values.filter { it.hasBatted }.maxByOrNull { it.runs }
        ?.let { "${it.name} ${it.runs}(${it.ballsFaced})" } ?: "n/a"
    fun topBowl(c: InningsScorecard) = c.bowlers.values.maxWithOrNull(compareBy<BowlerLiveStats> { it.wickets }.thenByDescending { it.runsConceded })
        ?.let { "${it.name} ${it.wickets}/${it.runsConceded}" } ?: "n/a"
    val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(match.date))
    return buildString {
        appendLine("🏏 Loco Street League")
        appendLine("$firstName v $secondName, ${match.overs} overs, $date")
        appendLine()
        appendLine("$firstName ${first.totalRuns}/${first.totalWickets} (${first.totalOversString})")
        appendLine("  Top bat: ${topBat(first)}  Top bowl: ${topBowl(first)}")
        appendLine("$secondName ${second.totalRuns}/${second.totalWickets} (${second.totalOversString})")
        appendLine("  Top bat: ${topBat(second)}  Top bowl: ${topBowl(second)}")
        appendLine()
        appendLine(if (match.status == "COMPLETED") "Result: ${match.resultText}" else "Match in progress")
        if (match.topPerformer.isNotEmpty()) appendLine("Player of the match: ${match.topPerformer}")
    }.trim()
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share scorecard"))
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Match summary", text))
    Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show()
}
