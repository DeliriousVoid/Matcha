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

    const val RECOMMENDATION_COVER_URL = "https://cdn.openvk.org/77/778810ca5531bd670a18094d8f046746fe68b9ade8510b166652273cc1ea0ec8b11ee9dc76f540ab21e017678c4b8dbcd892a69906285c7d3ae68600de0543bc.jpeg"

    const val SAVED_MESSAGES_AVATAR = "https://openvk.org/assets/packages/static/openvk/img/im/saved_messages.png"

    const val UNSUPPORTED_MESSAGE_TEXT = "данное сообщение не поддерживается в вашей версии Matcha. Проверьте последнюю версию на Github: https://github.com/DeliriousVoid/Matcha"
}
