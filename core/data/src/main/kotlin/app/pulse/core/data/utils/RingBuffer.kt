package app.pulse.core.data.utils

import android.net.Uri

open class RingBuffer<T>(val size: Int, private val init: (index: Int) -> T) : Iterable<T> {
    private val list = MutableList(size, init)

    @get:Synchronized
    @set:Synchronized
    private var index = 0

    operator fun get(index: Int) = list.getOrNull(index)
    operator fun plusAssign(element: T) {
        list[index++ % size] = element
    }

    override fun iterator() = list.iterator()

    fun clear() = list.indices.forEach {
        list[it] = init(it)
    }
}

class UriCache<Key : Any, Meta>(size: Int = 16) {
    private val buffer = RingBuffer<CachedUri<Key, Meta>?>(size) { null }

    data class CachedUri<Key, Meta> internal constructor(
        val key: Key,
        val meta: Meta,
        val uri: Uri
    )

    operator fun get(key: Key) = buffer.find { it != null && it.key == key }

    fun push(
        key: Key,
        meta: Meta,
        uri: Uri
    ) {
        buffer += CachedUri(key, meta, uri)
    }

    fun clear() = buffer.clear()
}
