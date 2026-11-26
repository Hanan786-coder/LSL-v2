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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerEntity
import com.example.data.PlayerRole
import com.example.data.TeamEntity
import com.example.data.TournamentEntity
import com.example.ui.LocoViewModel.SquadSpot
import com.example.ui.theme.Lsl
import com.example.ui.theme.Scoreboard

@Composable
fun TeamsManageScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val teams by viewModel.teams.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()
    val members by viewModel.members.collectAsState()
    val players by viewModel.players.collectAsState()

    var editing by remember { mutableStateOf<TeamEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TeamEntity?>(null) }

    LslScreen(
        title = "Teams",
        subtitle = if (teams.isEmpty()) null else "${teams.size} squad${if (teams.size == 1) "" else "s"}, ${players.size} saved players",
        onBack = onBack,
        floatingActionButton = {
            if (teams.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    containerColor = Lsl.Flame,
                    contentColor = Lsl.FlameInk,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("New team") },
                    modifier = Modifier.testTag("add_team_fab")
                )
            }
        }
    ) { inner ->
        if (teams.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Groups,
                title = "No teams yet",
                body = "Add a squad with players, roles and a captain.",
                actionText = "Add a team",
                onAction = { creating = true },
                modifier = Modifier.padding(inner).padding(top = 48.dp)
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(teams, key = { it.id }) { team ->
                    TeamCard(
                        team = team,
                        squad = viewModel.squadOf(team.id, members, players),
                        tournament = tournaments.find { it.id == team.tournamentId },
                        onEdit = { editing = team },
                        onDelete = { deleting = team },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (creating || editing != null) {
        val e = editing
        TeamEditorSheet(
            existing = e,
            initialSquad = if (e == null) emptyList() else viewModel.squadOf(e.id, members, players),
            savedPlayers = players,
            tournaments = tournaments,
            onDismiss = { creating = false; editing = null },
            onSave = { name, squad, tid ->
                viewModel.saveTeam(e, name, squad, tid)
                creating = false; editing = null
            }
        )
    }

    deleting?.let { t ->
        ConfirmDialog(
            title = "Delete ${t.name}?",
            body = "Its players, past matches and stats are kept.",
            confirmText = "Delete team",
            onConfirm = { viewModel.deleteTeam(t) },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun TeamCard(
    team: TeamEntity,
    squad: List<SquadSpot>,
    tournament: TournamentEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(team.id) { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "chev")
    val captain = squad.firstOrNull { it.captain }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Lsl.Deck)
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 0.99f) { expanded = !expanded }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Monogram(team.name, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(team.name, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    buildString {
                        append("${squad.size} player${if (squad.size == 1) "" else "s"}")
                        if (captain != null) append(", captain ${captain.name}")
                        if (tournament != null) append(", ${tournament.name}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Lsl.Mist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ExpandMore, if (expanded) "Hide players" else "Show players", tint = Lsl.Mist, modifier = Modifier.graphicsLayer { rotationZ = chevron })
        }
        if (expanded) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                squad.forEachIndexed { i, p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Lsl.Fade, modifier = Modifier.width(24.dp))
                        Text(p.name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (p.captain) { CaptainMark(); Spacer(Modifier.width(6.dp)) }
                        RoleBadge(p.role, long = true)
                    }
                }
                if (squad.any { it.role == PlayerRole.UNSET }) {
                    Text(
                        "Some roles aren't set yet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Lsl.Fade,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuietButton("Edit squad", onEdit, Modifier.weight(1f), icon = Icons.Default.Edit)
                QuietButton("Delete", onDelete, icon = Icons.Default.DeleteOutline, tint = Lsl.Tape)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TeamEditorSheet(
    existing: TeamEntity?,
    initialSquad: List<SquadSpot>,
    savedPlayers: List<PlayerEntity>,
    tournaments: List<TournamentEntity>,
    onDismiss: () -> Unit,
    onSave: (String, List<SquadSpot>, Int?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val squad = remember { mutableStateListOf<SquadSpot>().apply { addAll(initialSquad) } }
    var entry by remember { mutableStateOf("") }
    var tid by remember { mutableStateOf(existing?.tournamentId) }

    fun add(n: String) {
        val clean = n.trim()
        if (clean.isEmpty() || squad.any { it.name.equals(clean, true) }) return
        // Reuse the saved spelling when the player already exists
        val saved = savedPlayers.firstOrNull { it.name.equals(clean, true) }?.name ?: clean
        squad.add(SquadSpot(saved, PlayerRole.UNSET, false))
    }

    fun commitEntry() {
        entry.toPlayerList().forEach(::add)
        entry = ""
    }

    val suggestions = remember(entry, savedPlayers, squad.toList()) {
        savedPlayers.filter { p ->
            squad.none { it.name.equals(p.name, true) } &&
                (entry.isBlank() || p.name.contains(entry.trim(), ignoreCase = true))
        }.take(12)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Lsl.Deck,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (existing == null) "New team" else "Edit ${existing.name}", style = MaterialTheme.typography.headlineSmall, color = Lsl.Chalk)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Team name") },
                placeholder = { Text("Colony Chargers") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors = lslFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("team_name_input")
            )

            SectionTitle("Add players")
            OutlinedTextField(
                value = entry,
                onValueChange = {
                    if (it.endsWith(",") || it.endsWith("\n")) { entry = it; commitEntry() } else entry = it
                },
                label = { Text("Search saved players or type a new name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitEntry() }),
                trailingIcon = {
                    IconButton(onClick = { commitEntry() }, enabled = entry.isNotBlank()) {
                        Icon(Icons.Default.PersonAdd, "Add player", tint = if (entry.isNotBlank()) Lsl.Flame else Lsl.Fade)
                    }
                },
                colors = lslFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("player_input")
            )
            AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestions.forEach { p ->
                        ChoiceChip(
                            text = p.name,
                            selected = false,
                            onClick = { add(p.name); entry = "" },
                            leading = { Icon(Icons.Default.Add, null, tint = Lsl.Flame, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            SectionTitle(
                "Squad (${squad.size}), in batting order",
                trailing = { if (squad.isNotEmpty() && squad.none { it.captain }) Text("Tap the C to pick a captain", style = MaterialTheme.typography.labelSmall, color = Lsl.Fade) }
            )
            if (squad.isEmpty()) {
                Text("Add at least 2 players.", style = MaterialTheme.typography.bodySmall, color = Lsl.Mist)
            }
            squad.forEachIndexed { i, spot ->
                key(spot.name) {
                    SquadRow(
                        index = i,
                        spot = spot,
                        canMoveUp = i > 0,
                        onRole = { r -> squad[i] = spot.copy(role = r) },
                        onCaptain = {
                            val makeCaptain = !spot.captain
                            for (j in squad.indices) squad[j] = squad[j].copy(captain = makeCaptain && j == i)
                        },
                        onMoveUp = { val s = squad.removeAt(i); squad.add(i - 1, s) },
                        onRemove = { squad.removeAt(i) }
                    )
                }
            }

            if (tournaments.isNotEmpty()) {
                SectionTitle("Tournament")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("None", tid == null, { tid = null })
                    tournaments.forEach { t -> ChoiceChip(t.name, tid == t.id, { tid = t.id }, accent = Lsl.Gold) }
                }
            }

            val canSave = name.isNotBlank() && (squad.size + entry.toPlayerList().size) >= 2
            PrimaryButton(
                text = if (existing == null) "Save team" else "Save changes",
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().testTag("save_team"),
                onClick = {
                    commitEntry()
                    onSave(name, squad.toList(), tid)
                }
            )
        }
    }
}

@Composable
private fun SquadRow(
    index: Int,
    spot: SquadSpot,
    canMoveUp: Boolean,
    onRole: (PlayerRole) -> Unit,
    onCaptain: () -> Unit,
    onMoveUp: () -> Unit,
    onRemove: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Lsl.DeckRaised)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${index + 1}", fontFamily = Scoreboard, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Lsl.Fade, modifier = Modifier.width(22.dp))
            Text(spot.name, style = MaterialTheme.typography.titleSmall, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            // Captain toggle
            val capBg by animateColorAsState(if (spot.captain) Lsl.Gold else androidx.compose.ui.graphics.Color.Transparent, tween(160), label = "cap")
            Box(
                Modifier
                    .size(40.dp)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(capBg)
                    .border(1.5.dp, if (spot.captain) Lsl.Gold else Lsl.Line, CircleShape)
                    .pressable(onClick = onCaptain),
                contentAlignment = Alignment.Center
            ) {
                Text("C", fontFamily = Scoreboard, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = if (spot.captain) Lsl.Night else Lsl.Mist)
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.ArrowUpward, "Bat ${spot.name} earlier", tint = if (canMoveUp) Lsl.Mist else Lsl.Line, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Close, "Remove ${spot.name}", tint = Lsl.Mist, modifier = Modifier.size(18.dp))
            }
        }
        Row(Modifier.padding(start = 22.dp, end = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PlayerRole.choices.forEach { r ->
                val selected = spot.role == r
                val c = roleColor(r)
                val bg by animateColorAsState(if (selected) c else Lsl.Deck, tween(150), label = "role-bg")
                Box(
                    Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .pressable(role = androidx.compose.ui.semantics.Role.RadioButton) { onRole(if (selected) PlayerRole.UNSET else r) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (r == PlayerRole.ALL_ROUNDER) "All-round" else r.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selected) Lsl.Night else Lsl.Mist,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
