package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard

/** A player's career, merged across every squad they've played for. */
private data class Career(
    val name: String,
    val teams: List<String>,
    val matches: Int,
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    val wickets: Int,
    val conceded: Int,
    val bowled: Int
) {
    val strikeRate get() = if (balls > 0) runs * 100.0 / balls else 0.0
    val economy get() = if (bowled > 0) conceded / (bowled / 6.0) else 0.0
    val oversString get() = "${bowled / 6}.${bowled % 6}"
}

private enum class Metric(val label: String, val batting: Boolean) {
    RUNS("Runs", true), STRIKE("Strike rate", true), SIXES("Sixes", true), FOURS("Fours", true),
    WICKETS("Wickets", false), ECONOMY("Economy", false)
}

@Composable
fun LeaderboardScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.playerStats.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var metric by rememberSaveable { mutableStateOf(Metric.RUNS) }
    var query by rememberSaveable { mutableStateOf("") }

    val careers = remember(stats) {
        stats.groupBy { it.playerName.trim().lowercase() }.map { (_, rows) ->
            Career(
                name = rows.first().playerName,
                teams = rows.map { it.teamName }.distinct(),
                matches = rows.sumOf { it.matchesPlayed },
                runs = rows.sumOf { it.runsScored },
                balls = rows.sumOf { it.ballsFaced },
                fours = rows.sumOf { it.fours },
                sixes = rows.sumOf { it.sixes },
                wickets = rows.sumOf { it.wicketsTaken },
                conceded = rows.sumOf { it.runsConceded },
                bowled = rows.sumOf { it.ballsBowled }
            )
        }
    }

    fun rowsFor(m: Metric): List<Career> {
        val base = careers.filter {
            query.isBlank() || it.name.contains(query, true) || it.teams.any { t -> t.contains(query, true) }
        }
        return when (m) {
            Metric.RUNS -> base.filter { it.balls > 0 }.sortedByDescending { it.runs }
            Metric.STRIKE -> base.filter { it.balls >= 6 }.sortedByDescending { it.strikeRate }
            Metric.SIXES -> base.filter { it.sixes > 0 }.sortedByDescending { it.sixes }
            Metric.FOURS -> base.filter { it.fours > 0 }.sortedByDescending { it.fours }
            Metric.WICKETS -> base.filter { it.bowled > 0 }.sortedWith(compareByDescending<Career> { it.wickets }.thenBy { it.economy })
            Metric.ECONOMY -> base.filter { it.bowled >= 6 }.sortedBy { it.economy }
        }
    }

    LslScreen(title = "Player stats", subtitle = "Career totals across all matches", onBack = onBack) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SegmentedTabs(listOf("Batting", "Bowling"), tab) {
                tab = it
                metric = if (it == 0) Metric.RUNS else Metric.WICKETS
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Metric.entries.filter { it.batting == (tab == 0) }.forEach { m ->
                    ChoiceChip(m.label, metric == m, { metric = m })
                }
            }
            if (careers.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search player or team") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search", tint = Lsl.Moss) } },
                    singleLine = true,
                    colors = lslFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).testTag("stats_search_query")
                )
            }

            AnimatedContent(targetState = metric, label = "board", transitionSpec = {
                fadeIn(tween(200)) togetherWith fadeOut(tween(120))
            }) { m ->
                val rows = rowsFor(m)
                if (rows.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.Leaderboard,
                        title = if (careers.isEmpty()) "No stats yet" else "No one qualifies yet",
                        body = when {
                            careers.isEmpty() -> "Finish a match and every batter and bowler gets a career line here."
                            m == Metric.STRIKE || m == Metric.ECONOMY -> "Needs at least one full over (6 balls)."
                            else -> "Nobody has one of these yet."
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().navigationBarsPadding()
                    ) {
                        itemsIndexed(rows, key = { _, c -> c.name }) { i, c -> CareerRow(i, c, m) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerRow(rank: Int, c: Career, m: Metric) {
    val (big, unit) = when (m) {
        Metric.RUNS -> "${c.runs}" to "runs"
        Metric.STRIKE -> c.strikeRate.fmt(1) to "SR"
        Metric.SIXES -> "${c.sixes}" to "sixes"
        Metric.FOURS -> "${c.fours}" to "fours"
        Metric.WICKETS -> "${c.wickets}" to "wkts"
        Metric.ECONOMY -> c.economy.fmt(2) to "econ"
    }
    val detail = if (m.batting) {
        "${c.runs} off ${c.balls}, SR ${c.strikeRate.fmt(0)}, ${c.fours}×4 ${c.sixes}×6"
    } else {
        "${c.wickets} for ${c.conceded} in ${c.oversString} ov, econ ${c.economy.fmt(1)}"
    }
    val medal = when (rank) { 0 -> Lsl.Sun; 1 -> Lsl.Chalk; 2 -> Lsl.Flame; else -> Lsl.Fade }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Lsl.Turf)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${rank + 1}",
            fontFamily = Scoreboard,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = medal,
            modifier = Modifier.width(28.dp)
        )
        Monogram(c.name, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${c.teams.joinToString(", ")}, ${c.matches} match${if (c.matches == 1) "" else "es"}",
                style = MaterialTheme.typography.labelSmall,
                color = Lsl.Moss,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(detail, style = MaterialTheme.typography.labelSmall, color = Lsl.Fade, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(big, fontFamily = Scoreboard, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Lsl.Chalk)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = Lsl.Moss)
        }
    }
}
