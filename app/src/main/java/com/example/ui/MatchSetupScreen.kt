package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TeamEntity
import com.example.data.TournamentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val teams by viewModel.teams.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()

    var selectedTeamA by remember { mutableStateOf<TeamEntity?>(null) }
    var selectedTeamB by remember { mutableStateOf<TeamEntity?>(null) }
    var selectedTournamentId by remember { mutableStateOf<Int?>(null) }

    var oversChosen by remember { mutableStateOf(5) } // Standard 5 overs default
    var battingFirstId by remember { mutableStateOf<Int?>(null) } // Toss decides who bat first

    var tossWinnerName by remember { mutableStateOf("") }
    var showTossResult by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Match Arena Setup", fontWeight = FontWeight.Black, color = Color.White) },
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
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "CONFIGURE CRICKET CLASH",
                color = Color(0xFFF96116),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            // 1. Selector Tournament (Optional)
            if (tournaments.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Tournament League (Optional)", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InputChip(
                                selected = selectedTournamentId == null,
                                onClick = { selectedTournamentId = null },
                                label = { Text("None (Friendly Match)") }
                            )
                            tournaments.forEach { tour ->
                                InputChip(
                                    selected = selectedTournamentId == tour.id,
                                    onClick = { selectedTournamentId = tour.id },
                                    label = { Text(tour.name) }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Select Teams
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Contenders", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)

                    Text("Team A (Batting / Home):", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        teams.forEach { team ->
                            if (team.id != selectedTeamB?.id) {
                                InputChip(
                                    selected = selectedTeamA?.id == team.id,
                                    onClick = { selectedTeamA = team },
                                    label = { Text(team.name) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Team B (Bowling / Away):", color = Color.White, fontSize = 12.sp)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        teams.forEach { team ->
                            if (team.id != selectedTeamA?.id) {
                                InputChip(
                                    selected = selectedTeamB?.id == team.id,
                                    onClick = { selectedTeamB = team },
                                    label = { Text(team.name) }
                                )
                            }
                        }
                    }
                }
            }

            // 3. Configure Overs
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Configured Match Overs", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 5, 10, 20).forEach { ov ->
                            val isSelected = oversChosen == ov
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
                                    .clickable { oversChosen = ov }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$ov Overs",
                                    color = if (isSelected) Color(0xFF10B981) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Gully Toss Simulation Flipping Coin
            if (selectedTeamA != null && selectedTeamB != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Gully Toss Simulator", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val random = (0..1).random()
                                val winner = if (random == 0) selectedTeamA!! else selectedTeamB!!
                                tossWinnerName = winner.name
                                battingFirstId = winner.id
                                showTossResult = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Flip Coin (Toss)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        if (showTossResult) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "🪙 $tossWinnerName won the toss and elected to bat first!",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Start Match Action Button
            Button(
                onClick = {
                    if (selectedTeamA == null || selectedTeamB == null) {
                        return@Button
                    }
                    if (battingFirstId == null) {
                        battingFirstId = selectedTeamA!!.id // Default if toss not flipped
                    }

                    // Prepare listings
                    val playersA = selectedTeamA!!.playerNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val playersB = selectedTeamB!!.playerNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    viewModel.startMatch(
                        teamAId = selectedTeamA!!.id,
                        teamBId = selectedTeamB!!.id,
                        teamAName = selectedTeamA!!.name,
                        teamBName = selectedTeamB!!.name,
                        overs = oversChosen,
                        battingFirstId = battingFirstId!!,
                        teamAPlayers = playersA,
                        teamBPlayers = playersB,
                        tournamentId = selectedTournamentId
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_match_submission"),
                enabled = selectedTeamA != null && selectedTeamB != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF96116),
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SportsCricket, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Live Gully Cricket Clash", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
