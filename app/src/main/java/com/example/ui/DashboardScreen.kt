package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BallInfo
import com.example.data.CricketScorer
import com.example.data.JsonHelper
import com.example.data.MatchEntity
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: LocoViewModel,
    onStartMatchClick: () -> Unit,
    onManageTeamsClick: () -> Unit,
    onTournamentsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val matches by viewModel.matches.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()
    val players by viewModel.playerStats.collectAsState()
    val saved by viewModel.players.collectAsState()

    val ongoing = matches.filter { it.status == "ONGOING" }
    val completed = matches.filter { it.status == "COMPLETED" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Lsl.Night)
            .testTag("dashboard"),
        contentPadding = WindowInsets.safeDrawing.asPaddingValues().let {
            PaddingValues(start = 16.dp, end = 16.dp, top = it.calculateTopPadding() + 8.dp, bottom = it.calculateBottomPadding() + 24.dp)
        },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Brand header
        item {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.lsl_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Loco Street League", style = MaterialTheme.typography.headlineSmall, color = Lsl.Chalk)
                    Text(
                        greeting(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Lsl.Mist
                    )
                }
            }
        }

        // Live matches first: that's where the scorer wants to be
        if (ongoing.isNotEmpty()) {
            items(ongoing, key = { "live-${it.id}" }) { m ->
                LiveMatchCard(match = m, viewModel = viewModel, onClick = { viewModel.openMatch(m) })
            }
        }

        // Start a match
        item {
            val canStart = teams.size >= 2
            StartMatchPanel(
                canStart = canStart,
                hasLive = ongoing.isNotEmpty(),
                onClick = if (canStart) onStartMatchClick else onManageTeamsClick
            )
        }

        // Navigation list with real counts
        item {
            Panel(contentPadding = PaddingValues(vertical = 4.dp)) {
                NavRow(Icons.Default.Groups, "Teams", countLabel(teams.size, "squad"), Lsl.Flame, onManageTeamsClick, Modifier.testTag("nav_teams"))
                NavDivider()
                NavRow(Icons.Default.Person, "Players", countLabel(saved.size, "saved player"), roleColor(com.example.data.PlayerRole.KEEPER), { viewModel.navigateTo("players") }, Modifier.testTag("nav_players"))
                NavDivider()
                NavRow(Icons.Default.EmojiEvents, "Tournaments", countLabel(tournaments.size, "league"), Lsl.Gold, onTournamentsClick, Modifier.testTag("nav_tournaments"))
                NavDivider()
                NavRow(Icons.Default.Leaderboard, "Player stats", countLabel(players.distinctBy { it.playerName }.size, "player"), Lsl.Sky, onLeaderboardClick, Modifier.testTag("nav_stats"))
                NavDivider()
                NavRow(Icons.Default.History, "Match history", countLabel(completed.size, "match", "matches"), Lsl.Violet, onHistoryClick, Modifier.testTag("nav_history"))
            }
        }

        // Recent results
        item {
            SectionTitle(
                "Recent results",
                modifier = Modifier.padding(top = 8.dp),
                trailing = {
                    if (completed.size > 3) {
                        Text(
                            "See all",
                            color = Lsl.Flame,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).pressable(onClick = onHistoryClick).padding(8.dp)
                        )
                    }
                }
            )
        }
        if (completed.isEmpty()) {
            item {
                Panel {
                    Text("No finished matches yet", style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Results, top scorers and the player of the match show up here after your first game.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Lsl.Mist
                    )
                }
            }
        } else {
            items(completed.take(3), key = { "done-${it.id}" }) { m ->
                ResultCard(match = m, onClick = { viewModel.openMatch(m) })
            }
        }
    }
}

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "Morning game? Teams are ready when you are."
        in 12..16 -> "Afternoon game? Teams are ready when you are."
        in 17..20 -> "Evening game? Teams are ready when you are."
        else -> "Late one? Teams are ready when you are."
    }
}

private fun countLabel(n: Int, singular: String, plural: String = singular + "s") =
    if (n == 0) "None yet" else "$n ${if (n == 1) singular else plural}"

@Composable
private fun NavDivider() = HorizontalDivider(color = Lsl.Line, modifier = Modifier.padding(start = 64.dp, end = 16.dp))

@Composable
private fun NavRow(icon: ImageVector, title: String, meta: String, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk, modifier = Modifier.weight(1f))
        Text(meta, style = MaterialTheme.typography.labelMedium, color = Lsl.Mist)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Lsl.Fade)
    }
}

