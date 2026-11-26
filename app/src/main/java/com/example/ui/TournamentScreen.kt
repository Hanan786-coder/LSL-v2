package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard

@Composable
fun TournamentScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val tournaments by viewModel.tournaments.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val matches by viewModel.matches.collectAsState()

    var showCreate by remember { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var deleting by remember { mutableStateOf<TournamentEntity?>(null) }

    val selected = tournaments.find { it.id == selectedId }
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            val forward = targetState != null
            (slideInHorizontally(tween(300)) { if (forward) it / 5 else -it / 5 } + fadeIn(tween(220)))
                .togetherWith(slideOutHorizontally(tween(240)) { if (forward) -it / 8 else it / 8 } + fadeOut(tween(140)))
        },
        label = "tournament"
    ) { t ->
        if (t != null) {
            BackHandler { selectedId = null }
            TournamentDetail(
                tournament = t,
                viewModel = viewModel,
                allTeams = teams,
                allMatches = matches,
                onBack = { selectedId = null }
            )
        } else {
            LslScreen(
                title = "Tournaments",
                onBack = onBack,
                floatingActionButton = {
                    if (tournaments.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = { showCreate = true },
                            containerColor = Lsl.Mint,
                            contentColor = Lsl.MintDeep,
                            icon = { Icon(Icons.Default.Add, null) },
                            text = { Text("New league") },
                            modifier = Modifier.testTag("add_tournament_fab")
                        )
                    }
                }
            ) { inner ->
                if (tournaments.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.EmojiEvents,
                        title = "No leagues yet",
                        body = "Create a league, add teams to it, and the points table and fixtures build themselves.",
                        actionText = "Create a league",
                        onAction = { showCreate = true },
                        modifier = Modifier.padding(inner).padding(top = 48.dp)
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(inner),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tournaments, key = { it.id }) { tour ->
                            val tTeams = teams.count { it.tournamentId == tour.id }
                            val tPlayed = matches.count { it.tournamentId == tour.id && it.status == "COMPLETED" }
                            Row(
                                Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Lsl.Turf)
                                    .pressable(pressedScale = 0.98f, onLongClick = { deleting = tour }) { selectedId = tour.id }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Lsl.Sun.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.EmojiEvents, null, tint = Lsl.Sun) }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(tour.name, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "$tTeams team${if (tTeams == 1) "" else "s"}, $tPlayed played",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Lsl.Moss
                                    )
                                }
                                IconButton(onClick = { deleting = tour }) {
                                    Icon(Icons.Default.DeleteOutline, "Delete ${tour.name}", tint = Lsl.Fade)
                                }
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Lsl.Fade)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = Lsl.Turf,
            shape = RoundedCornerShape(24.dp),
            title = { Text("New league", style = MaterialTheme.typography.titleLarge, color = Lsl.Chalk) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("League name") },
                    placeholder = { Text("Basti Summer Cup") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = lslFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tournament_name_input")
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) { viewModel.createTournament(name); showCreate = false }
                }, enabled = name.isNotBlank()) {
                    Text("Create league", color = if (name.isNotBlank()) Lsl.Mint else Lsl.Fade, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel", color = Lsl.Moss) } }
        )
    }

    deleting?.let { t ->
        ConfirmDialog(
            title = "Delete ${t.name}?",
            body = "The league and its table are removed. Teams and finished matches stay.",
            confirmText = "Delete league",
            onConfirm = { viewModel.deleteTournament(t); if (selectedId == t.id) selectedId = null },
            onDismiss = { deleting = null }
        )
    }
}

