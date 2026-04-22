package com.elza.pulse.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class InnerTubeContext(
    val client: ClientContext
)

@Serializable
data class ClientContext(
    val clientName: String,
    val clientVersion: String,
    val hl: String = "en",
    val gl: String = "US"
)

@Serializable
data class SearchRequest(
    val context: InnerTubeContext,
    val query: String,
    val params: String? = null
)

@Serializable
data class SearchResponse(
    val contents: Contents? = null
)

@Serializable
data class Contents(
    val tabbedSearchResultsRenderer: TabbedSearchResultsRenderer? = null
)

@Serializable
data class TabbedSearchResultsRenderer(
    val tabs: List<Tab>? = null
)

@Serializable
data class Tab(
    val tabRenderer: TabRenderer? = null
)

@Serializable
data class TabRenderer(
    val content: SectionList? = null
)

@Serializable
data class SectionList(
    val sectionListRenderer: SectionListRenderer? = null
)

@Serializable
data class SectionListRenderer(
    val contents: List<SectionContent>? = null
)

@Serializable
data class SectionContent(
    val musicShelfRenderer: MusicShelfRenderer? = null,
    val musicCardShelfRenderer: MusicCardShelfRenderer? = null,
    val itemSectionRenderer: ItemSectionRenderer? = null,
    val musicCarouselShelfRenderer: MusicCarouselShelfRenderer? = null
)

@Serializable
data class MusicCarouselShelfRenderer(
    val contents: List<MusicCarouselShelfContent>? = null
)

@Serializable
data class MusicCarouselShelfContent(
    val musicTwoRowItemRenderer: MusicTwoRowItemRenderer? = null,
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
)

@Serializable
data class MusicTwoRowItemRenderer(
    val title: TextRenderer? = null,
    val thumbnailRenderer: ThumbnailRenderer? = null,
    val navigationEndpoint: NavigationEndpoint? = null
)

@Serializable
data class MusicCardShelfRenderer(
    val title: TextRenderer? = null,
    val contents: List<MusicCardShelfContent>? = null
)

@Serializable
data class MusicCardShelfContent(
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
)

@Serializable
data class ItemSectionRenderer(
    val contents: List<ItemSectionContent>? = null
)

@Serializable
data class ItemSectionContent(
    val musicShelfRenderer: MusicShelfRenderer? = null
)

@Serializable
data class MusicShelfRenderer(
    val title: TextRenderer? = null,
    val contents: List<MusicShelfContent>? = null
)

@Serializable
data class MusicShelfContent(
    val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null
)

@Serializable
data class MusicResponsiveListItemRenderer(
    val flexColumns: List<FlexColumn>? = null,
    val thumbnail: ThumbnailRenderer? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
    val overlay: Overlay? = null,
    val playlistItemData: PlaylistItemData? = null
)

@Serializable
data class Overlay(
    val musicItemThumbnailOverlayRenderer: MusicItemThumbnailOverlayRenderer? = null
)

@Serializable
data class MusicItemThumbnailOverlayRenderer(
    val content: MusicItemThumbnailOverlayContent? = null
)

@Serializable
data class MusicItemThumbnailOverlayContent(
    val musicPlayButtonRenderer: MusicPlayButtonRenderer? = null
)

@Serializable
data class MusicPlayButtonRenderer(
    val playNavigationEndpoint: NavigationEndpoint? = null
)

@Serializable
data class PlaylistItemData(
    val videoId: String? = null
)

@Serializable
data class FlexColumn(
    val musicResponsiveListItemFlexColumnRenderer: MusicResponsiveListItemFlexColumnRenderer? = null
)

@Serializable
data class MusicResponsiveListItemFlexColumnRenderer(
    val text: TextRenderer? = null
)

@Serializable
data class TextRenderer(
    val runs: List<Run>? = null
)

@Serializable
data class Run(
    val text: String? = null,
    val navigationEndpoint: NavigationEndpoint? = null
)

@Serializable
data class ThumbnailRenderer(
    val musicThumbnailRenderer: MusicThumbnailRenderer? = null
)

@Serializable
data class MusicThumbnailRenderer(
    val thumbnail: Thumbnails? = null
)

@Serializable
data class Thumbnails(
    val thumbnails: List<Thumbnail>? = null
)

@Serializable
data class Thumbnail(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class NavigationEndpoint(
    val browseEndpoint: BrowseEndpoint? = null,
    val watchEndpoint: WatchEndpoint? = null
)

@Serializable
data class BrowseEndpoint(
    val browseId: String? = null
)

@Serializable
data class WatchEndpoint(
    val videoId: String? = null
)
