package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BallInfo
import com.example.data.CricketScorer
import com.example.data.JsonHelper

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ScoringScreen(
    viewModel: LocoViewModel,
    onBackToDashboard: () -> Unit,
    onViewScorecardClick: () -> Unit
) {
    val match by viewModel.activeMatch.collectAsState()
    
    val showDismissal by viewModel.showDismissalDialog.collectAsState()
    val showBowlerSelect by viewModel.showBowlerSelectDialog.collectAsState()
    val showEditScore by viewModel.showEditScoreDialog.collectAsState()

    // Temporary modifiers
    var selectedExtraType by remember { mutableStateOf<String?>(null) } // "WIDE", "NO_BALL", "BYE", "LEG_BYE"
    
    if (match == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text("No active match loaded.", color = Color.White)
        }
        return
    }

    val activeMatch = match!!
    val battingRoster = viewModel.getBattingTeamPlayers()
    val bowlingRoster = viewModel.getBowlingTeamPlayers()

    val ballsList = remember(activeMatch.ballsJson) {
        JsonHelper.listFromJson(activeMatch.ballsJson, BallInfo::class.java)
    }

    // Live calculations
    val currentInnings = activeMatch.currentInnings
    val info = remember(ballsList, currentInnings) {
        CricketScorer.calculateInningsScorecard(ballsList, currentInnings, battingRoster)
    }

    val strikerStat = info.batsmen[activeMatch.currentStriker]
    val nonStrikerStat = info.batsmen[activeMatch.currentNonStriker]
    val bowlerStat = info.bowlers[activeMatch.currentBowler]

    var selectedBallToEditIndex by remember { mutableStateOf<Int?>(null) }
    var showDialogEditBall by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "${activeMatch.teamAName} vs ${activeMatch.teamBName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            "Overs: ${activeMatch.overs} • Gully Cricket Standard",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onViewScorecardClick) {
                        Icon(Icons.Default.Assessment, contentDescription = "Live Scorecard", tint = Color(0xFF10B981))
                    }
                    TextButton(onClick = onViewScorecardClick) {
                        Text("Scorecard", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E293B))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live score screen header
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Batting team name
                        val battingTeamName = if (activeMatch.currentInnings == 1) {
                            if (activeMatch.battingFirstTeamId == activeMatch.teamAId) activeMatch.teamAName else activeMatch.teamBName
                        } else {
                            if (activeMatch.battingFirstTeamId == activeMatch.teamAId) activeMatch.teamBName else activeMatch.teamAName
                        }

                        Text(
                            battingTeamName.uppercase(),
                            color = Color(0xFF10B981),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )

                        Text(
                            "INNINGS ${activeMatch.currentInnings}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Huge Cricket Score display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            "${info.totalRuns}-${info.totalWickets}",
                            color = Color.White,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "(${info.totalOversString} ov)",
                            color = Color(0xFF94A3B8),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "CRR: ${String.format("%.2f", info.runRate)}",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (activeMatch.currentInnings == 2) {
                            val runsNeeded = activeMatch.targetRuns - info.totalRuns
                            val maxBalls = activeMatch.overs * 6
                            val ballsRemaining = maxBalls - info.legalBallsBowled
                            val requiredRR = if (ballsRemaining > 0) (runsNeeded.toDouble() / (ballsRemaining.toDouble() / 6.0)) else 0.0

                            Text(
                                "Need ${runsNeeded} runs off ${ballsRemaining} balls (RRR: ${String.format("%.2f", requiredRR)})",
                                color = Color(0xFFF59E0B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "Extras: ${info.totalExtrasSum} (Wd ${info.extras["WD"] ?: 0}, Nb ${info.extras["NB"] ?: 0}, B ${info.extras["BYE"] ?: 0}, Lb ${info.extras["LB"] ?: 0})",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Striker/Non-Striker Stats Block
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BATSMAN", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.width(180.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("R", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            Text("B", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            Text("4s", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            Text("6s", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            Text("SR", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Striker Row
                    InteractiveBatsmanRow(
                        name = activeMatch.currentStriker,
                        stats = strikerStat,
                        isStriker = true,
                        onSwap = {
                            viewModel.replaceDismissedBatsman(activeMatch.currentStriker, isStriker = false)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Non-Striker Row
                    InteractiveBatsmanRow(
                        name = activeMatch.currentNonStriker,
                        stats = nonStrikerStat,
                        isStriker = false,
                        onSwap = {
                            viewModel.replaceDismissedBatsman(activeMatch.currentNonStriker, isStriker = true)
                        }
                    )
                }
            }

            // Bowler Stats Block
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BOWLER", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.width(180.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("O", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text("R", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text("W", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text("ECON", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SportsScore, contentDescription = "Bowler icon", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                activeMatch.currentBowler.ifEmpty { "Choose bowler" },
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Row(modifier = Modifier.width(180.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(bowlerStat?.oversString ?: "0.0", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text("${bowlerStat?.runsConceded ?: 0}", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text("${bowlerStat?.wickets ?: 0}", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text(
                                if (bowlerStat != null) String.format("%.2f", bowlerStat.economy) else "0.00",
                                color = Color.White,
                                fontSize = 14.sp,
                                modifier = Modifier.width(50.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.showBowlerSelectDialog.value = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Change Bowler", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Ball history ribbon (Cricbuzz-like horizontal over feed)
            Column {
                Text(
                    "THIS OVER",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (info.thisOverBalls.isEmpty()) {
                        item {
                            Text(
                                "No legal balls bowled yet in this over.",
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(info.thisOverBalls) { label ->
                            BallLabelCircle(label = label)
                        }
                    }
                }
            }

            // Ball scoring layout controls
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "EXTRAS MODIFIER",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Extras Modifier Selection Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val extraTypes = listOf("WIDE" to "Wide (+1)", "NO_BALL" to "No Ball (+1)", "BYE" to "Bye", "LEG_BYE" to "Leg Bye")
                        extraTypes.forEach { (type, label) ->
                            val isSelected = selectedExtraType == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF10B981) else Color(0xFF475569),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .background(
                                        if (isSelected) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        selectedExtraType = if (isSelected) null else type
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Large Scoring Keys Pad
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "QUICK SCORING PADS (One-Handed)",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Row 1: 0, 1, 2, 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(0, 1, 2, 3).forEach { runs ->
                        ScoringButton(
                            label = "$runs",
                            subLabel = if (runs == 0) "Dot" else if (runs == 1) "Single" else if (runs == 2) "Couple" else "Triple",
                            modifier = Modifier.weight(1f),
                            backgroundColor = Color(0xFF1E293B),
                            textColor = Color.White,
                            onClick = {
                                viewModel.recordBall(runs = runs, extraType = selectedExtraType)
                                selectedExtraType = null // Reset
                            }
                        )
                    }
                }

                // Row 2: 4, 6 and RED Wicket Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ScoringButton(
                        label = "4",
                        subLabel = "FOUR!",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF065F46),
                        textColor = Color(0xFF34D399),
                        onClick = {
                            viewModel.recordBall(runs = 4, extraType = selectedExtraType)
                            selectedExtraType = null
                        }
                    )
                    ScoringButton(
                        label = "6",
                        subLabel = "SIX!",
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF1E3A8A),
                        textColor = Color(0xFF60A5FA),
                        onClick = {
                            viewModel.recordBall(runs = 6, extraType = selectedExtraType)
                            selectedExtraType = null
                        }
                    )
                    ScoringButton(
                        label = "WICKET",
                        subLabel = "OUT!",
                        modifier = Modifier.weight(1.2f),
                        backgroundColor = Color(0xFF7F1D1D),
                        textColor = Color(0xFFFCA5A5),
                        onClick = {
                            viewModel.showDismissalDialog.value = true
                        }
                    )
                }
            }

            // Undo & Edit actions at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.undoLastBall() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Undo Last Ball", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (ballsList.isNotEmpty()) {
                            viewModel.showEditScoreDialog.value = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.weight(1f),
                    enabled = ballsList.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit score", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Log List", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // --- DIALOGS FOR SCORING SCREEN DETAILS ---

    // 1. Wicket / Dismissal Dialog
    if (showDismissal) {
        val remainingBatsmen = battingRoster.filter { name ->
            name != activeMatch.currentStriker && name != activeMatch.currentNonStriker &&
            !(info.batsmen[name]?.isOut ?: false)
        }
        var clickedSelectedBatsmanDismissed by remember { mutableStateOf(activeMatch.currentStriker) }
        var selectedWicketType by remember { mutableStateOf("BOWLED") }
        var selectedIncomingBatsman by remember { mutableStateOf(remainingBatsmen.getOrNull(0) ?: "Next Batter") }

        AlertDialog(
            onDismissRequest = { viewModel.showDismissalDialog.value = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.border(2.dp, Color(0xFFF96116), RoundedCornerShape(24.dp)),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF96116),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "WICKET FALLEN!",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Select the batsman who of this team was dismissed:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(activeMatch.currentStriker, activeMatch.currentNonStriker).forEach { name ->
                            val isSelected = clickedSelectedBatsmanDismissed == name
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFFF96116) else Color(0xFF152035))
                                    .border(1.dp, if (isSelected) Color.White else Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .clickable { clickedSelectedBatsmanDismissed = name }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Text("Method of Wicket:", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    val types = listOf("BOWLED", "CAUGHT", "RUN_OUT", "STUMPED", "LBW")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { type ->
                            val isSelected = selectedWicketType == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF10B981) else Color(0xFF152035))
                                    .border(1.dp, if (isSelected) Color.White else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { selectedWicketType = type }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = type,
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (remainingBatsmen.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Next Batter Incoming:", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        ScrollableIncomingBatsmanSelector(
                            list = remainingBatsmen,
                            selected = selectedIncomingBatsman,
                            onSelect = { selectedIncomingBatsman = it }
                        )
                    } else {
                        Text("No further batsmen remaining in the gully roster.", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wasStriker = clickedSelectedBatsmanDismissed == activeMatch.currentStriker
                        viewModel.recordBall(
                            runs = 0,
                            extraType = selectedExtraType,
                            isWicket = true,
                            wicketType = selectedWicketType,
                            dismissedPlayer = clickedSelectedBatsmanDismissed
                        )
                        selectedExtraType = null
                        if (remainingBatsmen.isNotEmpty()) {
                            viewModel.replaceDismissedBatsman(selectedIncomingBatsman, isStriker = wasStriker)
                        } else {
                            viewModel.showDismissalDialog.value = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DONE Scoring", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDismissalDialog.value = false }) {
                    Text("CANCEL", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 2. Select Bowler Dialog
    if (showBowlerSelect) {
        AlertDialog(
            onDismissRequest = { viewModel.showBowlerSelectDialog.value = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(24.dp)),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SportsCricket,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "SELECT NEXT BOWLER",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                }
            },
            text = {
                Column {
                    Text("Choose a bowler from the roster for the next over:", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(180.dp)) {
                        items(bowlingRoster.filter { it != activeMatch.currentBowler }.toList()) { bowler ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF152035))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectNewBowler(bowler)
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFACC15), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = bowler,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // 3. Edit score list Dialog
    if (showEditScore) {
        AlertDialog(
            onDismissRequest = { viewModel.showEditScoreDialog.value = false },
            title = { Text("Ball-by-Ball Log Editor", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = Color(0xFF1E293B),
            text = {
                Column {
                    Text("Update or correct any previous ball logged in the active innings:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val currentInningsBalls = ballsList.filter { it.innings == activeMatch.currentInnings }
                    
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(260.dp)) {
                        itemsIndexed(currentInningsBalls) { originalIdx, ballObj ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedBallToEditIndex = originalIdx
                                        showDialogEditBall = true
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Over ${ballObj.overIndex}.${ballObj.ballNumber}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                        Text("${ballObj.striker} faced ${ballObj.bowler}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    
                                    val ballLabel = when {
                                        ballObj.isWicket -> "W"
                                        ballObj.extraType != null -> "${ballObj.runs + ballObj.extraRuns} ${ballObj.extraType}"
                                        else -> "${ballObj.runs} Runs"
                                    }
                                    
                                    Text(
                                        ballLabel,
                                        color = if (ballObj.isWicket) Color(0xFFEF4444) else Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showEditScoreDialog.value = false }) {
                    Text("Done", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Modal to modify specific ball characteristics
    if (showDialogEditBall && selectedBallToEditIndex != null) {
        val targetIdx = selectedBallToEditIndex!!
        val ballToEdit = ballsList.getOrNull(targetIdx)
        if (ballToEdit != null) {
            var tempRuns by remember { mutableStateOf(ballToEdit.runs) }
            var editIsWicket by remember { mutableStateOf(ballToEdit.isWicket) }
            var editWicketType by remember { mutableStateOf(ballToEdit.wicketType ?: "BOWLED") }

            AlertDialog(
                onDismissRequest = { showDialogEditBall = false },
                title = { Text("Edit Delivery details", color = Color.White, fontWeight = FontWeight.Bold) },
                containerColor = Color(0xFF334155),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Over ${ballToEdit.overIndex}.${ballToEdit.ballNumber} faced by ${ballToEdit.striker}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        
                        Text("Runs Scored:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 1, 2, 3, 4, 6).forEach { runVal ->
                                val active = tempRuns == runVal
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (active) Color(0xFF10B981) else Color(0xFF1E293B))
                                        .clickable { tempRuns = runVal }
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$runVal", color = if (active) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = editIsWicket, onCheckedChange = { editIsWicket = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFEF4444)))
                            Text("Is Wicket / Out dismissed", color = Color.White)
                        }

                        if (editIsWicket) {
                            Text("Wicket Dismissal Type:", color = Color.White, fontSize = 12.sp)
                            val wicketTypes = listOf("BOWLED", "CAUGHT", "RUN_OUT", "STUMPED", "LBW")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                wicketTypes.forEach { type ->
                                    InputChip(
                                        selected = editWicketType == type,
                                        onClick = { editWicketType = type },
                                        label = { Text(type, fontSize = 11.sp, color = Color.White) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val updatedBall = ballToEdit.copy(
                                runs = tempRuns,
                                isWicket = editIsWicket,
                                wicketType = if (editIsWicket) editWicketType else null,
                                dismissedPlayer = if (editIsWicket) ballToEdit.striker else null
                            )
                            viewModel.editBallAt(targetIdx, updatedBall)
                            showDialogEditBall = false
                            selectedBallToEditIndex = null
                            viewModel.showEditScoreDialog.value = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialogEditBall = false }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScrollableIncomingBatsmanSelector(
    list: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        list.forEach { batsman ->
            val isActive = selected == batsman
            InputChip(
                selected = isActive,
                onClick = { onSelect(batsman) },
                label = { Text(batsman, fontSize = 11.sp) }
            )
        }
    }
}

@Composable
fun InteractiveBatsmanRow(
    name: String,
    stats: com.example.data.BatsmanLiveStats?,
    isStriker: Boolean,
    onSwap: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = Icons.Default.SportsCricket,
                contentDescription = if (isStriker) "On Strike" else "Non Strike",
                tint = if (isStriker) Color(0xFF10B981) else Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                name.ifEmpty { "Select Batsman" },
                color = if (isStriker) Color(0xFF10B981) else Color.White,
                fontWeight = if (isStriker) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp
            )
            if (isStriker) {
                Text("*", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
            }
        }
        Row(modifier = Modifier.width(180.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${stats?.runs ?: 0}", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
            Text("${stats?.ballsFaced ?: 0}", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
            Text("${stats?.fours ?: 0}", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
            Text("${stats?.sixes ?: 0}", color = Color.White, fontSize = 14.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
            Text(
                if (stats != null) String.format("%.1f", stats.strikeRate) else "0.0",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.width(50.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun BallLabelCircle(label: String) {
    val isOut = label == "W" || label == "W-RO"
    val isBoundary = label == "4" || label == "6"
    val isExtra = label.contains("wd") || label.contains("nb") || label.contains("B") || label.contains("LB")

    val bg = when {
        isOut -> Color(0xFF7F1D1D)
        label == "4" -> Color(0xFF065F46)
        label == "6" -> Color(0xFF1E3A8A)
        isExtra -> Color(0xFFF59E0B).copy(alpha = 0.2f)
        else -> Color(0xFF334155)
    }

    val textCol = when {
        isOut -> Color(0xFFFCA5A5)
        label == "4" -> Color(0xFF34D399)
        label == "6" -> Color(0xFF60A5FA)
        isExtra -> Color(0xFFF59E0B)
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = textCol,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ScoringButton(
    label: String,
    subLabel: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .height(64.dp)
            .testTag("score_pad_$label"),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                label,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                subLabel,
                color = textColor.copy(alpha = 0.6f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
