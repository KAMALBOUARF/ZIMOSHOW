package com.zimobox.iptv.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class IptvRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS).build()

    private fun get(url: String): String {
        val request = Request.Builder().url(url)
            .header("User-Agent", "ZIMO IPTV PLAYER").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    fun loadM3u(url: String): List<Channel> {
        require(url.startsWith("http://") || url.startsWith("https://")) { "رابط M3U غير صحيح" }
        return M3uParser.parse(get(url))
    }

    fun loadXtream(hostInput: String, user: String, pass: String, action: String): List<Channel> {
        val host = hostInput.trim().trimEnd('/')
        require(host.startsWith("http://") || host.startsWith("https://")) {
            "أدخل عنوان الخادم كاملاً مع http أو https"
        }
        val base = "$host/player_api.php?username=${enc(user)}&password=${enc(pass)}"
        val endpoint = when (action) {
            "live" -> "get_live_streams"
            "movies" -> "get_vod_streams"
            else -> "get_series"
        }
        val json = JSONArray(get("$base&action=$endpoint"))
        val result = mutableListOf<Channel>()
        for (i in 0 until json.length()) {
            val item = json.optJSONObject(i) ?: continue
            val name = item.optString("name", "Unknown")
            val id = item.optString("stream_id", item.optString("series_id", ""))
            if (id.isBlank()) continue
            val path = when (action) {
                "live" -> "live/$user/$pass/$id.ts"
                "movies" -> "movie/$user/$pass/$id.mp4"
                else -> "series/$user/$pass/$id"
            }
            result += Channel(name, "$host/$path",
                item.optString("category_name", action), item.optString("stream_icon").takeIf { it.isNotBlank() }, action)
        }
        return result
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
}
