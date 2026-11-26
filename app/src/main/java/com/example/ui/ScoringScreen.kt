package com.example.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BallInfo
import com.example.data.BatsmanLiveStats
import com.example.data.CricketScorer
import com.example.data.InningsScorecard
import com.example.data.JsonHelper
import com.example.data.MatchEntity
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WicketTypes = listOf(
    "BOWLED" to "Bowled",
    "CAUGHT" to "Caught",
    "RUN_OUT" to "Run out",
    "STUMPED" to "Stumped",
    "LBW" to "LBW",
    "HIT_WICKET" to "Hit wicket"
)

private val ExtraTypes = listOf(
    "WIDE" to "Wide",
    "NO_BALL" to "No ball",
    "BYE" to "Bye",
    "LEG_BYE" to "Leg bye"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoringScreen(
    viewModel: LocoViewModel,
    onBackToDashboard: () -> Unit,
    onViewScorecardClick: () -> Unit
) {
    val match by viewModel.activeMatch.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val showDismissal by viewModel.showDismissalDialog.collectAsState()
    val showBowlerSelect by viewModel.showBowlerSelectDialog.collectAsState()
    val showEditScore by viewModel.showEditScoreDialog.collectAsState()

    BackHandler { onBackToDashboard() }

    // Scorers stand in the sun for an hour: never let the screen sleep mid-over.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val activeMatch = match
    if (activeMatch == null) {
        Box(Modifier.fillMaxSize().background(Lsl.Pitch), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Lsl.Mint)
        }
        return
    }

    // Rosters depend on teams being loaded too
    val battingRoster = remember(activeMatch.currentInnings, activeMatch.battingFirstTeamId, teams) {
        viewModel.rosterFor(viewModel.battingTeamId(activeMatch))
    }
    val bowlingRoster = remember(activeMatch.currentInnings, activeMatch.battingFirstTeamId, teams) {
        viewModel.rosterFor(viewModel.bowlingTeamId(activeMatch))
    }
    val ballsList = remember(activeMatch.ballsJson) {
        JsonHelper.listFromJson(activeMatch.ballsJson, BallInfo::class.java)
    }
    val info = remember(ballsList, activeMatch.currentInnings, battingRoster) {
        CricketScorer.calculateInningsScorecard(ballsList, activeMatch.currentInnings, battingRoster)
    }
    val battingTeam = viewModel.teamName(activeMatch, viewModel.battingTeamId(activeMatch))

    var selectedExtra by rememberSaveable { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var pickBatterForStrikerEnd by remember { mutableStateOf<Boolean?>(null) }
    var confirmEndInnings by remember { mutableStateOf(false) }
    var inningsBreak by remember { mutableStateOf<ScoreEvent.InningsBreak?>(null) }
    var overToast by remember { mutableStateOf<ScoreEvent.OverComplete?>(null) }

    // Celebration burst
    var burst by remember { mutableStateOf<ScoreEvent?>(null) }
    var burstKey by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                is ScoreEvent.Four, is ScoreEvent.Six, is ScoreEvent.Wicket -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (!reduceMotion) { burst = e; burstKey++ }
                }
                is ScoreEvent.OverComplete -> overToast = e
                is ScoreEvent.InningsBreak -> inningsBreak = e
            }
        }
    }
    LaunchedEffect(overToast) {
        if (overToast != null) { delay(2200); overToast = null }
    }

    fun score(runs: Int) {
        viewModel.recordBall(runs = runs, extraType = selectedExtra)
        selectedExtra = null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Lsl.Pitch)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val compact = maxHeight < 660.dp
            val gap = if (compact) 8.dp else 12.dp

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                // ---- Top bar --------------------------------------------------
                Row(
                    Modifier.fillMaxWidth().height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to home", tint = Lsl.Chalk)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${activeMatch.teamAName} v ${activeMatch.teamBName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Lsl.Chalk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${if (activeMatch.currentInnings == 1) "1st" else "2nd"} innings, ${activeMatch.overs} overs",
                            style = MaterialTheme.typography.labelSmall,
                            color = Lsl.Moss
                        )
                    }
                    IconButton(onClick = onViewScorecardClick, modifier = Modifier.testTag("open_scorecard")) {
                        Icon(Icons.Default.Leaderboard, "Full scorecard", tint = Lsl.Mint)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, "More options", tint = Lsl.Chalk)
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = Lsl.TurfRaised
                        ) {
                            DropdownMenuItem(
                                text = { Text("Correct a ball", color = Lsl.Chalk) },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = Lsl.Moss) },
                                enabled = ballsList.any { it.innings == activeMatch.currentInnings },
                                onClick = { menuOpen = false; viewModel.showEditScoreDialog.value = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Change bowler", color = Lsl.Chalk) },
                                leadingIcon = { Icon(Icons.Default.SportsBaseball, null, tint = Lsl.Moss) },
                                onClick = { menuOpen = false; viewModel.showBowlerSelectDialog.value = true }
                            )
                            DropdownMenuItem(
                                text = { Text("End innings now", color = Lsl.Tape) },
                                leadingIcon = { Icon(Icons.Default.StopCircle, null, tint = Lsl.Tape) },
                                onClick = { menuOpen = false; confirmEndInnings = true }
                            )
                        }
                    }
                }

                // ---- Scoreboard ----------------------------------------------
                Scoreboard(
                    match = activeMatch,
                    info = info,
                    battingTeam = battingTeam,
                    compact = compact
                )

                // ---- Players at the crease -----------------------------------
                CreasePanel(
                    match = activeMatch,
                    info = info,
                    compact = compact,
                    onSwap = { viewModel.swapStrike() },
                    onPickBatter = { strikerEnd -> pickBatterForStrikerEnd = strikerEnd },
                    onPickBowler = { viewModel.showBowlerSelectDialog.value = true }
                )

                // ---- Extras modifiers + undo ---------------------------------
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExtraTypes.forEach { (type, label) ->
                        ExtraToggle(
                            text = label,
                            selected = selectedExtra == type,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedExtra = if (selectedExtra == type) null else type }
                        )
                    }
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Lsl.TurfRaised)
                            .pressable(enabled = ballsList.isNotEmpty()) { viewModel.undoLastBall() }
                            .testTag("undo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo last ball",
                            tint = if (ballsList.isNotEmpty()) Lsl.Chalk else Lsl.Fade
                        )
                    }
                }

                // Hint text explains what the keys mean while an extra is armed
                AnimatedVisibility(
                    visible = selectedExtra != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Text(
                        when (selectedExtra) {
                            "WIDE" -> "Wide: +1 extra. Tap the runs they ran (usually 0)."
                            "NO_BALL" -> "No ball: +1 extra. Tap the runs off the bat."
                            "BYE" -> "Byes: tap the runs they ran."
                            else -> "Leg byes: tap the runs they ran."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Lsl.Lilac,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---- Keypad: takes whatever height is left ------------------
                Keypad(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    armedExtra = selectedExtra,
                    onRuns = ::score,
                    onWicket = { viewModel.showDismissalDialog.value = true }
                )
            }
        }

        // Over-complete toast
        AnimatedVisibility(
            visible = overToast != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(top = 56.dp),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            val t = overToast
            if (t != null) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Lsl.Chalk)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Lsl.MintDeep, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "End of over ${t.overNumber}: ${t.runs} run${if (t.runs == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Lsl.Pitch
                    )
                }
            }
        }

        // Boundary / wicket burst
        CelebrationBurst(event = burst, key = burstKey, modifier = Modifier.align(Alignment.Center))
    }

    // ------------------------------------------------------------------ sheets

    if (showDismissal) {
        WicketSheet(
            match = activeMatch,
            info = info,
            battingRoster = battingRoster,
            armedExtra = selectedExtra,
            onDismiss = { viewModel.showDismissalDialog.value = false },
            onConfirm = { dismissed, type, runs, incoming ->
                viewModel.showDismissalDialog.value = false
                viewModel.recordBall(
                    runs = runs,
                    extraType = selectedExtra,
                    isWicket = true,
                    wicketType = type,
                    dismissedPlayer = dismissed,
                    incomingBatter = incoming
                )
                selectedExtra = null
            }
        )
    }

    if (showBowlerSelect) {
        BowlerSheet(
            roster = bowlingRoster,
            info = info,
            current = activeMatch.currentBowler,
            overJustEnded = info.legalBallsBowled > 0 && info.legalBallsInCurrentOver == 0,
            onDismiss = { viewModel.showBowlerSelectDialog.value = false },
            onPick = { viewModel.selectNewBowler(it) }
        )
    }

    pickBatterForStrikerEnd?.let { strikerEnd ->
        BatterSheet(
            atStrikerEnd = strikerEnd,
            match = activeMatch,
            info = info,
            roster = battingRoster,
            onDismiss = { pickBatterForStrikerEnd = null },
            onPick = { name ->
                viewModel.setBatter(name, strikerEnd)
                pickBatterForStrikerEnd = null
            }
        )
    }

    if (showEditScore) {
        EditBallsSheet(
            balls = ballsList,
            innings = activeMatch.currentInnings,
            onDismiss = { viewModel.showEditScoreDialog.value = false },
            onSave = { idx, ball -> viewModel.editBallAt(idx, ball) }
        )
    }

    if (confirmEndInnings) {
        ConfirmDialog(
            title = if (activeMatch.currentInnings == 1) "End the 1st innings?" else "End the match?",
            body = if (activeMatch.currentInnings == 1)
                "$battingTeam's innings closes at ${info.totalRuns}/${info.totalWickets}. The chase starts next."
            else "The result is decided on the current score of ${info.totalRuns}/${info.totalWickets}.",
            confirmText = "End innings",
            onConfirm = { viewModel.endInningsNow() },
            onDismiss = { confirmEndInnings = false }
        )
    }

    inningsBreak?.let { ib ->
        AlertDialog(
            onDismissRequest = { inningsBreak = null },
            containerColor = Lsl.Turf,
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Default.SportsCricket, null, tint = Lsl.Mint, modifier = Modifier.size(32.dp)) },
            title = { Text("Innings break", style = MaterialTheme.typography.headlineSmall, color = Lsl.Chalk) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${ib.target}",
                        fontFamily = Scoreboard,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 64.sp,
                        color = Lsl.Mint
                    )
                    Text(
                        "${ib.chasingTeam} need ${ib.target} to win off ${activeMatch.overs * 6} balls",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Lsl.Chalk,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                PrimaryButton(text = "Start the chase", onClick = { inningsBreak = null })
            }
        )
    }
}

