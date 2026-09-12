package com.deliriousvoid.openvkmatcha.data.model

data class GithubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val isPrerelease: Boolean,
    val htmlUrl: String,
    val assets: List<GithubAsset>
)

data class GithubAsset(
    val name: String,
    val browserDownloadUrl: String,
    val contentType: String,
    val size: Long
)
