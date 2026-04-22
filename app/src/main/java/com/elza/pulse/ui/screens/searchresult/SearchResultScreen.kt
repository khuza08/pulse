package com.elza.pulse.ui.screens.searchresult

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.elza.pulse.ui.screens.search.SearchState
import com.elza.pulse.ui.screens.search.SearchViewModel
import com.elza.pulse.innertube.models.SongItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultScreen(
    navController: NavController,
    query: String,
    viewModel: SearchViewModel = viewModel()
) {
    println("SearchResultScreen: Composing for query: '$query'")
    LaunchedEffect(query) {
        println("SearchResultScreen: LaunchedEffect triggered for query: '$query'")
        viewModel.search(query)
    }

    val state by viewModel.searchState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = query) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val currentState = state) {
                is SearchState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SearchState.Success -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(currentState.items) { item ->
                            SearchResultItem(
                                item = item,
                                onClick = { /* Navigate to player */ }
                            )
                        }
                    }
                }
                is SearchState.Error -> {
                    Text(
                        text = "Error: ${currentState.message}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SearchResultItem(item: SongItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Text(
                text = item.artist,
                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.tertiary),
                maxLines = 1
            )
        }
    }
}
