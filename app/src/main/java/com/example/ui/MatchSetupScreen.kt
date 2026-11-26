package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TeamEntity
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val teams by viewModel.teams.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()

    var teamA by remember { mutableStateOf<TeamEntity?>(null) }
    var teamB by remember { mutableStateOf<TeamEntity?>(null) }
    var tournamentId by remember { mutableStateOf<Int?>(null) }
    var overs by remember { mutableIntStateOf(5) }
    var tossWinner by remember { mutableStateOf<TeamEntity?>(null) }
    var battingFirst by remember { mutableStateOf<TeamEntity?>(null) }
    var pickingSlot by remember { mutableStateOf<Int?>(null) } // 0 = A, 1 = B

    // Fixture shortcuts arrive pre-filled
    LaunchedEffect(teams) {
        val p = viewModel.setupPrefill.value ?: return@LaunchedEffect
        if (teams.isEmpty()) return@LaunchedEffect
        teamA = teams.find { it.id == p.teamAId }
        teamB = teams.find { it.id == p.teamBId }
        tournamentId = p.tournamentId
        viewModel.setupPrefill.value = null
    }

    // Reset toss if the teams change
    LaunchedEffect(teamA?.id, teamB?.id) { tossWinner = null; battingFirst = null }

    val playersA = teamA?.playerNames?.toPlayerList().orEmpty()
    val playersB = teamB?.playerNames?.toPlayerList().orEmpty()
    val problem = when {
        teamA == null || teamB == null -> "Pick both teams"
        playersA.size < 2 -> "${teamA!!.name} needs at least 2 players"
        playersB.size < 2 -> "${teamB!!.name} needs at least 2 players"
        battingFirst == null -> "Toss the coin or pick who bats first"
        else -> null
    }

    LslScreen(title = "New match", onBack = onBack) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1 · Teams
                StepHeader(1, "Teams")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamSlot(teamA, "Home team", Modifier.weight(1f).testTag("slot_a")) { pickingSlot = 0 }
                    Text(
                        "v",
                        fontFamily = Scoreboard,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Lsl.Fade,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    TeamSlot(teamB, "Away team", Modifier.weight(1f).testTag("slot_b")) { pickingSlot = 1 }
                }

                if (tournaments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Counts towards")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip("Friendly", tournamentId == null, { tournamentId = null })
                            tournaments.forEach { t ->
                                ChoiceChip(t.name, tournamentId == t.id, { tournamentId = t.id }, accent = Lsl.Sun)
                            }
                        }
                    }
                }

                // 2 · Overs
                StepHeader(2, "Overs per side")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Lsl.TurfRaised)
                            .pressable(enabled = overs > 1) { overs-- },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Remove, "Fewer overs", tint = Lsl.Chalk) }
                    AnimatedContent(
                        targetState = overs,
                        transitionSpec = {
                            val up = targetState > initialState
                            (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
                        },
                        modifier = Modifier.weight(1f),
                        label = "overs"
                    ) { v ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text("$v", fontFamily = Scoreboard, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp, color = Lsl.Chalk, lineHeight = 48.sp)
                            Text("${v * 6} balls each", style = MaterialTheme.typography.labelSmall, color = Lsl.Moss)
                        }
                    }
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(Lsl.TurfRaised)
                            .pressable(enabled = overs < 50) { overs++ },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, "More overs", tint = Lsl.Chalk) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 5, 6, 8, 10, 20).forEach { o ->
                        ChoiceChip("$o", overs == o, { overs = o }, modifier = Modifier.widthIn(min = 48.dp))
                    }
                }

                // 3 · Toss
                StepHeader(3, "Toss")
                TossPanel(
                    teamA = teamA,
                    teamB = teamB,
                    winner = tossWinner,
                    battingFirst = battingFirst,
                    onTossed = { tossWinner = it; battingFirst = null },
                    onChoose = { battingFirst = it }
                )
                Spacer(Modifier.height(8.dp))
            }

            // Pinned call to action
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Lsl.Pitch)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                AnimatedContent(targetState = problem ?: "${battingFirst!!.name} bat first, $overs overs a side", label = "summary") { s ->
                    Text(
                        s,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (problem == null) Lsl.Mint else Lsl.Moss,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(
                    text = "Start match",
                    icon = Icons.Default.SportsCricket,
                    enabled = problem == null,
                    modifier = Modifier.fillMaxWidth().testTag("start_match_submission"),
                    height = 56.dp,
                    onClick = {
                        val a = teamA ?: return@PrimaryButton
                        val b = teamB ?: return@PrimaryButton
                        val bf = battingFirst ?: return@PrimaryButton
                        viewModel.startMatch(
                            teamAId = a.id,
                            teamBId = b.id,
                            teamAName = a.name,
                            teamBName = b.name,
                            overs = overs,
                            battingFirstId = bf.id,
                            teamAPlayers = playersA,
                            teamBPlayers = playersB,
                            tournamentId = tournamentId
                        )
                    }
                )
            }
        }
    }

    pickingSlot?.let { slot ->
        val other = if (slot == 0) teamB else teamA
        ModalBottomSheet(onDismissRequest = { pickingSlot = null }, containerColor = Lsl.Turf) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text(if (slot == 0) "Home team" else "Away team", style = MaterialTheme.typography.headlineSmall, color = Lsl.Chalk)
                Spacer(Modifier.height(12.dp))
                val list = teams.filter { it.id != other?.id }
                if (list.isEmpty()) {
                    Text("No other teams yet. Add one from Teams on the home screen.", color = Lsl.Moss)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 460.dp)) {
                    items(list, key = { it.id }) { t ->
                        val n = t.playerNames.toPlayerList().size
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Lsl.TurfRaised)
                                .pressable {
                                    if (slot == 0) teamA = t else teamB = t
                                    pickingSlot = null
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Monogram(t.name, size = 38.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.name, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk)
                                Text(
                                    "$n player${if (n == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (n < 2) Lsl.Tape else Lsl.Moss
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
private fun StepHeader(n: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).border(1.5.dp, Lsl.Mint, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$n", fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Lsl.Mint)
        }
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Lsl.Chalk)
    }
}

