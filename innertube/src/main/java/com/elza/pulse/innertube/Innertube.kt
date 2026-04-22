package com.elza.pulse.innertube

import com.elza.pulse.innertube.models.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object Innertube {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"

    val httpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
    }.apply {
        sendPipeline.intercept(HttpSendPipeline.State) {
            val requestBuilder = this.context
            val host = if (requestBuilder.host == "youtubei.googleapis.com") "www.youtube.com" else requestBuilder.host
            val origin = "${requestBuilder.url.protocol.name}://$host"
            requestBuilder.header("host", host)
            requestBuilder.header("x-origin", origin)
            requestBuilder.header("origin", origin)
        }
    }

    private val client = httpClient.config {
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.youtube.com"
                encodedPath = "/youtubei/v1/"
                parameters.append("key", API_KEY)
                parameters.append("prettyPrint", "false")
            }
            contentType(ContentType.Application.Json)
            header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0")
            header("X-Goog-Api-Key", API_KEY)
            header("Origin", "https://music.youtube.com")
        }
    }

    val innerTubeContext = InnerTubeContext(
        client = ClientContext(
            clientName = "WEB_REMIX",
            clientVersion = "1.20240722.01.00"
        )
    )

    suspend fun search(query: String): SearchResponse {
        println("Innertube: Searching for: $query")
        return try {
            val responseString = client.post("search") {
                setBody(
                    SearchRequest(
                        context = innerTubeContext,
                        query = query
                    )
                )
            }.body<String>()
            
            if (!responseString.contains("musicResponsiveListItemRenderer")) {
                println("Innertube: musicResponsiveListItemRenderer not found in raw response")
                // Look for other possible renderers
                if (responseString.contains("musicListItemRenderer")) println("Innertube: Found musicListItemRenderer instead")
                if (responseString.contains("musicTwoRowItemRenderer")) println("Innertube: Found musicTwoRowItemRenderer instead")
            } else {
                println("Innertube: musicResponsiveListItemRenderer FOUND in raw response")
            }
            
            val response = json.decodeFromString<SearchResponse>(responseString)
            println("Innertube: Search successful for: $query")
            response
        } catch (e: Exception) {
            println("Innertube: Search failed for: $query")
            e.printStackTrace()
            throw e
        }
    }
}
