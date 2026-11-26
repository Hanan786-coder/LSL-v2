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
import com.example.data.CricketScorer
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
    val bowled: Int,
    val catches: Int = 0,
    val runOuts: Int = 0,
    val stumpings: Int = 0
) {
    val dismissalsInField get() = catches + runOuts + stumpings
    val strikeRate get() = if (balls > 0) runs * 100.0 / balls else 0.0
    val economy get() = if (bowled > 0) conceded / (bowled / 6.0) else 0.0
    val oversString get() = "${bowled / 6}.${bowled % 6}"
}

private enum class Metric(val label: String, val tab: Int) {
    RUNS("Runs", 0), STRIKE("Strike rate", 0), SIXES("Sixes", 0), FOURS("Fours", 0),
    WICKETS("Wickets", 1), ECONOMY("Economy", 1),
    CATCHES("Catches", 2), RUN_OUTS("Run outs", 2), FIELD_TOTAL("All dismissals", 2);

    val batting get() = tab == 0
}

/** Catches, run outs and stumpings per player, read from finished matches' ball logs. */
private data class Fielding(var catches: Int = 0, var runOuts: Int = 0, var stumpings: Int = 0)

@Composable
fun LeaderboardScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.playerStats.collectAsState()
    val matches by viewModel.matches.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var metric by rememberSaveable { mutableStateOf(Metric.RUNS) }
    var query by rememberSaveable { mutableStateOf("") }

    val fielding = remember(matches) {
        val map = mutableMapOf<String, Fielding>()
        matches.filter { it.status == "COMPLETED" }.forEach { m ->
            CricketScorer.parseBalls(m.ballsJson).forEach { b ->
                val f = b.fielder?.trim()?.takeIf { b.isWicket && it.isNotEmpty() } ?: return@forEach
                val e = map.getOrPut(f.lowercase()) { Fielding() }
                when (b.wicketType) {
                    "CAUGHT" -> e.catches++
                    "RUN_OUT" -> e.runOuts++
                    "STUMPED" -> e.stumpings++
                }
            }
        }
        map
    }

    val careers = remember(stats, fielding) {
        stats.groupBy { it.playerName.trim().lowercase() }.map { (key, rows) ->
            val fd = fielding[key]
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
                bowled = rows.sumOf { it.ballsBowled },
                catches = fd?.catches ?: 0,
                runOuts = fd?.runOuts ?: 0,
                stumpings = fd?.stumpings ?: 0
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
            Metric.CATCHES -> base.filter { it.catches > 0 }.sortedByDescending { it.catches }
            Metric.RUN_OUTS -> base.filter { it.runOuts > 0 }.sortedByDescending { it.runOuts }
            Metric.FIELD_TOTAL -> base.filter { it.dismissalsInField > 0 }.sortedByDescending { it.dismissalsInField }
        }
    }

    LslScreen(title = "Player stats", subtitle = "Career totals across all matches", onBack = onBack) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SegmentedTabs(listOf("Batting", "Bowling", "Fielding"), tab) {
                tab = it
                metric = when (it) { 0 -> Metric.RUNS; 1 -> Metric.WICKETS; else -> Metric.CATCHES }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Metric.entries.filter { it.tab == tab }.forEach { m ->
                    ChoiceChip(m.label, metric == m, { metric = m })
                }
            }
            if (careers.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search player or team") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search", tint = Lsl.Mist) } },
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
                            careers.isEmpty() -> "Stats appear after your first finished match."
                            m == Metric.STRIKE || m == Metric.ECONOMY -> "Needs at least one full over (6 balls)."
                            m.tab == 2 -> "Counted when you pick the fielder on a catch, run out or stumping."
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
        Metric.CATCHES -> "${c.catches}" to "catches"
        Metric.RUN_OUTS -> "${c.runOuts}" to "run outs"
        Metric.FIELD_TOTAL -> "${c.dismissalsInField}" to "dismissals"
    }
    val detail = when (m.tab) {
        0 -> "${c.runs} off ${c.balls}, SR ${c.strikeRate.fmt(0)}, ${c.fours}×4 ${c.sixes}×6"
        1 -> "${c.wickets} for ${c.conceded} in ${c.oversString} ov, econ ${c.economy.fmt(1)}"
        else -> "${c.catches} ct, ${c.runOuts} ro, ${c.stumpings} st"
    }
    val medal = when (rank) { 0 -> Lsl.Gold; 1 -> Lsl.Chalk; 2 -> Lsl.Bronze; else -> Lsl.Fade }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Lsl.Deck)
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
                color = Lsl.Mist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(detail, style = MaterialTheme.typography.labelSmall, color = Lsl.Fade, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(big, fontFamily = Scoreboard, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Lsl.Chalk)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = Lsl.Mist)
        }
    }
}
