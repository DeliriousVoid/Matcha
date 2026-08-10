package com.deliriousvoid.openvkmatcha.data.repository

import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import org.json.JSONArray
import org.json.JSONObject

class NotesRepository(private val api: OpenVKApi) {
    suspend fun getNotes(userId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "notes.get",
        mapOf(
            "user_id" to userId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "sort" to "1"
        )
    ).map { JsonParsers.parseNotes(it) }

    suspend fun getNoteById(ownerId: Int, noteId: Int) = api.callMethod(
        "notes.getById",
        mapOf(
            "owner_id" to ownerId.toString(),
            "note_id" to noteId.toString()
        )
    ).mapCatching { json ->
        val resp = json.opt("response")
        val items = when (resp) {
            is JSONArray -> resp
            is JSONObject -> resp.optJSONArray("items") ?: JSONArray().apply { if (resp.has("id") || resp.has("nid")) put(resp) }
            else -> JSONArray()
        }
        
        if (items.length() > 0) {
            JsonParsers.parseNote(items.getJSONObject(0))
        } else {
            // Try fallback to notes.get if ownerId is the user
            val list = getNotes(ownerId, 0, 100).getOrNull()
            list?.items?.find { it.id == noteId }
        }
    }

    suspend fun addNote(title: String, text: String) = api.callMethod(
        "notes.add",
        mapOf(
            "title" to title,
            "text" to text
        ),
        isPost = true
    )

    suspend fun editNote(noteId: Int, title: String, text: String) = api.callMethod(
        "notes.edit",
        mapOf(
            "note_id" to noteId.toString(),
            "title" to title,
            "text" to text
        ),
        isPost = true
    )

    suspend fun deleteNote(noteId: Int) = api.callMethod(
        "notes.delete",
        mapOf("note_id" to noteId.toString())
    )

    suspend fun getComments(ownerId: Int, noteId: Int, offset: Int = 0, count: Int = 30) = api.callMethod(
        "notes.getComments",
        mapOf(
            "owner_id" to ownerId.toString(),
            "note_id" to noteId.toString(),
            "offset" to offset.toString(),
            "count" to count.toString(),
            "extended" to "1",
            "fields" to "photo_50,verified"
        )
    ).map { JsonParsers.parseComments(it) }

    suspend fun createComment(ownerId: Int, noteId: Int, message: String) = api.callMethod(
        "notes.createComment",
        mapOf(
            "owner_id" to ownerId.toString(),
            "note_id" to noteId.toString(),
            "message" to message
        ),
        isPost = true
    )
}
