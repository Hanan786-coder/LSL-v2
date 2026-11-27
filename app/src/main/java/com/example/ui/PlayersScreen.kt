package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.PlayerEntity
import com.example.data.PlayerRole
import com.example.ui.theme.Lsl

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayersScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val players by viewModel.players.collectAsState()
    val members by viewModel.members.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val context = LocalContext.current

    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PlayerEntity?>(null) }

    data class Membership(val team: String, val role: PlayerRole, val captain: Boolean)

    val membership = remember(members, teams) {
        val teamNames = teams.associate { it.id to it.name }
        members.groupBy { it.playerId }.mapValues { (_, ms) ->
            ms.mapNotNull { m -> teamNames[m.teamId]?.let { Membership(it, PlayerRole.of(m.role), m.isCaptain) } }
        }
    }
    val shown = players.filter { query.isBlank() || it.name.contains(query.trim(), true) }

    LslScreen(
        title = "Players",
        subtitle = if (players.isEmpty()) null else "${players.size} saved",
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                containerColor = Lsl.Volt,
                contentColor = Lsl.VoltInk,
                icon = { Icon(Icons.Default.PersonAdd, null) },
                text = { Text("Add player") },
                modifier = Modifier.testTag("add_player_fab")
            )
        }
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (players.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search players") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search", tint = Lsl.Mist) } },
                    singleLine = true,
                    colors = lslFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
            if (shown.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Person,
                    title = if (players.isEmpty()) "No saved players yet" else "No one called \"$query\"",
                    body = if (players.isEmpty()) "Save regular players once and add them to any team."
                    else "Add them as a new player.",
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(shown, key = { it.id }) { p ->
                        val ms = membership[p.id].orEmpty()
                        Row(
                            Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Lsl.Deck)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Monogram(p.name, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, color = Lsl.Chalk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (ms.isEmpty()) {
                                    Text("Not in a team yet", style = MaterialTheme.typography.labelMedium, color = Lsl.Fade)
                                } else {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ms.forEach { m ->
                                            // Fixed height + centring keeps names, C marks and role tags on one line
                                            Row(Modifier.height(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(m.team, style = MaterialTheme.typography.labelLarge.merge(CenteredLabel), color = Lsl.Mist, maxLines = 1)
                                                if (m.captain) { Spacer(Modifier.width(6.dp)); CaptainMark(size = 18.dp) }
                                                if (m.role != PlayerRole.UNSET) { Spacer(Modifier.width(6.dp)); RoleBadge(m.role) }
                                            }
                                        }
                                    }
                                }
                            }
                            IconButton(onClick = { deleting = p }) {
                                Icon(Icons.Default.DeleteOutline, "Delete ${p.name}", tint = Lsl.Fade)
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        var name by remember { mutableStateOf("") }
        val exists = players.any { it.name.equals(name.trim(), true) }
        AlertDialog(
            onDismissRequest = { adding = false },
            containerColor = Lsl.Deck,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Add player", style = MaterialTheme.typography.titleLarge, color = Lsl.Chalk) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        isError = exists,
                        supportingText = { if (exists) Text("Already saved") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = lslFieldColors(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("new_player_name")
                    )
                    Text("Roles are set per team.", style = MaterialTheme.typography.bodySmall, color = Lsl.Mist)
                }
            },
            confirmButton = {
                val ok = name.isNotBlank() && !exists
                TextButton(onClick = { viewModel.addPlayer(name); adding = false }, enabled = ok) {
                    Text("Save player", color = if (ok) Lsl.Volt else Lsl.Fade, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Cancel", color = Lsl.Mist) } }
        )
    }

    deleting?.let { p ->
        val teamCount = membership[p.id].orEmpty().size
        if (teamCount > 0) {
            AlertDialog(
                onDismissRequest = { deleting = null },
                containerColor = Lsl.Deck,
                shape = RoundedCornerShape(24.dp),
                title = { Text("${p.name} is in $teamCount team${if (teamCount == 1) "" else "s"}", style = MaterialTheme.typography.titleLarge, color = Lsl.Chalk) },
                text = { Text("Remove them from those squads first.", color = Lsl.Mist) },
                confirmButton = { TextButton(onClick = { deleting = null }) { Text("OK", color = Lsl.Volt) } }
            )
        } else {
            ConfirmDialog(
                title = "Delete ${p.name}?",
                body = "Their past stats are kept.",
                confirmText = "Delete player",
                onConfirm = {
                    viewModel.deletePlayer(p) { ok ->
                        if (!ok) Toast.makeText(context, "${p.name} is still in a team", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { deleting = null }
            )
        }
    }
}