// Points statistics holder
data class PointsTableRow(
    val teamId: Int,
    val name: String,
    val played: Int,
    val won: Int,
    val lost: Int,
    val tied: Int,
    val points: Int,
    val runsScored: Int,
    val oversFaced: Double,
    val runsConceded: Int,
    val oversBowled: Double,
    val form: List<Char>
) {
    val netRunRate: Double
        get() {
            val scRate = if (oversFaced > 0) runsScored / oversFaced else 0.0
            val conRate = if (oversBowled > 0) runsConceded / oversBowled else 0.0
            return scRate - conRate
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TournamentDetail(
    tournament: TournamentEntity,
    viewModel: LocoViewModel,
    allTeams: List<TeamEntity>,
    allMatches: List<MatchEntity>,
    onBack: () -> Unit
) {
    val tTeams = allTeams.filter { it.tournamentId == tournament.id }
    val tMatches = allMatches.filter { it.tournamentId == tournament.id }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var addingTeams by remember { mutableStateOf(false) }

    val table = remember(tMatches, tTeams) {
        tTeams.map { team ->
            var played = 0; var won = 0; var lost = 0; var tied = 0
            var rs = 0; var bf = 0; var rc = 0; var bb = 0
            val form = mutableListOf<Char>()
            tMatches.filter { it.status == "COMPLETED" && (it.teamAId == team.id || it.teamBId == team.id) }
                .sortedBy { it.date }
                .forEach { m ->
                    played++
                    when (m.winnerTeamId) {
                        team.id -> { won++; form += 'W' }
                        null -> { tied++; form += 'T' }
                        else -> { lost++; form += 'L' }
                    }
                    val balls = JsonHelper.listFromJson(m.ballsJson, BallInfo::class.java)
                    val mine = if (m.battingFirstTeamId == team.id) 1 else 2
                    val theirs = if (mine == 1) 2 else 1
                    val s = CricketScorer.calculateInningsScorecard(balls, mine, emptyList())
                    val c = CricketScorer.calculateInningsScorecard(balls, theirs, emptyList())
                    rs += s.totalRuns; bf += s.legalBallsBowled
                    rc += c.totalRuns; bb += c.legalBallsBowled
                }
            PointsTableRow(team.id, team.name, played, won, lost, tied, won * 2 + tied, rs, bf / 6.0, rc, bb / 6.0, form.takeLast(5))
        }.sortedWith(compareByDescending<PointsTableRow> { it.points }.thenByDescending { it.netRunRate })
    }

    val fixtures = remember(tTeams) {
        buildList {
            for (i in tTeams.indices) for (j in i + 1 until tTeams.size) add(tTeams[i] to tTeams[j])
        }
    }

    LslScreen(
        title = tournament.name,
        subtitle = "${tTeams.size} teams, ${tMatches.count { it.status == "COMPLETED" }} of ${fixtures.size} fixtures played",
        onBack = onBack,
        actions = {
            IconButton(onClick = { addingTeams = true }) { Icon(Icons.Default.GroupAdd, "Add teams", tint = Lsl.Mint) }
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            SegmentedTabs(listOf("Table", "Fixtures"), tab) { tab = it }
            AnimatedContent(targetState = tab, label = "t-tab", transitionSpec = {
                val d = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(240)) { it / 6 * d } + fadeIn()) togetherWith (slideOutHorizontally(tween(200)) { -it / 6 * d } + fadeOut())
            }) { t ->
                if (tTeams.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.GroupAdd,
                        title = "No teams in this league",
                        body = "Add at least two teams to build the table and fixtures.",
                        actionText = "Add teams",
                        onAction = { addingTeams = true }
                    )
                } else if (t == 0) {
                    PointsTable(table)
                } else {
                    Fixtures(
                        fixtures = fixtures,
                        played = tMatches,
                        onStart = { a, b -> viewModel.setupFixture(a.id, b.id, tournament.id) },
                        onOpen = { viewModel.openMatch(it) }
                    )
                }
            }
        }
    }

    if (addingTeams) {
        ModalBottomSheet(onDismissRequest = { addingTeams = false }, containerColor = Lsl.Turf) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("Teams in ${tournament.name}", style = MaterialTheme.typography.headlineSmall, color = Lsl.Chalk)
                Text("Tap to add or remove.", style = MaterialTheme.typography.bodySmall, color = Lsl.Moss)
                Spacer(Modifier.height(12.dp))
                if (allTeams.isEmpty()) Text("Create teams first from the home screen.", color = Lsl.Moss)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 460.dp)) {
                    items(allTeams, key = { it.id }) { team ->
                        val inThis = team.tournamentId == tournament.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (inThis) Lsl.Mint.copy(alpha = 0.14f) else Lsl.TurfRaised)
                                .pressable {
                                    viewModel.updateTeam(team, team.name, team.playerNames.toPlayerList(), if (inThis) null else tournament.id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Monogram(team.name, size = 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(team.name, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk, modifier = Modifier.weight(1f))
                            Icon(
                                if (inThis) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                                contentDescription = if (inThis) "In league" else "Not in league",
                                tint = if (inThis) Lsl.Mint else Lsl.Fade
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PointsTable(table: List<PointsTableRow>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    ) {
        item {
            Row(Modifier.padding(horizontal = 12.dp)) {
                Text("Team", style = MaterialTheme.typography.labelMedium, color = Lsl.Moss, modifier = Modifier.weight(1f))
                listOf("P" to 26.dp, "W" to 26.dp, "L" to 26.dp, "Pts" to 34.dp, "NRR" to 54.dp).forEach { (h, w) ->
                    Text(h, style = MaterialTheme.typography.labelMedium, color = Lsl.Moss, textAlign = TextAlign.End, modifier = Modifier.width(w))
                }
            }
        }
        itemsIndexed(table, key = { _, r -> r.teamId }) { rank, r ->
            Column(
                Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (rank == 0 && r.played > 0) Lsl.Sun.copy(alpha = 0.10f) else Lsl.Turf)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${rank + 1}",
                        fontFamily = Scoreboard,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (rank == 0 && r.played > 0) Lsl.Sun else Lsl.Fade,
                        modifier = Modifier.width(22.dp)
                    )
                    Text(r.name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("${r.played}", color = Lsl.Moss, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                    Text("${r.won}", color = Lsl.Moss, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                    Text("${r.lost}", color = Lsl.Moss, textAlign = TextAlign.End, modifier = Modifier.width(26.dp))
                    Text(
                        "${r.points}",
                        fontFamily = Scoreboard,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Lsl.Chalk,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(34.dp)
                    )
                    Text(
                        (if (r.netRunRate >= 0) "+" else "") + r.netRunRate.fmt(2),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (r.netRunRate >= 0) Lsl.Mint else Lsl.Tape,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(54.dp)
                    )
                }
                if (r.form.isNotEmpty()) {
                    Row(Modifier.padding(start = 22.dp, top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        r.form.forEach { c ->
                            Box(
                                Modifier.size(18.dp).clip(CircleShape).background(
                                    when (c) { 'W' -> Lsl.Mint; 'L' -> Lsl.Tape; else -> Lsl.Fade }
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$c", fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Lsl.Pitch)
                            }
                        }
                    }
                }
            }
        }
        item {
            Text(
                "2 points for a win, 1 for a tie. Ties on points are split by net run rate.",
                style = MaterialTheme.typography.labelSmall,
                color = Lsl.Fade,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun Fixtures(
    fixtures: List<Pair<TeamEntity, TeamEntity>>,
    played: List<MatchEntity>,
    onStart: (TeamEntity, TeamEntity) -> Unit,
    onOpen: (MatchEntity) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize().navigationBarsPadding()
    ) {
        if (fixtures.isEmpty()) {
            item { Text("Add one more team to create fixtures.", color = Lsl.Moss, modifier = Modifier.padding(16.dp)) }
        }
        items(fixtures, key = { "${it.first.id}-${it.second.id}" }) { (a, b) ->
            fun pair(m: MatchEntity) = (m.teamAId == a.id && m.teamBId == b.id) || (m.teamAId == b.id && m.teamBId == a.id)
            val done = played.filter { it.status == "COMPLETED" && pair(it) }.maxByOrNull { it.date }
            val live = played.find { it.status == "ONGOING" && pair(it) }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Lsl.Turf)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Monogram(a.name, size = 24.dp); Spacer(Modifier.width(8.dp))
                        Text(a.name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Monogram(b.name, size = 24.dp); Spacer(Modifier.width(8.dp))
                        Text(b.name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (done != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(done.resultText, style = MaterialTheme.typography.labelMedium, color = Lsl.Sun)
                    }
                }
                Spacer(Modifier.width(8.dp))
                when {
                    live != null -> QuietButton("Resume", { onOpen(live) }, icon = Icons.Default.PlayArrow, tint = Lsl.Tape)
                    done != null -> QuietButton("Scorecard", { onOpen(done) })
                    else -> PrimaryButton("Play", { onStart(a, b) }, icon = Icons.Default.SportsCricket, height = 44.dp)
                }
            }
        }
    }
}
