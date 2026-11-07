package com.sc.player.data

data class Channel(
    val name: String,
    val url: String,
    val group: String,
    val logo: String?,
    val tvgId: String?,
)

data class Playlist(
    val id: String,
    val name: String,
    val url: String,
)
