package com.deliriousvoid.openvkmatcha

object Constants {
    const val DEFAULT_INSTANCE = "https://api.openvk.org"
    const val CLIENT_NAME = "Matcha"
    const val PLATFORM_ID = "android"
    const val API_VERSION = "5.131"

    const val CONNECT_TIMEOUT_SEC = 15L
    const val READ_TIMEOUT_SEC = 30L
    const val WRITE_TIMEOUT_SEC = 30L
    const val MIN_REQUEST_INTERVAL_MS = 100L

    const val POSTS_PER_PAGE = 20
    const val MESSAGES_PER_PAGE = 20
    const val AUDIO_PER_PAGE = 50

    val CUSTOM_VERIFIED_IDS = listOf(20350, 13990, -12083)
}
