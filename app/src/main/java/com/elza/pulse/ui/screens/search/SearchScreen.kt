package com.elza.pulse.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.elza.pulse.ui.components.SearchBar
import com.elza.pulse.ui.screens.Route

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text(
                        text = "Search",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { 
                        println("SearchScreen: onSearch triggered with query: '$it'")
                        if (it.isNotEmpty()) {
                            val route = Route.SearchResult.createRoute(it)
                            println("SearchScreen: Navigating to $route")
                            navController.navigate(route)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Text(
                    text = "Recent Searches",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            items(mockHistory) { historyItem ->
                HistoryItem(
                    text = historyItem,
                    onClick = {
                        searchQuery = historyItem
                        navController.navigate(Route.SearchResult.createRoute(historyItem))
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

val mockHistory = listOf("Blinding Lights", "Dua Lipa", "Lo-fi Hip Hop", "Classical Study", "Pop Hits 2024")