// ============================================================================
// Scoreboard
// ============================================================================

@Composable
private fun Scoreboard(
    match: MatchEntity,
    info: InningsScorecard,
    battingTeam: String,
    compact: Boolean
) {
    val maxBalls = match.overs * 6
    val progress by animateFloatAsState(
        targetValue = if (maxBalls > 0) info.legalBallsBowled / maxBalls.toFloat() else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "overs-progress"
    )

    Panel(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = if (compact) 12.dp else 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Monogram(battingTeam, size = 22.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                battingTeam,
                style = MaterialTheme.typography.titleSmall,
                color = Lsl.Chalk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text("CRR ${info.runRate.fmt()}", style = MaterialTheme.typography.labelMedium, color = Lsl.Moss)
        }

        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            RollingNumber(
                value = info.totalRuns,
                fontSize = if (compact) 52.sp else 68.sp,
                color = Lsl.Chalk,
                modifier = Modifier.testTag("score_runs")
            )
            Text(
                "/",
                fontFamily = Scoreboard,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 36.sp else 44.sp,
                color = Lsl.Fade,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = if (compact) 4.dp else 6.dp)
            )
            RollingNumber(
                value = info.totalWickets,
                fontSize = if (compact) 40.sp else 50.sp,
                color = if (info.totalWickets > 0) Lsl.Tape else Lsl.Moss,
                modifier = Modifier.padding(bottom = if (compact) 2.dp else 3.dp)
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 8.dp)) {
                Text(
                    info.totalOversString,
                    fontFamily = Scoreboard,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 24.sp else 28.sp,
                    color = Lsl.Chalk
                )
                Text("of ${match.overs} overs", style = MaterialTheme.typography.labelSmall, color = Lsl.Moss)
            }
        }

        // Overs progress rail
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Lsl.TurfRaised)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Lsl.Mint)
            )
        }

        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))

        // Situation line: chase equation or projection
        if (match.currentInnings == 2 && match.targetRuns > 0) {
            val need = (match.targetRuns - info.totalRuns).coerceAtLeast(0)
            val ballsLeft = (maxBalls - info.legalBallsBowled).coerceAtLeast(0)
            val rrr = if (ballsLeft > 0) need / (ballsLeft / 6.0) else 0.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Need $need off $ballsLeft",
                    style = MaterialTheme.typography.titleSmall,
                    color = Lsl.Sun,
                    modifier = Modifier.weight(1f)
                )
                Text("Target ${match.targetRuns}  RRR ${rrr.fmt()}", style = MaterialTheme.typography.labelMedium, color = Lsl.Moss)
            }
        } else {
            val projected = if (info.legalBallsBowled > 0) (info.runRate * match.overs).toInt() else 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (info.legalBallsBowled > 0) "Projected $projected" else "First ball coming up",
                    style = MaterialTheme.typography.labelMedium,
                    color = Lsl.Moss,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Extras ${info.totalExtrasSum}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Lsl.Moss
                )
            }
        }

        Spacer(Modifier.height(if (compact) 8.dp else 12.dp))
        OverTrack(info = info, compact = compact)
    }
}

