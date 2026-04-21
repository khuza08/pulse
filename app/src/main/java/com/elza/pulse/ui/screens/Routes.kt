package com.elza.pulse.ui.screens

sealed class Route(val route: String) {
    object Home : Route("home")
    object Search : Route("search")
    object SearchResult : Route("searchResult/{query}") {
        fun createRoute(query: String) = "searchResult/$query"
    }
    object Album : Route("album/{browseId}") {
        fun createRoute(browseId: String) = "album/$browseId"
    }
    object Artist : Route("artist/{browseId}") {
        fun createRoute(browseId: String) = "artist/$browseId"
    }
    object Settings : Route("settings")
}
