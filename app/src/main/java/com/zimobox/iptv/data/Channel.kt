package com.zimobox.iptv.data

data class Channel(
    val name: String,
    val url: String,
    val group: String = "Other",
    val logo: String? = null,
    val kind: String = "live"
)