/** Number that rolls up when it grows and down when it shrinks (undo). */
@Composable
fun RollingNumber(value: Int, fontSize: androidx.compose.ui.unit.TextUnit, color: Color, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            val up = targetState > initialState
            (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 500f)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(150)))
                .togetherWith(slideOutVertically(tween(150)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(120)))
                .using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "rolling-number"
    ) { v ->
        Text(
            "$v",
            fontFamily = Scoreboard,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize,
            lineHeight = fontSize,
            color = color
        )
    }
}

/** Six slots for the over in progress; extras push extra chips in. */
@Composable
private fun OverTrack(info: InningsScorecard, compact: Boolean) {
    val current = info.thisOverBalls
    val lastComplete = info.overs.lastOrNull()?.takeIf { it.isComplete }
    val showingLast = current.isEmpty() && lastComplete != null
    val labels = if (showingLast) lastComplete!!.labels else current
    val emptySlots = if (showingLast) 0 else (6 - info.legalBallsInCurrentOver).coerceAtLeast(0)
    val chip: Dp = if (compact) 30.dp else 34.dp
    val listState = rememberLazyListState()

    LaunchedEffect(labels.size) {
        if (labels.isNotEmpty()) listState.animateScrollToItem(labels.size - 1)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (showingLast) "Last over" else "This over",
            style = MaterialTheme.typography.labelSmall,
            color = Lsl.Moss,
            modifier = Modifier.width(58.dp)
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f),
            userScrollEnabled = true
        ) {
            items(labels.size, key = { "b$it-${labels[it]}" }) { i ->
                var shown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { shown = true }
                val s by animateFloatAsState(
                    if (shown) 1f else 0.3f,
                    spring(dampingRatio = 0.45f, stiffness = 600f),
                    label = "ball-pop"
                )
                BallChip(
                    labels[i],
                    size = chip,
                    modifier = Modifier.graphicsLayer {
                        scaleX = s; scaleY = s
                        alpha = if (showingLast) 0.55f else 1f
                    }
                )
            }
            items(emptySlots) {
                Box(
                    Modifier
                        .size(chip)
                        .drawBehind {
                            drawRoundRect(
                                color = Lsl.Line,
                                cornerRadius = CornerRadius(size.minDimension / 2),
                                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                            )
                        }
                )
            }
        }
        if (showingLast) {
            Text(
                "${lastComplete!!.runs}",
                fontFamily = Scoreboard,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Lsl.Moss
            )
        } else {
            Text(
                "${info.overs.lastOrNull()?.takeIf { !it.isComplete }?.runs ?: 0}",
                fontFamily = Scoreboard,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Lsl.Chalk
            )
        }
    }
}

