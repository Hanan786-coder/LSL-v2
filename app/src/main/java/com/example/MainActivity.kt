package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.LocoDatabase
import com.example.data.MatchRepository
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Room Database & Repository
        val database = LocoDatabase.getDatabase(this)
        val repository = MatchRepository(database.locoDao())
        
        // Define Custom ViewModel Factory for injection
        val factory = LocoViewModel.provideFactory(repository)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<LocoViewModel>(factory = factory)
                val currentScreen by viewModel.currentScreen.collectAsState()

                when (currentScreen) {
                    "splash" -> SplashScreen(
                        onSplashDone = { viewModel.navigateTo("dashboard") }
                    )
                    "dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        onStartMatchClick = { viewModel.navigateTo("match_setup") },
                        onManageTeamsClick = { viewModel.navigateTo("teams") },
                        onTournamentsClick = { viewModel.navigateTo("tournaments") },
                        onHistoryClick = { viewModel.navigateTo("history") },
                        onLeaderboardClick = { viewModel.navigateTo("leaderboard") }
                    )
                    "match_setup" -> MatchSetupScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo("dashboard") }
                    )
                    "scoring" -> ScoringScreen(
                        viewModel = viewModel,
                        onBackToDashboard = { viewModel.navigateTo("dashboard") },
                        onViewScorecardClick = { viewModel.navigateTo("scorecard") }
                    )
                    "scorecard" -> ScorecardScreen(
                        viewModel = viewModel,
                        onBackClick = {
                            val active = viewModel.activeMatch.value
                            if (active != null && active.status == "ONGOING") {
                                viewModel.navigateTo("scoring")
                            } else {
                                viewModel.navigateTo("dashboard")
                            }
                        }
                    )
                    "teams" -> TeamsManageScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo("dashboard") }
                    )
                    "tournaments" -> TournamentScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo("dashboard") }
                    )
                    "leaderboard" -> LeaderboardScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo("dashboard") }
                    )
                    "history" -> HistoryScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo("dashboard") }
                    )
                    else -> DashboardScreen(
                        viewModel = viewModel,
                        onStartMatchClick = { viewModel.navigateTo("match_setup") },
                        onManageTeamsClick = { viewModel.navigateTo("teams") },
                        onTournamentsClick = { viewModel.navigateTo("tournaments") },
                        onHistoryClick = { viewModel.navigateTo("history") },
                        onLeaderboardClick = { viewModel.navigateTo("leaderboard") }
                    )
                }
            }
        }
    }
}
