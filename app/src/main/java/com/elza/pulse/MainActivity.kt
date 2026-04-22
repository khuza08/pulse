package com.elza.pulse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.elza.pulse.ui.screens.Route
import com.elza.pulse.ui.screens.home.HomeScreen
import com.elza.pulse.ui.screens.search.SearchScreen
import com.elza.pulse.ui.screens.searchresult.SearchResultScreen
import com.elza.pulse.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulseTheme {
                PulseApp()
            }
        }
    }
}

@Composable
fun PulseApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Home.route
    ) {
        composable(Route.Home.route) {
            HomeScreen(navController)
        }
        composable(Route.Search.route) {
            SearchScreen(navController)
        }
        composable(Route.SearchResult.route) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchResultScreen(navController, query)
        }
        composable(Route.Album.route) { backStackEntry ->
            val browseId = backStackEntry.arguments?.getString("browseId") ?: ""
            Text("Album: $browseId")
        }
        composable(Route.Artist.route) { backStackEntry ->
            val browseId = backStackEntry.arguments?.getString("browseId") ?: ""
            Text("Artist: $browseId")
        }
    }
}