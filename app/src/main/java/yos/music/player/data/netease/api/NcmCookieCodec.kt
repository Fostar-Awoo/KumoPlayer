package yos.music.player.data.netease.api

/** Cookie header utilities kept independent from Android so session persistence can be unit tested. */
internal object NcmCookieCodec {
    fun parse(header: String): LinkedHashMap<String, String> {
        val cookies = linkedMapOf<String, String>()
        header.split(';').forEach { part ->
            val pair = part.trim().split('=', limit = 2)
            if (pair.size == 2 && pair[0].isNotBlank()) {
                cookies[pair[0].trim()] = pair[1].trim()
            }
        }
        return cookies
    }

    fun serialize(cookies: Map<String, String>): String =
        cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" }

    fun normalize(header: String): String = serialize(parse(header))

    fun merge(current: String, incoming: String): String {
        val cookies = parse(current)
        cookies.putAll(parse(incoming))
        return serialize(cookies)
    }
}