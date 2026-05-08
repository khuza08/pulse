package com.elza.pulsekmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform