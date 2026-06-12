package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.PlayerStatsEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val playerStats by viewModel.playerStats.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSortMetric by remember { mutableStateOf("RUNS") } // "RUNS", "WICKETS", "SIXES", "FOURS"

    val filteredList = remember(playerStats, searchQuery, selectedSortMetric) {
        playerStats.filter {
            it.playerName.contains(searchQuery, ignoreCase = true) ||
            it.teamName.contains(searchQuery, ignoreCase = true)
        }.sortedWith { a, b ->
            when (selectedSortMetric) {
                "WICKETS" -> b.wicketsTaken.compareTo(a.wicketsTaken)
                "SIXES" -> b.sixes.compareTo(a.sixes)
                "FOURS" -> b.fours.compareTo(a.fours)
                else -> b.runsScored.compareTo(a.runsScored) // default runs
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dugout Career Stats", fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.5.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "INDIVIDUAL PLAYER LEADERBOARD (Across All Matches)",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )

            // Search Bar Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search player or squad") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFF96116)) },
                modifier = Modifier.fillMaxWidth().testTag("stats_search_query"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFF96116),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF94A3B8),
                    focusedLabelColor = Color(0xFFF96116),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Sort Selector Categories
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    "RUNS" to "runs Scored",
                    "WICKETS" to "wickets Taken",
                    "SIXES" to "sixes (6s)",
                    "FOURS" to "fours (4s)"
                ).forEach { (metric, label) ->
                    val isSelected = selectedSortMetric == metric
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSortMetric = metric },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = Color.LightGray,
                            selectedLabelColor = Color.Black,
                            selectedContainerColor = Color(0xFFF96116)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color(0xFF334155),
                            selectedBorderColor = Color.White,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Player Items list
            if (filteredList.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF152035)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFF38BDF8).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Leaderboard",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            "Leaderboard is clean!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No career data recorded yet. Score your first match to automatically rank the local legends here.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredList) { statsObj ->
                        PlayerLeaderboardCard(player = statsObj, rankMetric = selectedSortMetric)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerLeaderboardCard(player: PlayerStatsEntity, rankMetric: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF152035)),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF96116).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFF96116))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(player.playerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(player.teamName, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Played: ${player.matchesPlayed} Matches", color = Color(0xFF64748B), fontSize = 11.sp)
                }
            }

            // Metric Value display box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFFF96116).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val labelText = when (rankMetric) {
                        "WICKETS" -> "WKTS"
                        "SIXES" -> "6s"
                        "FOURS" -> "4s"
                        else -> "RUNS"
                    }
                    val valueText = when (rankMetric) {
                        "WICKETS" -> "${player.wicketsTaken}"
                        "SIXES" -> "${player.sixes}"
                        "FOURS" -> "${player.fours}"
                        else -> "${player.runsScored}"
                    }
                    
                    Text(valueText, color = Color(0xFF10B981), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text(labelText, color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