@Composable
private fun StartMatchPanel(canStart: Boolean, hasLive: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (hasLive) Lsl.Deck else Lsl.Flame)
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .padding(20.dp)
            .testTag("hero_promo_card"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fg = if (hasLive) Lsl.Chalk else Lsl.FlameInk
        Column(Modifier.weight(1f)) {
            Text(
                when {
                    !canStart -> "Add two teams to start"
                    hasLive -> "Start another match"
                    else -> "Start a match"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = fg
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (canStart) "Pick two teams, toss the coin, score every ball."
                else "Create squads with player names, then score your first game.",
                style = MaterialTheme.typography.bodyMedium,
                color = fg.copy(alpha = 0.8f)
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(if (hasLive) Lsl.Flame else Lsl.FlameInk),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (canStart) Icons.Default.SportsCricket else Icons.Default.GroupAdd,
                contentDescription = null,
                tint = if (hasLive) Lsl.FlameInk else Lsl.Flame,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun LiveMatchCard(match: MatchEntity, viewModel: LocoViewModel, onClick: () -> Unit) {
    val balls = remember(match.ballsJson) { CricketScorer.parseBalls(match.ballsJson) }
    val card = remember(balls, match.currentInnings) { CricketScorer.calculateInningsScorecard(balls, match.currentInnings, emptyList()) }
    val batting = viewModel.teamName(match, viewModel.battingTeamId(match))
    val bowling = viewModel.teamName(match, viewModel.bowlingTeamId(match))

    val pulse = rememberInfiniteTransition(label = "live")
    val a by pulse.animateFloat(0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "live-dot")

    Panel(
        modifier = Modifier.fillMaxWidth().testTag("live_match_card"),
        shape = RoundedCornerShape(22.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).graphicsLayer { alpha = a }.clip(CircleShape).background(Lsl.Tape))
            Spacer(Modifier.width(8.dp))
            Text("Live", style = MaterialTheme.typography.labelLarge, color = Lsl.Tape)
            Spacer(Modifier.weight(1f))
            Text("Tap to keep scoring", style = MaterialTheme.typography.labelMedium, color = Lsl.Mist)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(batting, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(batting, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("v $bowling", style = MaterialTheme.typography.labelMedium, color = Lsl.Mist, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    RollingNumber(card.totalRuns, 32.sp, Lsl.Chalk)
                    Text("/", fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Lsl.Fade, modifier = Modifier.padding(horizontal = 1.dp, vertical = 3.dp))
                    RollingNumber(card.totalWickets, 24.sp, if (card.totalWickets > 0) Lsl.Tape else Lsl.Mist, Modifier.padding(bottom = 2.dp))
                }
                Text("${card.totalOversString} of ${match.overs} ov", style = MaterialTheme.typography.labelSmall, color = Lsl.Mist)
            }
        }
        if (match.currentInnings == 2 && match.targetRuns > 0) {
            Spacer(Modifier.height(8.dp))
            val need = (match.targetRuns - card.totalRuns).coerceAtLeast(0)
            val left = (match.overs * 6 - card.legalBallsBowled).coerceAtLeast(0)
            Text("Need $need off $left balls", style = MaterialTheme.typography.labelLarge, color = Lsl.Gold)
        }
    }
}

@Composable
fun ResultCard(match: MatchEntity, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val balls = remember(match.ballsJson) { CricketScorer.parseBalls(match.ballsJson) }
    val first = remember(balls) { CricketScorer.calculateInningsScorecard(balls, 1, emptyList()) }
    val second = remember(balls) { CricketScorer.calculateInningsScorecard(balls, 2, emptyList()) }
    val firstId = match.battingFirstTeamId
    val secondId = if (firstId == match.teamAId) match.teamBId else match.teamAId
    val firstName = if (firstId == match.teamAId) match.teamAName else match.teamBName
    val secondName = if (secondId == match.teamAId) match.teamAName else match.teamBName
    val date = remember(match.date) { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(match.date)) }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Lsl.Deck)
            .pressable(pressedScale = 0.98f, onLongClick = onLongClick, onClick = onClick)
            .padding(16.dp)
    ) {
        Text("$date, ${match.overs} overs", style = MaterialTheme.typography.labelSmall, color = Lsl.Mist)
        Spacer(Modifier.height(8.dp))
        TeamScoreLine(firstName, "${first.totalRuns}/${first.totalWickets}", first.totalOversString, match.winnerTeamId == firstId)
        Spacer(Modifier.height(6.dp))
        TeamScoreLine(secondName, "${second.totalRuns}/${second.totalWickets}", second.totalOversString, match.winnerTeamId == secondId)
        Spacer(Modifier.height(10.dp))
        Text(match.resultText.ifEmpty { "Result pending" }, style = MaterialTheme.typography.labelLarge, color = Lsl.Gold)
        if (match.topPerformer.isNotEmpty()) {
            Text("Player of the match: ${match.topPerformer}", style = MaterialTheme.typography.labelMedium, color = Lsl.Mist)
        }
    }
}

@Composable
private fun TeamScoreLine(name: String, score: String, overs: String, won: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Monogram(name, size = 26.dp)
        Spacer(Modifier.width(10.dp))
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            color = if (won) Lsl.Chalk else Lsl.Mist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (won) {
            Icon(Icons.Default.EmojiEvents, "Winner", tint = Lsl.Gold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            score,
            fontFamily = Scoreboard,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = if (won) Lsl.Chalk else Lsl.Mist
        )
        Text(" ($overs)", style = MaterialTheme.typography.labelSmall, color = Lsl.Fade)
    }
}
