package com.elza.pulse.innertube.models

data class SongItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: String?,
    val thumbnail: String?
)

data class ArtistItem(
    val id: String,
    val name: String,
    val thumbnail: String?
)
