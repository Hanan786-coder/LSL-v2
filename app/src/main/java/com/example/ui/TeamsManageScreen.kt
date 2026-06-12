package com.example.ui

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TeamEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsManageScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val teams by viewModel.teams.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cricket Team Rosters", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A)),
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Team", tint = Color(0xFFF96116))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFFF96116),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_team_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Team")
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(18.dp)
        ) {
            Text(
                "MANAGE LOCAL TEAMS & SQUADS",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (teams.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF152035)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFFF96116).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = "Teams",
                                tint = Color(0xFFF96116),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No squads enrolled yet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the '+' action button to enroll your neighborhood squads! Add list of custom player names to unlock individual ball-by-ball striking career tracking.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(teams) { team ->
                        TeamRosterCard(
                            team = team,
                            onDeleteClick = { viewModel.deleteTeam(team) }
                        )
                    }
                }
            }
        }
    }

    // Create Team Dialog Modal
    if (showCreateDialog) {
        var teamName by remember { mutableStateOf("") }
        var rawPlayerNames by remember { mutableStateOf("") }
        var selectedTourneyId by remember { mutableStateOf<Int?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color(0xFF0F172A),
            modifier = Modifier.border(2.dp, Color(0xFFF96116), RoundedCornerShape(24.dp)),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = Color(0xFFF96116),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("CREATE NEW SQUAD", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 0.5.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Input your street squadron identity and registered players:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name / Colony identity") },
                        placeholder = { Text("e.g. Colony Chargers") },
                        modifier = Modifier.fillMaxWidth().testTag("team_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF96116),
                            unfocusedBorderColor = Color(0xFF334155),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedLabelColor = Color(0xFFF96116),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = rawPlayerNames,
                        onValueChange = { rawPlayerNames = it },
                        label = { Text("Players roster (comma-separated list)") },
                        placeholder = { Text("e.g. Ali Khan, Babar Zaman, Shaheen Afridi") },
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF96116),
                            unfocusedBorderColor = Color(0xFF334155),
                            unfocusedLabelColor = Color(0xFF94A3B8),
                            focusedLabelColor = Color(0xFFF96116),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (tournaments.isNotEmpty()) {
                        Text("Link with active tournament (Optional):", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InputChip(
                                selected = selectedTourneyId == null,
                                onClick = { selectedTourneyId = null },
                                label = { Text("None") }
                            )
                            tournaments.forEach { tour ->
                                InputChip(
                                    selected = selectedTourneyId == tour.id,
                                    onClick = { selectedTourneyId = tour.id },
                                    label = { Text(tour.name) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (teamName.isNotBlank() && rawPlayerNames.isNotBlank()) {
                            val playersList = rawPlayerNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            viewModel.createTeam(teamName, playersList, selectedTourneyId)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAVE", color = Color.Black, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun TeamRosterCard(team: TeamEntity, onDeleteClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val players = remember(team.playerNames) {
        team.playerNames.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF152035)),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(team.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${players.size} Players registered", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.8f))
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = Color.White
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(10.dp))

                Text("PLAYER ROSTER:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Wrap in flow layouts or grids
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    players.forEachIndexed { idx, name ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${idx + 1}", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
