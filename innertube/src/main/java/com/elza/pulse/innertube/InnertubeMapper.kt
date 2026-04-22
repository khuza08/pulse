package com.elza.pulse.innertube

import com.elza.pulse.innertube.models.*

object InnertubeMapper {
    fun fromSearchResponse(response: SearchResponse): List<SongItem> {
        val songs = mutableListOf<SongItem>()
        
        // Navigate through the tabs and sections to find the music shelves
        val tabs = response.contents?.tabbedSearchResultsRenderer?.tabs
        println("InnertubeMapper: Tabs found: ${tabs?.size ?: 0}")
        
        tabs?.forEach { tab ->
            val sections = tab.tabRenderer?.content?.sectionListRenderer?.contents
            println("InnertubeMapper: Sections found in tab: ${sections?.size ?: 0}")
            
            sections?.forEach { sectionContent ->
                // Check direct musicShelfRenderer
                sectionContent.musicShelfRenderer?.let { shelf ->
                    println("InnertubeMapper: Direct music shelf found")
                    processShelf(shelf, songs)
                }

                // Check musicCardShelfRenderer (Top result)
                sectionContent.musicCardShelfRenderer?.let { shelf ->
                    println("InnertubeMapper: Music card shelf found")
                    shelf.contents?.forEach { musicCardShelfContent ->
                        musicCardShelfContent.musicResponsiveListItemRenderer?.let { renderer ->
                            mapToSongItem(renderer)?.let { songs.add(it) }
                        }
                    }
                }

                // Check musicCarouselShelfRenderer
                sectionContent.musicCarouselShelfRenderer?.let { shelf ->
                    println("InnertubeMapper: Music carousel shelf found")
                    shelf.contents?.forEach { carouselContent ->
                        carouselContent.musicResponsiveListItemRenderer?.let { renderer ->
                            mapToSongItem(renderer)?.let { songs.add(it) }
                        }
                        // Handle two row item renderer if needed
                    }
                }
                
                // Check musicShelfRenderer inside itemSectionRenderer
                sectionContent.itemSectionRenderer?.contents?.forEach { itemSectionContent ->
                    itemSectionContent.musicShelfRenderer?.let { shelf ->
                        println("InnertubeMapper: Nested music shelf found in itemSectionRenderer")
                        processShelf(shelf, songs)
                    }
                }
            }
        }
        
        println("InnertubeMapper: Total songs mapped: ${songs.size}")
        return songs
    }

    private fun processShelf(shelf: MusicShelfRenderer, songs: MutableList<SongItem>) {
        val contents = shelf.contents
        println("InnertubeMapper: Shelf has ${contents?.size ?: 0} items")
        
        contents?.forEachIndexed { index, item ->
            val renderer = item.musicResponsiveListItemRenderer
            if (renderer != null) {
                val songItem = mapToSongItem(renderer)
                if (songItem != null) {
                    songs.add(songItem)
                } else {
                    println("InnertubeMapper: Item $index failed to map. FlexColumns: ${renderer.flexColumns?.size ?: 0}")
                }
            } else {
                println("InnertubeMapper: Item $index has no musicResponsiveListItemRenderer")
            }
        }
    }

    private fun mapToSongItem(renderer: MusicResponsiveListItemRenderer): SongItem? {
        val flexColumns = renderer.flexColumns ?: run {
            println("InnertubeMapper: flexColumns is null")
            return null
        }
        
        // Find title: usually the first run of the first flex column
        val titleColumn = flexColumns.getOrNull(0)
        val titleRun = titleColumn?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
        val title = titleRun?.text
        if (title == null) {
            println("InnertubeMapper: title is null. Column present: ${titleColumn != null}")
            return null
        }
        
        // Find artist: usually in the second flex column
        val secondColumnRuns = flexColumns.getOrNull(1)
            ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs ?: emptyList()
        
        val artist = secondColumnRuns.firstOrNull()?.text ?: "Unknown Artist"
        
        // Find videoId in various possible locations
        val videoId = renderer.navigationEndpoint?.watchEndpoint?.videoId
            ?: renderer.playlistItemData?.videoId
            ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
            ?: titleRun.navigationEndpoint?.watchEndpoint?.videoId
            ?: secondColumnRuns.mapNotNull { it.navigationEndpoint?.watchEndpoint?.videoId }.firstOrNull()
        
        if (videoId == null) {
            println("InnertubeMapper: videoId is null for '$title'")
            return null
        }
        
        val thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url

        return SongItem(
            id = videoId,
            title = title,
            artist = artist,
            album = null,
            duration = null,
            thumbnail = thumbnail
        )
    }
}