// ============================================================================
// Crease panel
// ============================================================================

@Composable
private fun CreasePanel(
    match: MatchEntity,
    info: InningsScorecard,
    compact: Boolean,
    onSwap: () -> Unit,
    onPickBatter: (Boolean) -> Unit,
    onPickBowler: () -> Unit
) {
    val bowler = info.bowlers[match.currentBowler]
    Panel(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = if (compact) 4.dp else 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BatterRow(match.currentStriker, info.batsmen[match.currentStriker], true, compact) { onPickBatter(true) }
                BatterRow(match.currentNonStriker, info.batsmen[match.currentNonStriker], false, compact) { onPickBatter(false) }
            }
            // Swap ends: rotates on press so the change is visible
            var turns by remember { mutableIntStateOf(0) }
            val rot by animateFloatAsState(turns * 180f, tween(280, easing = FastOutSlowInEasing), label = "swap-rot")
            Box(
                Modifier
                    .padding(end = 4.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Lsl.TurfRaised)
                    .pressable { turns++; onSwap() }
                    .testTag("swap_strike"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.SwapVert,
                    contentDescription = "Swap strike",
                    tint = Lsl.Mint,
                    modifier = Modifier.graphicsLayer { rotationZ = rot }
                )
            }
        }
        HorizontalDivider(color = Lsl.Line, modifier = Modifier.padding(horizontal = 10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compact) 38.dp else 44.dp)
                .clip(RoundedCornerShape(10.dp))
                .pressable(pressedScale = 0.98f, onClick = onPickBowler)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SportsBaseball, null, tint = Lsl.Moss, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                match.currentBowler.ifEmpty { "Pick a bowler" },
                style = MaterialTheme.typography.titleSmall,
                color = Lsl.Chalk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${bowler?.oversString ?: "0.0"}-${bowler?.runsConceded ?: 0}-${bowler?.wickets ?: 0}",
                fontFamily = Scoreboard,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Lsl.Chalk
            )
            Spacer(Modifier.width(8.dp))
            Text("Econ ${(bowler?.economy ?: 0.0).fmt(1)}", style = MaterialTheme.typography.labelSmall, color = Lsl.Moss)
        }
        if (!compact) {
            Text(
                "Partnership ${info.partnership.runs} (${info.partnership.balls})",
                style = MaterialTheme.typography.labelSmall,
                color = Lsl.Fade,
                modifier = Modifier.padding(start = 34.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun BatterRow(name: String, stats: BatsmanLiveStats?, striker: Boolean, compact: Boolean, onClick: () -> Unit) {
    val dot by animateColorAsState(if (striker) Lsl.Mint else Color.Transparent, tween(220), label = "strike-dot")
    val nameColor by animateColorAsState(if (striker) Lsl.Chalk else Lsl.Moss, tween(220), label = "strike-name")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 38.dp else 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(16.dp)
                .border(1.5.dp, if (striker) Lsl.Mint else Lsl.Line, CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
                .background(dot)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name.ifEmpty { "Pick a batter" },
            style = MaterialTheme.typography.titleSmall,
            color = nameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            "${stats?.runs ?: 0}",
            fontFamily = Scoreboard,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = if (striker) Lsl.Chalk else Lsl.Moss
        )
        Text(
            " (${stats?.ballsFaced ?: 0})",
            style = MaterialTheme.typography.labelMedium,
            color = Lsl.Moss,
            modifier = Modifier.widthIn(min = 34.dp)
        )
        if (!compact) {
            Text(
                "${stats?.fours ?: 0}×4 ${stats?.sixes ?: 0}×6",
                style = MaterialTheme.typography.labelSmall,
                color = Lsl.Fade,
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

// ============================================================================
// Controls
// ============================================================================

@Composable
private fun ExtraToggle(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Lsl.Lilac else Lsl.TurfRaised, tween(150), label = "x-bg")
    val fg by animateColorAsState(if (selected) Lsl.Pitch else Lsl.Chalk, tween(150), label = "x-fg")
    Box(
        modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .pressable(onClick = onClick)
            .testTag("extra_$text"),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 1)
    }
}

@Composable
private fun Keypad(
    modifier: Modifier,
    armedExtra: String?,
    onRuns: (Int) -> Unit,
    onWicket: () -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0, 1, 2, 3).forEach { r ->
                RunKey(
                    label = if (r == 0) "0" else "$r",
                    caption = when (r) { 0 -> "dot"; 1 -> "single"; 2 -> "double"; else -> "three" },
                    bg = Lsl.TurfRaised,
                    fg = Lsl.Chalk,
                    armed = armedExtra != null,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onRuns(r) }
                )
            }
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RunKey("4", "four", Lsl.Sun, Lsl.Pitch, armedExtra != null, Modifier.weight(1f).fillMaxHeight()) { onRuns(4) }
            RunKey("6", "six", Lsl.Sky, Lsl.Pitch, armedExtra != null, Modifier.weight(1f).fillMaxHeight()) { onRuns(6) }
            RunKey("5", "five", Lsl.TurfRaised, Lsl.Chalk, armedExtra != null, Modifier.weight(0.7f).fillMaxHeight()) { onRuns(5) }
            RunKey("Out", "wicket", Lsl.Tape, Lsl.Pitch, false, Modifier.weight(1.3f).fillMaxHeight(), onClick = onWicket)
        }
    }
}

