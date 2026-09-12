package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import com.deliriousvoid.openvkmatcha.data.model.AudioTrack
import com.deliriousvoid.openvkmatcha.data.model.Playlist

class MusicRepository(val api: OpenVKApi) {

    suspend fun loadMainMusicData(userId: Int, tracksOffset: Int = 0, tracksCount: Int = 50, playlistsCount: Int = 15): Result<Pair<List<AudioTrack>, List<Playlist>>> = api.execute(
        """
            var t = API.audio.get({"owner_id": $userId, "offset": $tracksOffset, "count": $tracksCount});
            var p = API.audio.getPlaylists({"owner_id": $userId, "count": $playlistsCount});
            return { "tracks": t, "playlists": p };
        """.trimIndent()
    ).map { json ->
        val response = json.getJSONObject("response")
        val tracks = JsonParsers.parseAudio(response.getJSONObject("tracks"))
        val playlists = JsonParsers.parsePlaylists(response.getJSONObject("playlists"))
        Pair(tracks, playlists)
    }

    suspend fun loadMyAudio(userId: Int, offset: Int = 0, count: Int = 50) = api.callMethod(
        "audio.get",
        mapOf(
            "owner_id" to userId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
        ),
    ).map { JsonParsers.parseAudio(it) }

    suspend fun searchAudio(query: String, offset: Int = 0, count: Int = 30) = api.callMethod(
        "audio.search",
        mapOf(
            "q" to query,
            "offset" to offset.toString(),
            "count" to count.toString(),
        ),
    ).map { JsonParsers.parseAudio(it) }

    suspend fun addAudio(audioId: Int, ownerId: Int) = api.callMethod(
        "audio.add",
        mapOf(
            "audio_id" to audioId.toString(),
            "owner_id" to ownerId.toString(),
        ),
    )

    suspend fun deleteAudio(audioId: Int, ownerId: Int) = api.callMethod(
        "audio.delete",
        mapOf(
            "audio_id" to audioId.toString(),
            "owner_id" to ownerId.toString(),
        ),
    )

    suspend fun loadPlaylists(ownerId: Int, offset: Int = 0, count: Int = 50) = api.callMethod(
        "audio.getPlaylists",
        mapOf(
            "owner_id" to ownerId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
        )
    ).map { JsonParsers.parsePlaylists(it) }

    suspend fun searchPlaylists(query: String, offset: Int = 0, count: Int = 50) = api.callMethod(
        "audio.searchAlbums",
        mapOf(
            "query" to query,
            "offset" to offset.toString(),
            "limit" to count.toString(),
        )
    ).map { JsonParsers.parsePlaylists(it) }

    suspend fun loadPlaylistTracks(ownerId: Int, playlistId: Int, offset: Int = 0, count: Int = 200) = api.callMethod(
        "audio.get",
        mapOf(
            "owner_id" to ownerId.toString(),
            "album_id" to playlistId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
        )
    ).map { JsonParsers.parseAudio(it) }

    suspend fun bookmarkPlaylist(playlistId: Int) = api.callMethod(
        "audio.bookmarkAlbum",
        mapOf("id" to playlistId.toString())
    )

    suspend fun unbookmarkPlaylist(playlistId: Int) = api.callMethod(
        "audio.unbookmarkAlbum",
        mapOf("id" to playlistId.toString())
    )

    suspend fun createPlaylist(title: String, description: String = "") = api.callMethod(
        "audio.addAlbum",
        mapOf(
            "title" to title,
            "description" to description
        )
    )

    suspend fun editPlaylist(playlistId: Int, title: String, description: String = "") = api.callMethod(
        "audio.editAlbum",
        mapOf(
            "album_id" to playlistId.toString(),
            "title" to title,
            "description" to description
        )
    )

    suspend fun deletePlaylist(playlistId: Int) = api.callMethod(
        "audio.deleteAlbum",
        mapOf("album_id" to playlistId.toString())
    )

    suspend fun addAudioToPlaylist(playlistId: Int, audioIds: String) = api.callMethod(
        "audio.moveToAlbum",
        mapOf(
            "album_id" to playlistId.toString(),
            "audio_ids" to audioIds
        )
    )

    suspend fun removeAudioFromPlaylist(playlistId: Int, track: AudioTrack) = api.callMethod(
        "audio.removeFromAlbum",
        mapOf(
            "owner_id" to track.ownerId.toString(),
            "album_id" to playlistId.toString(),
            "audio_ids" to track.stableId
        )
    )
}
