package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.MatchEntity
import com.example.ui.theme.Lsl
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: LocoViewModel,
    onBack: () -> Unit
) {
    val matches by viewModel.matches.collectAsState()
    val completed = matches.filter { it.status == "COMPLETED" }
    var query by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<MatchEntity?>(null) }

    val filtered = remember(completed, query) {
        completed.filter {
            query.isBlank() ||
                it.teamAName.contains(query, true) ||
                it.teamBName.contains(query, true) ||
                it.resultText.contains(query, true) ||
                it.topPerformer.contains(query, true)
        }
    }
    // Group by month so long histories stay scannable
    val grouped = remember(filtered) {
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        filtered.groupBy { fmt.format(Date(it.date)) }
    }

    LslScreen(
        title = "Match history",
        subtitle = if (completed.isEmpty()) null else "${completed.size} finished, long-press to delete",
        onBack = onBack
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner)) {
            if (completed.isNotEmpty()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search teams, results or players") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search", tint = Lsl.Moss) }
                    },
                    singleLine = true,
                    colors = lslFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("history_search_query")
                )
            }
            if (filtered.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = if (completed.isEmpty()) "No finished matches yet" else "Nothing matches \"$query\"",
                    body = if (completed.isEmpty()) "Finish a match and its full scorecard is saved here." else "Try a team or player name.",
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().navigationBarsPadding()
                ) {
                    grouped.forEach { (month, list) ->
                        item(key = "h-$month") { SectionTitle(month, Modifier.padding(top = 4.dp)) }
                        items(list, key = { it.id }) { m ->
                            ResultCard(
                                match = m,
                                onClick = { viewModel.openMatch(m) },
                                onLongClick = { deleting = m }
                            )
                        }
                    }
                }
            }
        }
    }

    deleting?.let { m ->
        ConfirmDialog(
            title = "Delete this match?",
            body = "${m.teamAName} v ${m.teamBName} and its scorecard are removed. Player career stats already counted stay as they are.",
            confirmText = "Delete match",
            onConfirm = { viewModel.deleteMatch(m.id) },
            onDismiss = { deleting = null }
        )
    }
}
