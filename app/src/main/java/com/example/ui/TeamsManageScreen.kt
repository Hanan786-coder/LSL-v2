package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.TeamEntity
import com.example.data.TournamentEntity
import com.example.ui.theme.Lsl

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeamsManageScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val teams by viewModel.teams.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()

    var editing by remember { mutableStateOf<TeamEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<TeamEntity?>(null) }

    LslScreen(
        title = "Teams",
        subtitle = if (teams.isEmpty()) null else "${teams.size} squad${if (teams.size == 1) "" else "s"}",
        onBack = onBack,
        floatingActionButton = {
            if (teams.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { creating = true },
                    containerColor = Lsl.Mint,
                    contentColor = Lsl.MintDeep,
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
                body = "Add your squads with player names. Every ball they face or bowl is tracked to their name.",
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
        TeamEditorSheet(
            existing = editing,
            tournaments = tournaments,
            onDismiss = { creating = false; editing = null },
            onSave = { name, players, tid ->
                val e = editing
                if (e == null) viewModel.createTeam(name, players, tid) else viewModel.updateTeam(e, name, players, tid)
                creating = false; editing = null
            }
        )
    }

    deleting?.let { t ->
        ConfirmDialog(
            title = "Delete ${t.name}?",
            body = "The squad is removed. Finished matches and player career stats stay in history.",
            confirmText = "Delete team",
            onConfirm = { viewModel.deleteTeam(t) },
            onDismiss = { deleting = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeamCard(
    team: TeamEntity,
    tournament: TournamentEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(team.id) { mutableStateOf(false) }
    val players = remember(team.playerNames) { team.playerNames.toPlayerList() }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "chev")

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Lsl.Turf)
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
                        append("${players.size} player${if (players.size == 1) "" else "s"}")
                        if (tournament != null) append(", ${tournament.name}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Lsl.Moss,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.ExpandMore, if (expanded) "Hide players" else "Show players", tint = Lsl.Moss, modifier = Modifier.graphicsLayer { rotationZ = chevron })
        }
        if (expanded) {
            FlowRow(
                Modifier.padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                players.forEach { p ->
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(Lsl.TurfRaised).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p, style = MaterialTheme.typography.labelLarge, color = Lsl.Chalk)
                    }
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
    tournaments: List<TournamentEntity>,
    onDismiss: () -> Unit,
    onSave: (String, List<String>, Int?) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    val players = remember { mutableStateListOf<String>().apply { addAll(existing?.playerNames?.toPlayerList().orEmpty()) } }
    var entry by remember { mutableStateOf("") }
    var tid by remember { mutableStateOf(existing?.tournamentId) }

    fun commitEntry() {
        // Accept a pasted list too: "Ali, Babar, Shaheen"
        entry.toPlayerList().forEach { p -> if (players.none { it.equals(p, true) }) players.add(p) }
        entry = ""
    }

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

            SectionTitle("Players (${players.size})")
            OutlinedTextField(
                value = entry,
                onValueChange = {
                    if (it.endsWith(",") || it.endsWith("\n")) { entry = it; commitEntry() } else entry = it
                },
                label = { Text("Add a player") },
                placeholder = { Text("Type a name, press enter") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitEntry() }),
                trailingIcon = {
                    IconButton(onClick = { commitEntry() }, enabled = entry.isNotBlank()) {
                        Icon(Icons.Default.PersonAdd, "Add player", tint = if (entry.isNotBlank()) Lsl.Mint else Lsl.Fade)
                    }
                },
                colors = lslFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("player_input")
            )

            if (players.isEmpty()) {
                Text("Batting order follows the order you add players. You need at least 2.", style = MaterialTheme.typography.bodySmall, color = Lsl.Moss)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    players.forEachIndexed { i, p ->
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Lsl.TurfRaised)
                                .padding(start = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = Lsl.Mint)
                            Spacer(Modifier.width(6.dp))
                            Text(p, style = MaterialTheme.typography.labelLarge, color = Lsl.Chalk)
                            IconButton(onClick = { players.removeAt(i) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.Close, "Remove $p", tint = Lsl.Moss, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (tournaments.isNotEmpty()) {
                SectionTitle("Tournament")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("None", tid == null, { tid = null })
                    tournaments.forEach { t -> ChoiceChip(t.name, tid == t.id, { tid = t.id }, accent = Lsl.Sun) }
                }
            }

            val canSave = name.isNotBlank() && (players.size + entry.toPlayerList().size) >= 2
            PrimaryButton(
                text = if (existing == null) "Save team" else "Save changes",
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().testTag("save_team"),
                onClick = {
                    commitEntry()
                    onSave(name, players.toList(), tid)
                }
            )
        }
    }
}