@Composable
private fun RunKey(
    label: String,
    caption: String,
    bg: Color,
    fg: Color,
    armed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    // Armed extras tint the run keys lilac so it's obvious the next tap is an extra
    val ring by animateColorAsState(if (armed) Lsl.Lilac else Color.Transparent, tween(150), label = "key-ring")
    BoxWithConstraints(
        modifier
            .heightIn(min = 56.dp, max = 120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(2.dp, ring, RoundedCornerShape(18.dp))
            .pressable(pressedScale = 0.93f, onClick = onClick)
            .testTag("score_pad_$label"),
        contentAlignment = Alignment.Center
    ) {
        val big = maxHeight > 76.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                fontFamily = Scoreboard,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if (label.length > 1) (if (big) 30.sp else 24.sp) else (if (big) 40.sp else 30.sp),
                lineHeight = if (big) 40.sp else 30.sp,
                color = fg
            )
            if (big) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.65f))
            }
        }
    }
}

// ============================================================================
// Celebration burst – the single orchestrated moment, triggered by a tap
// ============================================================================

@Composable
private fun CelebrationBurst(event: ScoreEvent?, key: Int, modifier: Modifier) {
    if (event == null) return
    val (text, color) = when (event) {
        is ScoreEvent.Four -> "Four!" to Lsl.Sun
        is ScoreEvent.Six -> "Six!" to Lsl.Sky
        is ScoreEvent.Wicket -> "Out!" to Lsl.Tape
        else -> return
    }
    val scale = remember(key) { Animatable(0.4f) }
    val alpha = remember(key) { Animatable(0f) }
    val ring = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        alpha.snapTo(1f)
        coroutineScope {
            launch { ring.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
            scale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 380f))
        }
        delay(320)
        alpha.animateTo(0f, tween(260))
    }
    Box(modifier.graphicsLayer { this.alpha = alpha.value }, contentAlignment = Alignment.Center) {
        // Expanding ring
        Box(
            Modifier
                .size(220.dp)
                .graphicsLayer {
                    scaleX = 0.4f + ring.value * 0.9f
                    scaleY = 0.4f + ring.value * 0.9f
                    this.alpha = (1f - ring.value).coerceIn(0f, 1f)
                }
                .border(6.dp, color, CircleShape)
        )
        Text(
            text,
            fontFamily = Scoreboard,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 96.sp,
            color = color,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value; scaleY = scale.value
                rotationZ = (1f - scale.value) * -12f
            }
        )
    }
}

