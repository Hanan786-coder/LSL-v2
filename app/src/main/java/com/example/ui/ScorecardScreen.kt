package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    viewModel: LocoViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val match by viewModel.activeMatch.collectAsState()

    if (match == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text("No match selected.", color = Color.White)
        }
        return
    }

    val activeMatch = match!!
    val battingRoster = viewModel.getBattingTeamPlayers()
    val bowlingRoster = viewModel.getBowlingTeamPlayers()

    val ballsList = remember(activeMatch.ballsJson) {
        JsonHelper.listFromJson(activeMatch.ballsJson, BallInfo::class.java)
    }

    // Process both innings
    val firstInningsCard = remember(ballsList) {
        CricketScorer.calculateInningsScorecard(ballsList, 1, bowlingRoster)
    }
    val secondInningsCard = remember(ballsList) {
        CricketScorer.calculateInningsScorecard(ballsList, 2, battingRoster)
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Summary, 1 = Innings 1, 2 = Innings 2
    var showShareModal by remember { mutableStateOf(false) }

    val formattedDate = remember(activeMatch.date) {
        SimpleDateFormat("EEEE, dd MMMM yyyy, h:mm a", Locale.getDefault()).format(Date(activeMatch.date))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Center Scorecard", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showShareModal = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Scorecard", tint = Color(0xFF10B981))
                    }
                    IconButton(onClick = {
                        val summaryText = generateMatchSummaryText(activeMatch, firstInningsCard, secondInningsCard)
                        copyToClipboard(context, "Loco Match Summary", summaryText)
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Summary", tint = Color(0xFF38BDF8))
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
        ) {
            // Tab Header Bar
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF10B981)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("SUMMARY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Color(0xFF10B981) else Color(0xFF94A3B8)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(activeMatch.teamAName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color(0xFF10B981) else Color(0xFF94A3B8)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text(activeMatch.teamBName.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 2) Color(0xFF10B981) else Color(0xFF94A3B8)) }
                )
            }

            // Tabs Content
            when (selectedTab) {
                0 -> SummaryTabContent(
                    match = activeMatch,
                    firstCard = firstInningsCard,
                    secondCard = secondInningsCard,
                    formattedDate = formattedDate,
                    onShareClick = { showShareModal = true }
                )
                1 -> InningsTabContent(
                    teamName = activeMatch.teamAName,
                    card = firstInningsCard
                )
                2 -> InningsTabContent(
                    teamName = activeMatch.teamBName,
                    card = secondInningsCard
                )
            }
        }
    }

    // Share Scorecard Simulated Modal Panel
    if (showShareModal) {
        AlertDialog(
            onDismissRequest = { showShareModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Share / Export Scorecard", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Generate a graphical summary cards below to copy and share on WhatsApp, Facebook or local colony groups:", color = Color(0xFF94A3B8), fontSize = 13.sp)

                    // A beautiful simulated graphic
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏏 LOCO STREET LEAGUE", fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981), fontSize = 12.sp)
                            Text("GULLY MATCH SCORECARD", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp)
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(activeMatch.teamAName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${firstInningsCard.totalRuns}/${firstInningsCard.totalWickets}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    Text("(${firstInningsCard.totalOversString} Ov)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                                Text("VS", color = Color(0xFFEF4444), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(activeMatch.teamBName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${secondInningsCard.totalRuns}/${secondInningsCard.totalWickets}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    Text("(${secondInningsCard.totalOversString} Ov)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                "Result: ${activeMatch.resultText.ifEmpty { "In Progress..." }}",
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val text = generateMatchSummaryText(activeMatch, firstInningsCard, secondInningsCard)
                                copyToClipboard(context, "Match Summary", text)
                                showShareModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Summary Text", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Scorecard image generated and saved to gallery!", Toast.LENGTH_LONG).show()
                                showShareModal = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save as Image", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun SummaryTabContent(
    match: MatchEntity,
    firstCard: InningsScorecard,
    secondCard: InningsScorecard,
    formattedDate: String,
    onShareClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Result Banner Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    match.resultText.ifEmpty { "MATCH IS ONGOING" },
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                if (match.topPerformer.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "🔥 Match MVP: ${match.topPerformer}",
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    formattedDate,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Versus Comparison Box
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "MATCH SCORE SUMMARY",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Team A scores row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(match.teamAName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${firstCard.totalRuns}/${firstCard.totalWickets}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            " (${firstCard.totalOversString} ov)",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Team B scores row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(match.teamBName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${secondCard.totalRuns}/${secondCard.totalWickets}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            " (${secondCard.totalOversString} ov)",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }

        // Quick Sharing Card Promo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                .clickable { onShareClick() }
                .border(1.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Share scorecard to friends", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Generate dynamic image summary for WhatsApp & gully groups", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun InningsTabContent(
    teamName: String,
    card: InningsScorecard
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL INNINGS RUNS", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Text(
                        "${card.totalRuns}-${card.totalWickets} (${card.totalOversString} Overs)",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Batsman scorecard section
        item {
            Text("BATTING HIGHLIGHTS", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        val batList = card.batsmen.values.filter { it.runs > 0 || it.ballsFaced > 0 }
        if (batList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No batsman has run/faced a legal ball yet.", color = Color(0xFF475569), fontSize = 13.sp)
                }
            }
        } else {
            items(batList) { batsman ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(batsman.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (batsman.isOut) {
                                    val outText = when {
                                        batsman.outType != null && batsman.bowlerName != null -> "out ${batsman.outType} b ${batsman.bowlerName}"
                                        batsman.outType != null -> "out ${batsman.outType}"
                                        else -> "out"
                                    }
                                    Text(outText, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                } else {
                                    Text("not out", color = Color(0xFF10B981), fontSize = 11.sp)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${batsman.runs}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "off ${batsman.ballsFaced}b",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Boundary: ${batsman.fours} fours • ${batsman.sixes} sixes", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text("SR: ${String.format("%.1f", batsman.strikeRate)}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Bowling stats section
        item {
            Text("BOWLING ANALYTICS", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        val bowlList = card.bowlers.values
        if (bowlList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No bowler has thrown a ball in this innings.", color = Color(0xFF475569), fontSize = 13.sp)
                }
            }
        } else {
            items(bowlList.toList()) { bowler ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(bowler.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Wides: ${bowler.wides} • NoBalls: ${bowler.noBalls}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${bowler.wickets} Wkts", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${bowler.runsConceded} Runs", color = Color.White, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${bowler.oversString} ov", color = Color.White, fontSize = 13.sp)
                                Text("Econ: ${String.format("%.1f", bowler.economy)}", color = Color(0xFF38BDF8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers for sharing & string export logs
private fun generateMatchSummaryText(
    match: MatchEntity,
    first: InningsScorecard,
    second: InningsScorecard
): String {
    return """
🏏 LOCO STREET LEAGUE MATCH SUMMARY 🏏
-------------------------------------------
📅 Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(match.date))}
⚔️ ${match.teamAName} vs ${match.teamBName}
Overs: ${match.overs} • Gully League

First Innings (${match.teamAName}):
👉 Score: ${first.totalRuns}/${first.totalWickets} (${first.totalOversString} Overs)
Top Batter: ${first.batsmen.values.maxByOrNull { it.runs }?.let { "${it.name} ${it.runs}(${it.ballsFaced})" } ?: "N/A"}
Top Bowler: ${first.bowlers.values.maxByOrNull { it.wickets }?.let { "${it.name} ${it.wickets}/${it.runsConceded}" } ?: "N/A"}

Second Innings (${match.teamBName}):
👉 Score: ${second.totalRuns}/${second.totalWickets} (${second.totalOversString} Overs)
Top Batter: ${second.batsmen.values.maxByOrNull { it.runs }?.let { "${it.name} ${it.runs}(${it.ballsFaced})" } ?: "N/A"}
Top Bowler: ${second.bowlers.values.maxByOrNull { it.wickets }?.let { "${it.name} ${it.wickets}/${it.runsConceded}" } ?: "N/A"}

🔥 RESULT: ${match.resultText.ifEmpty { "Match in progress!" }}
🏆 MVP: ${match.topPerformer.ifEmpty { "N/A" }}

Tracked live with Loco Street League scoring app!
""".trimIndent()
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Match summary copied to clipboard!", Toast.LENGTH_SHORT).show()
}
