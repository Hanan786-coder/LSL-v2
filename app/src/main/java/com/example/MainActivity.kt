package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.LocoDatabase
import com.example.data.MatchRepository
import com.example.ui.*
import com.example.ui.theme.Lsl
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = LocoDatabase.getDatabase(this)
        val repository = MatchRepository(database.locoDao())
        val factory = LocoViewModel.provideFactory(repository)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme {
                val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<LocoViewModel>(factory = factory)
                val currentScreen by viewModel.currentScreen.collectAsState()

                // System back walks the in-app history; on home it leaves the app as usual.
                BackHandler(enabled = currentScreen != "dashboard" && currentScreen != "splash") {
                    viewModel.goBack()
                }

                Box(Modifier.fillMaxSize().background(Lsl.Night)) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (initialState == "splash") {
                                fadeIn(tween(420)) togetherWith fadeOut(tween(300))
                            } else {
                                val dir = if (viewModel.lastNavWasBack) -1 else 1
                                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it / 5 * dir } + fadeIn(tween(240)))
                                    .togetherWith(slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 8 * dir } + fadeOut(tween(160)))
                            }
                        },
                        label = "screen"
                    ) { screen ->
                        when (screen) {
                            "splash" -> SplashScreen(onSplashDone = { viewModel.navigateTo("dashboard") })
                            "match_setup" -> MatchSetupScreen(viewModel = viewModel, onBack = { viewModel.goBack() })
                            "scoring" -> ScoringScreen(
                                viewModel = viewModel,
                                onBackToDashboard = { viewModel.navigateTo("dashboard") },
                                onViewScorecardClick = { viewModel.navigateTo("scorecard") }
                            )
                            "scorecard" -> ScorecardScreen(viewModel = viewModel, onBackClick = { viewModel.goBack() })
                            "teams" -> TeamsManageScreen(viewModel = viewModel, onBack = { viewModel.goBack() })
                            "players" -> PlayersScreen(viewModel = viewModel, onBack = { viewModel.goBack() })
                            "tournaments" -> TournamentScreen(viewModel = viewModel, onBack = { viewModel.goBack() })
                            "leaderboard" -> LeaderboardScreen(viewModel = viewModel, onBack = { viewModel.goBack() })
                            "history" -> HistoryScreen(viewModel = viewModel, onBack = { viewModel.goBack() })
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
    }
}
