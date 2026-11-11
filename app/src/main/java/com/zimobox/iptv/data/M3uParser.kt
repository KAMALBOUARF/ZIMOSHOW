package com.zimobox.iptv.data

object M3uParser {
    fun parse(content: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var name = "Unknown"
        var group = "Other"
        var logo: String? = null
        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    name = line.substringAfterLast(",").trim().ifBlank { "Unknown" }
                    group = Regex("group-title=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                        .find(line)?.groupValues?.get(1) ?: "Other"
                    logo = Regex("tvg-logo=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
                        .find(line)?.groupValues?.get(1)
                }
                line.startsWith("http://") || line.startsWith("https://") -> {
                    result += Channel(name, line, group, logo, "live")
                    name = "Unknown"; group = "Other"; logo = null
                }
            }
        }
        return result
    }
}
