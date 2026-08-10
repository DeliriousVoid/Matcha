package com.deliriousvoid.openvkmatcha.data.model

data class LyricsRecord(
    val id: Int,
    val name: String,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val duration: Int,
    val instrumental: Boolean,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

data class LrcLine(
    val timestampMs: Long,
    val text: String
)
