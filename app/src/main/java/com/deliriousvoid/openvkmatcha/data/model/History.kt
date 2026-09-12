package com.deliriousvoid.openvkmatcha.data.model

data class AuthorInfo(
    val id: Int,
    val name: String,
    val avatar: String,
    val verified: Boolean = false,
    val online: Boolean = false,
    val mobileOnline: Boolean = false,
)

data class ParsedHistory(
    val messages: List<ChatMessage>,
    val authors: Map<Int, AuthorInfo>
)