// ============================================================================
// Sheets
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WicketSheet(
    match: MatchEntity,
    info: InningsScorecard,
    battingRoster: List<String>,
    armedExtra: String?,
    onDismiss: () -> Unit,
    onConfirm: (dismissed: String, type: String, runs: Int, incoming: String?) -> Unit
) {
    val remaining = battingRoster.filter { n ->
        n != match.currentStriker && n != match.currentNonStriker && info.batsmen[n]?.isOut != true
    }
    var dismissed by remember { mutableStateOf(match.currentStriker) }
    var type by remember { mutableStateOf("BOWLED") }
    var runs by remember { mutableIntStateOf(0) }
    var incoming by remember { mutableStateOf(remaining.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Lsl.Turf,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Wicket", style = MaterialTheme.typography.headlineSmall, color = Lsl.Tape)
            if (armedExtra != null) {
                Text(
                    "Recorded on a ${ExtraTypes.first { it.first == armedExtra }.second.lowercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Lsl.Lilac
                )
            }

            SectionTitle("Who's out")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(match.currentStriker, match.currentNonStriker).forEach { n ->
                    ChoiceChip(
                        text = n,
                        selected = dismissed == n,
                        accent = Lsl.Tape,
                        onClick = { dismissed = n },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionTitle("How")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WicketTypes.forEach { (key, label) ->
                    ChoiceChip(text = label, selected = type == key, onClick = { type = key })
                }
            }

            AnimatedVisibility(visible = type == "RUN_OUT") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Runs completed before the run out")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0..3).forEach { r ->
                            ChoiceChip(text = "$r", selected = runs == r, onClick = { runs = r }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            SectionTitle("Next batter")
            if (remaining.isEmpty()) {
                Text(
                    "No batters left. This wicket ends the innings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Lsl.Sun
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    remaining.forEach { n ->
                        ChoiceChip(text = n, selected = incoming == n, onClick = { incoming = n })
                    }
                }
            }

            PrimaryButton(
                text = "Record wicket",
                icon = Icons.Default.Check,
                color = Lsl.Tape,
                contentColor = Lsl.Pitch,
                modifier = Modifier.fillMaxWidth().testTag("confirm_wicket"),
                onClick = { onConfirm(dismissed, type, if (type == "RUN_OUT") runs else 0, incoming) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BowlerSheet(
    roster: List<String>,
    info: InningsScorecard,
    current: String,
    overJustEnded: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Lsl.Turf) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                if (overJustEnded) "Who bowls the next over?" else "Change bowler",
                style = MaterialTheme.typography.headlineSmall,
                color = Lsl.Chalk
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                items(roster) { name ->
                    val s = info.bowlers[name]
                    val blocked = overJustEnded && name == current
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (name == current && !blocked) Lsl.Mint.copy(alpha = 0.14f) else Lsl.TurfRaised)
                            .pressable(enabled = !blocked) { onPick(name) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Monogram(name, size = 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall, color = if (blocked) Lsl.Fade else Lsl.Chalk)
                            Text(
                                if (blocked) "Bowled the last over" else if (s == null) "Yet to bowl" else "Econ ${s.economy.fmt(1)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Lsl.Moss
                            )
                        }
                        if (s != null) {
                            Text(
                                "${s.oversString}-${s.runsConceded}-${s.wickets}",
                                fontFamily = Scoreboard,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (blocked) Lsl.Fade else Lsl.Chalk
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatterSheet(
    atStrikerEnd: Boolean,
    match: MatchEntity,
    info: InningsScorecard,
    roster: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val current = if (atStrikerEnd) match.currentStriker else match.currentNonStriker
    val options = roster.filter { it != current && info.batsmen[it]?.isOut != true }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Lsl.Turf) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                if (atStrikerEnd) "On strike" else "At the non-striker's end",
                style = MaterialTheme.typography.headlineSmall,
                color = Lsl.Chalk
            )
            Text(
                "Replaces $current. Picking the batter at the other end swaps them.",
                style = MaterialTheme.typography.bodySmall,
                color = Lsl.Moss
            )
            Spacer(Modifier.height(12.dp))
            if (options.isEmpty()) {
                Text("Everyone else in the squad is out.", color = Lsl.Moss)
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                items(options) { name ->
                    val s = info.batsmen[name]
                    val other = name == match.currentStriker || name == match.currentNonStriker
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Lsl.TurfRaised)
                            .pressable { onPick(name) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Monogram(name, size = 34.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk)
                            Text(
                                when {
                                    other -> "At the other end"
                                    s?.hasBatted == true -> "${s.runs} (${s.ballsFaced}) not out"
                                    else -> "Yet to bat"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Lsl.Moss
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditBallsSheet(
    balls: List<BallInfo>,
    innings: Int,
    onDismiss: () -> Unit,
    onSave: (Int, BallInfo) -> Unit
) {
    // Keep the index into the FULL list so edits land on the right ball in either innings
    val indexed = balls.withIndex().filter { it.value.innings == innings }.reversed()
    var editing by remember { mutableStateOf<IndexedValue<BallInfo>?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Lsl.Turf) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Correct a ball", style = MaterialTheme.typography.headlineSmall, color = Lsl.Chalk)
            Text("Latest first. Tap a ball to change it.", style = MaterialTheme.typography.bodySmall, color = Lsl.Moss)
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 440.dp)) {
                items(indexed, key = { it.value.id }) { iv ->
                    val b = iv.value
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Lsl.TurfRaised)
                            .pressable(pressedScale = 0.98f) { editing = iv }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${b.overIndex}.${b.ballNumber}",
                            fontFamily = Scoreboard,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Lsl.Moss,
                            modifier = Modifier.width(44.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text("${b.bowler} to ${b.striker}", style = MaterialTheme.typography.bodyMedium, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        BallChip(CricketScorer.ballLabel(b), size = 32.dp)
                    }
                }
            }
        }
    }

    editing?.let { iv ->
        val b = iv.value
        val originalTotal = when (b.extraType) {
            "WIDE" -> b.extraRuns - 1
            "BYE", "LEG_BYE" -> b.extraRuns
            else -> b.runs
        }
        var runs by remember(iv.index) { mutableIntStateOf(originalTotal.coerceAtLeast(0)) }
        var extra by remember(iv.index) { mutableStateOf(b.extraType) }
        var isWicket by remember(iv.index) { mutableStateOf(b.isWicket) }
        var wType by remember(iv.index) { mutableStateOf(b.wicketType ?: "BOWLED") }

        AlertDialog(
            onDismissRequest = { editing = null },
            containerColor = Lsl.Turf,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Ball ${b.overIndex}.${b.ballNumber}", style = MaterialTheme.typography.titleLarge, color = Lsl.Chalk) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${b.bowler} to ${b.striker}", style = MaterialTheme.typography.bodySmall, color = Lsl.Moss)
                    SectionTitle("Runs")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 1, 2, 3, 4, 5, 6).forEach { r ->
                            ChoiceChip("$r", runs == r, { runs = r })
                        }
                    }
                    SectionTitle("Extra")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ChoiceChip("None", extra == null, { extra = null })
                        ExtraTypes.forEach { (k, l) -> ChoiceChip(l, extra == k, { extra = k }, accent = Lsl.Lilac) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = isWicket,
                            onCheckedChange = { isWicket = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Lsl.Tape, checkedThumbColor = Lsl.Pitch)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Wicket on this ball", color = Lsl.Chalk)
                    }
                    if (isWicket) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            WicketTypes.forEach { (k, l) -> ChoiceChip(l, wType == k, { wType = k }, accent = Lsl.Tape) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val edited = b.copy(
                        runs = when (extra) { "WIDE", "BYE", "LEG_BYE" -> 0; else -> runs },
                        extraRuns = when (extra) { "WIDE" -> 1 + runs; "NO_BALL" -> 1; "BYE", "LEG_BYE" -> runs; else -> 0 },
                        isExtra = extra != null,
                        extraType = extra,
                        isWicket = isWicket,
                        wicketType = if (isWicket) wType else null,
                        dismissedPlayer = if (isWicket) (b.dismissedPlayer ?: b.striker) else null
                    )
                    onSave(iv.index, edited)
                    editing = null
                }) { Text("Save ball", color = Lsl.Mint, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel", color = Lsl.Moss) } }
        )
    }
}
