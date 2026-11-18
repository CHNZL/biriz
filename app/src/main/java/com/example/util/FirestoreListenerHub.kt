package com.example.util

import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.atomic.AtomicLong

/**
 * Aynı sınıf veya turnuva alt koleksiyonuna birden fazla ekran/ViewModel tarafından açılan
 * gerçek zamanlı Firestore dinleyicilerini tek bir paylaşımlı akışta birleştiren (multiplexer)
 * ve başvuru sayısı (reference counting) sıfıra düştüğünde dinleyiciyi otomatik kapatan yönetici.
 *
 * - İlk abone Firestore'dan canlı dinleyiciyi başlatır.
 * - Aynı kanala katılan sonraki ekranlar ek Firestore okuması yapmadan son anlık görüntüyü
 *   hemen alır ve canlı güncellemeleri paylaşır.
 * - Tüm aboneler ayrıldığında gerçek Firestore ListenerRegistration kapatılır.
 */
object FirestoreListenerHub {

    private class SharedChannel<T>(
        val key: String
    ) {
        var upstream: ListenerRegistration? = null
        var hasLatest: Boolean = false
        var latestValue: T? = null
        val subscribers = LinkedHashMap<Long, (T) -> Unit>()
    }

    private val lock = Any()
    private val channels = HashMap<String, SharedChannel<Any?>>()
    private val nextToken = AtomicLong(1L)

    @Suppress("UNCHECKED_CAST")
    fun <T> subscribe(
        key: String,
        createUpstream: ((T) -> Unit) -> ListenerRegistration?,
        onUpdate: (T) -> Unit
    ): ListenerRegistration? {
        if (key.isBlank()) return null

        val token = nextToken.getAndIncrement()
        var needsUpstream = false
        var cachedValue: Any? = null
        var hasCached = false

        synchronized(lock) {
            val rawChannel = channels[key]
            val channel = if (rawChannel != null) {
                rawChannel as SharedChannel<T>
            } else {
                SharedChannel<T>(key).also { created ->
                    channels[key] = created as SharedChannel<Any?>
                }
            }
            channel.subscribers[token] = onUpdate
            if (channel.upstream == null) {
                needsUpstream = true
            } else if (channel.hasLatest) {
                hasCached = true
                cachedValue = channel.latestValue
            }
        }

        if (hasCached) {
            try {
                onUpdate(cachedValue as T)
            } catch (_: Exception) {
            }
        }

        if (needsUpstream) {
            val upstreamReg = try {
                createUpstream { value ->
                    dispatch(key, value)
                }
            } catch (_: Exception) {
                null
            }

            if (upstreamReg == null) {
                synchronized(lock) {
                    val channel = channels[key] as? SharedChannel<T>
                    if (channel != null) {
                        channel.subscribers.remove(token)
                        if (channel.subscribers.isEmpty() && channel.upstream == null) {
                            channels.remove(key)
                        }
                    }
                }
                return null
            }

            var shouldCloseImmediately = false
            synchronized(lock) {
                val channel = channels[key] as? SharedChannel<T>
                if (channel == null || channel.subscribers.isEmpty()) {
                    shouldCloseImmediately = true
                } else if (channel.upstream == null) {
                    channel.upstream = upstreamReg
                } else {
                    shouldCloseImmediately = true
                }
            }

            if (shouldCloseImmediately) {
                try {
                    upstreamReg.remove()
                } catch (_: Exception) {
                }
            }
        }

        return ListenerRegistration {
            unsubscribe(key, token)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> dispatch(key: String, value: T) {
        val snapshotCallbacks: List<(T) -> Unit> = synchronized(lock) {
            val channel = channels[key] as? SharedChannel<T> ?: return
            channel.latestValue = value
            channel.hasLatest = true
            channel.subscribers.values.toList()
        }
        snapshotCallbacks.forEach { callback ->
            try {
                callback(value)
            } catch (_: Exception) {
            }
        }
    }

    private fun unsubscribe(key: String, token: Long) {
        val toClose: ListenerRegistration? = synchronized(lock) {
            val channel = channels[key] ?: return
            channel.subscribers.remove(token)
            if (channel.subscribers.isEmpty()) {
                channels.remove(key)
                channel.upstream.also { channel.upstream = null }
            } else {
                null
            }
        }
        if (toClose != null) {
            try {
                toClose.remove()
            } catch (_: Exception) {
            }
        }
    }

    fun activeChannelCount(): Int {
        synchronized(lock) {
            return channels.size
        }
    }

    fun subscriberCount(key: String): Int {
        synchronized(lock) {
            return channels[key]?.subscribers?.size ?: 0
        }
    }

    fun clearClass(classId: String) {
        if (classId.isBlank()) return
        val suffix = ":$classId"
        val toClose = mutableListOf<ListenerRegistration>()
        synchronized(lock) {
            val keys = channels.keys.filter { it.endsWith(suffix) || it.contains("$suffix:") }
            keys.forEach { k ->
                val ch = channels.remove(k)
                ch?.subscribers?.clear()
                ch?.upstream?.let { toClose.add(it) }
            }
        }
        toClose.forEach {
            try {
                it.remove()
            } catch (_: Exception) {
            }
        }
    }

    fun clearAll() {
        val toClose = mutableListOf<ListenerRegistration>()
        synchronized(lock) {
            channels.values.forEach { ch ->
                ch.subscribers.clear()
                ch.upstream?.let { toClose.add(it) }
            }
            channels.clear()
        }
        toClose.forEach {
            try {
                it.remove()
            } catch (_: Exception) {
            }
        }
    }
}
