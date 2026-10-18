package cat.vira.android.app

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cat.vira.android.features.emotions.EmotionsScreen
import cat.vira.android.features.plan.PlanScreen
import cat.vira.android.features.progress.ProgressScreen
import cat.vira.android.features.today.TodayScreen

/// L'arrel: les quatre pestanyes i la barra. Calca `RootView` de l'iPhone.
///
/// La barra d'aquí és la de sistema, provisional: `ViraTabBar` (barra sempre opaca, icones
/// pròpies, botó central de Concentra't) la fa el fil Sistema de disseny a `core/design`.
@Composable
fun RootScreen() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                ViraTab.entries.forEach { tab ->
                    val title = stringResource(tab.title)
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(tab.icon),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(26.dp),
                            )
                        },
                        label = { Text(title) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ViraTab.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(ViraTab.Today.route) { TodayScreen() }
            composable(ViraTab.Plan.route) { PlanScreen() }
            composable(ViraTab.Emotions.route) { EmotionsScreen() }
            composable(ViraTab.Progress.route) { ProgressScreen() }
        }
    }
}
