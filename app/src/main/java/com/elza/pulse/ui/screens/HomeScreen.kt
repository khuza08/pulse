package com.elza.pulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elza.pulse.ui.components.SearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                Text(
                    text = "Listen Now",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { /* Handle search */ }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 100.dp) // Space for player
        ) {
            // Quick Picks Section
            item {
                SectionHeader(title = "Quick Picks")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mockSongs) { song ->
                        QuickPickItem(song)
                    }
                }
            }

            // Trending Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Trending Now")
            }
            
            items(mockSongs.takeLast(4)) { song ->
                TrendingItem(song)
            }

            // Artists Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Featured Artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(mockArtists) { artist ->
                        ArtistItem(artist)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun QuickPickItem(song: MockSong) {
    Column(modifier = Modifier.width(140.dp)) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        ) {
            // Placeholder for Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.tertiary),
            maxLines = 1
        )
    }
}

@Composable
fun TrendingItem(song: MockSong) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = song.artist, style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.tertiary))
        }
    }
}

@Composable
fun ArtistItem(artist: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = artist, style = MaterialTheme.typography.labelLarge)
    }
}

// Mock Data
data class MockSong(val title: String, val artist: String)
val mockSongs = listOf(
    MockSong("Blinding Lights", "The Weeknd"),
    MockSong("Levitating", "Dua Lipa"),
    MockSong("Stay", "The Kid LAROI & Justin Bieber"),
    MockSong("Heat Waves", "Glass Animals"),
    MockSong("Save Your Tears", "The Weeknd"),
    MockSong("Peaches", "Justin Bieber")
)

val mockArtists = listOf("The Weeknd", "Dua Lipa", "Justin Bieber", "Drake", "Taylor Swift")