@Composable
private fun TeamSlot(team: TeamEntity?, placeholder: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Lsl.Turf)
            .then(if (team == null) Modifier.border(1.5.dp, Lsl.Line, RoundedCornerShape(20.dp)) else Modifier)
            .pressable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(targetState = team, label = "slot", transitionSpec = {
            (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith fadeOut()
        }) { t ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (t == null) {
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Lsl.TurfRaised), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, null, tint = Lsl.Mint)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(placeholder, style = MaterialTheme.typography.titleSmall, color = Lsl.Moss)
                } else {
                    Monogram(t.name, size = 52.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        t.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Lsl.Chalk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text("${t.playerNames.toPlayerList().size} players", style = MaterialTheme.typography.labelSmall, color = Lsl.Moss)
                }
            }
        }
    }
}

@Composable
private fun TossPanel(
    teamA: TeamEntity?,
    teamB: TeamEntity?,
    winner: TeamEntity?,
    battingFirst: TeamEntity?,
    onTossed: (TeamEntity) -> Unit,
    onChoose: (TeamEntity) -> Unit
) {
    val scope = rememberCoroutineScope()
    val spin = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    var flipping by remember { mutableStateOf(false) }
    val ready = teamA != null && teamB != null

    Panel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The coin
            Box(
                Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        rotationY = spin.value
                        translationY = -lift.value
                        cameraDistance = 12f * density
                    }
                    .clip(CircleShape)
                    .background(if (ready) Lsl.Sun else Lsl.TurfRaised)
                    .border(3.dp, if (ready) Lsl.Sun.copy(alpha = 0.5f) else Lsl.Line, CircleShape)
                    .pressable(enabled = ready && !flipping) {
                        val a = teamA ?: return@pressable
                        val b = teamB ?: return@pressable
                        val result = if ((0..1).random() == 0) a else b
                        flipping = true
                        scope.launch {
                            launch {
                                lift.animateTo(60f, tween(380, easing = FastOutSlowInEasing))
                                lift.animateTo(0f, tween(420, easing = { t -> 1f - (1f - t) * (1f - t) }))
                            }
                            spin.snapTo(0f)
                            spin.animateTo(360f * 5, tween(800, easing = LinearOutSlowInEasing))
                            onTossed(result)
                            flipping = false
                        }
                    }
                    .testTag("toss_coin"),
                contentAlignment = Alignment.Center
            ) {
                val face = winner?.name ?: if (ready) "Toss" else "–"
                Text(
                    if (winner != null && !flipping) initialsOf(face) else if (ready) "Toss" else "–",
                    fontFamily = Scoreboard,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (winner != null && !flipping) 26.sp else 18.sp,
                    color = Lsl.Pitch
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        !ready -> "Pick both teams first"
                        flipping -> "In the air…"
                        winner == null -> "Tap the coin to toss"
                        else -> "${winner.name} won the toss"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = Lsl.Chalk
                )
                Text(
                    if (winner == null) "Or choose who bats first below." else "What do they choose?",
                    style = MaterialTheme.typography.bodySmall,
                    color = Lsl.Moss
                )
            }
        }

        if (ready) {
            Spacer(Modifier.height(14.dp))
            if (winner != null && !flipping) {
                val loser = if (winner.id == teamA!!.id) teamB!! else teamA
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Bat first", battingFirst?.id == winner.id, { onChoose(winner) }, Modifier.weight(1f))
                    ChoiceChip("Bowl first", battingFirst?.id == loser.id, { onChoose(loser) }, Modifier.weight(1f))
                }
            } else if (!flipping) {
                Text("Batting first", style = MaterialTheme.typography.labelMedium, color = Lsl.Moss)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(teamA!!.name, battingFirst?.id == teamA.id, { onChoose(teamA) }, Modifier.weight(1f))
                    ChoiceChip(teamB!!.name, battingFirst?.id == teamB.id, { onChoose(teamB) }, Modifier.weight(1f))
                }
            }
        }
    }
}
