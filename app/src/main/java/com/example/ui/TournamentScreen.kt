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
import com.example.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val tournaments by viewModel.tournaments.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val matches by viewModel.matches.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTournament by remember { mutableStateOf<TournamentEntity?>(null) }

    // If a tournament is selected, show detail tabs (Points Table & Schedule fixtures)
    if (selectedTournament != null) {
        TournamentDetailTab(
            tournament = selectedTournament!!,
            viewModel = viewModel,
            allTeams = teams,
            allMatches = matches,
            onBackToTournaments = { selectedTournament = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Tournaments", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A)),
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Tournament", tint = Color(0xFFF96116))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFFF96116),
                contentColor = Color.White,
                modifier = Modifier.testTag("add_tournament_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New Tournament")
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
                "ACTIVE TOURNAMENT LEAGUES",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (tournaments.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF152035)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp).fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFFFACC15).copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Tournament",
                                tint = Color(0xFFFACC15),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "No active championships",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the '+' icon to launch a local championship! Add multiple team rosters, compile automatic points standings tables of wins/losses, and maintain custom fixtures schedules.",
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
                    items(tournaments) { tour ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTournament = tour },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF152035)),
                            border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(tour.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        val tourTeams = teams.filter { it.tournamentId == tour.id }
                                        Text("${tourTeams.size} registered teams", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { viewModel.deleteTournament(tour) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444).copy(alpha = 0.8f))
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var tourName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Tournament league", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFF1E293B),
            text = {
                OutlinedTextField(
                    value = tourName,
                    onValueChange = { tourName = it },
                    label = { Text("League Name (e.g. Loco Basti Cup)") },
                    modifier = Modifier.fillMaxWidth().testTag("tournament_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedLabelColor = Color(0xFF10B981),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tourName.isNotBlank()) {
                            viewModel.createTournament(tourName)
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Save League", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
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
    val points: Int,
    val runsScored: Int,
    val oversFaced: Double,
    val runsConceded: Int,
    val oversBowled: Double
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
fun TournamentDetailTab(
    tournament: TournamentEntity,
    viewModel: LocoViewModel,
    allTeams: List<TeamEntity>,
    allMatches: List<MatchEntity>,
    onBackToTournaments: () -> Unit
) {
    val tournamentTeams = allTeams.filter { it.tournamentId == tournament.id }
    val tournamentMatches = allMatches.filter { it.tournamentId == tournament.id }

    var tabSelected by remember { mutableStateOf(0) } // 0 = Points Table, 1 = Fixture Schedule matches

    // Dynamic calculated points table
    val pointsTable = remember(tournamentMatches) {
        tournamentTeams.map { team ->
            var played = 0
            var won = 0
            var lost = 0
            
            var runsScored = 0
            var legalBallsFaced = 0
            var runsConceded = 0
            var legalBallsBowled = 0

            for (match in tournamentMatches.filter { it.status == "COMPLETED" }) {
                val isTeamA = match.teamAId == team.id
                val isTeamB = match.teamBId == team.id

                if (isTeamA || isTeamB) {
                    played++
                    if (match.winnerTeamId == team.id) {
                        won++
                    } else if (match.winnerTeamId != null) {
                        lost++
                    }

                    // Calculate balls stats
                    val balls = JsonHelper.listFromJson(match.ballsJson, BallInfo::class.java)
                    
                    // Team played innings
                    val (isTeamFirstBat, teamInningsId) = if (match.battingFirstTeamId == team.id) Pair(true, 1) else Pair(false, 2)
                    val opponentInningsId = if (teamInningsId == 1) 2 else 1

                    val scoutInnings = CricketScorer.calculateInningsScorecard(balls, teamInningsId, emptyList())
                    val conInnings = CricketScorer.calculateInningsScorecard(balls, opponentInningsId, emptyList())

                    runsScored += scoutInnings.totalRuns
                    legalBallsFaced += scoutInnings.legalBallsBowled

                    runsConceded += conInnings.totalRuns
                    legalBallsBowled += conInnings.legalBallsBowled
                }
            }

            PointsTableRow(
                teamId = team.id,
                name = team.name,
                played = played,
                won = won,
                lost = lost,
                points = won * 2,
                runsScored = runsScored,
                oversFaced = legalBallsFaced / 6.0,
                runsConceded = runsConceded,
                oversBowled = legalBallsBowled / 6.0
            )
        }.sortedWith(compareByDescending<PointsTableRow> { it.points }.thenByDescending { it.netRunRate })
    }

    // Round robin schedule generation combinations
    val scheduleFixturesList = remember(tournamentTeams) {
        val fixtures = mutableListOf<Pair<TeamEntity, TeamEntity>>()
        if (tournamentTeams.size >= 2) {
            for (i in 0 until tournamentTeams.size) {
                for (j in i + 1 until tournamentTeams.size) {
                    fixtures.add(Pair(tournamentTeams[i], tournamentTeams[j]))
                }
            }
        }
        fixtures
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tournament.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackToTournaments) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            // Internal tab bar selection
            TabRow(
                selectedTabIndex = tabSelected,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF10B981)
            ) {
                Tab(
                    selected = tabSelected == 0,
                    onClick = { tabSelected = 0 },
                    text = { Text("POINTS TABLE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tabSelected == 1,
                    onClick = { tabSelected = 1 },
                    text = { Text("SCHEDULE MATCHES (${scheduleFixturesList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            when (tabSelected) {
                0 -> PointsTableTab(pointsTable)
                1 -> ScheduleFixturesTab(
                    fixtures = scheduleFixturesList,
                    playedMatches = tournamentMatches,
                    onStartFixture = { teamA, teamB ->
                        val playersA = teamA.playerNames.split(",").map { it.trim() }
                        val playersB = teamB.playerNames.split(",").map { it.trim() }
                        viewModel.startMatch(
                            teamAId = teamA.id,
                            teamBId = teamB.id,
                            teamAName = teamA.name,
                            teamBName = teamB.name,
                            overs = 5, // Default 5 overs for tournament
                            battingFirstId = teamA.id,
                            teamAPlayers = playersA,
                            teamBPlayers = playersB,
                            tournamentId = tournament.id
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun PointsTableTab(table: List<PointsTableRow>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B), RoundedCornerShape(4.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TEAM NAME", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f))
            Row(modifier = Modifier.weight(1.8f), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("P", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("W", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("L", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("PTS", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(34.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("NRR", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }

        if (table.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No teams enrolled in this league yet.", color = Color(0xFF475569), fontSize = 14.sp)
            }
        } else {
            table.forEachIndexed { rank, row ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.5f)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (rank == 0) Color(0xFFF59E0B) else Color(0xFF475569)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${rank + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(row.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        }

                        Row(modifier = Modifier.weight(1.8f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${row.played}", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("${row.won}", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("${row.lost}", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text("${row.points}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.width(34.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text(
                                if (row.netRunRate >= 0) "+${String.format("%.3f", row.netRunRate)}" else String.format("%.3f", row.netRunRate),
                                color = if (row.netRunRate >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.width(48.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleFixturesTab(
    fixtures: List<Pair<TeamEntity, TeamEntity>>,
    playedMatches: List<MatchEntity>,
    onStartFixture: (TeamEntity, TeamEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ROUND-ROBIN SCHEDULER", color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold, fontSize = 11.sp)

        if (fixtures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Require at least 2 teams enrolled in this league to schedule round-robin matchups.", color = Color(0xFF475569), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 14.sp)
            }
        } else {
            fixtures.forEach { (teamA, teamB) ->
                val completedMatch = playedMatches.find {
                    it.status == "COMPLETED" &&
                    ((it.teamAId == teamA.id && it.teamBId == teamB.id) ||
                     (it.teamAId == teamB.id && it.teamBId == teamA.id))
                }
                
                val ongoingMatch = playedMatches.find {
                    it.status == "ONGOING" &&
                    ((it.teamAId == teamA.id && it.teamBId == teamB.id) ||
                     (it.teamAId == teamB.id && it.teamBId == teamA.id))
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(teamA.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("vs", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(teamB.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            
                            Box(modifier = Modifier.padding(start = 10.dp)) {
                                if (completedMatch != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("FINISHED", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (ongoingMatch != null) {
                                    Button(
                                        onClick = { onStartFixture(teamA, teamB) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("LIVE SCORING", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { onStartFixture(teamA, teamB) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("START GAME", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (completedMatch != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(completedMatch.resultText, color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
