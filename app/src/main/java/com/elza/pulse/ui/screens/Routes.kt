package com.elza.pulse.ui.screens

import android.net.Uri

sealed class Route(val route: String) {
    object Home : Route("home")
    object Search : Route("search")
    object SearchResult : Route("searchResult/{query}") {
        fun createRoute(query: String) = "searchResult/${Uri.encode(query)}"
    }
    object Album : Route("album/{browseId}") {
        fun createRoute(browseId: String) = "album/${Uri.encode(browseId)}"
    }
    object Artist : Route("artist/{browseId}") {
        fun createRoute(browseId: String) = "artist/${Uri.encode(browseId)}"
    }
    object Settings : Route("settings")
}
