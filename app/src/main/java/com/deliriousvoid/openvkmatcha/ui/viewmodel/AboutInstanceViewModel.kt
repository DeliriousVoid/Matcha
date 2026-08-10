package com.deliriousvoid.openvkmatcha.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.model.InstanceInfo
import com.deliriousvoid.openvkmatcha.data.model.InstanceStatistics
import com.deliriousvoid.openvkmatcha.data.model.InstanceAdmins
import com.deliriousvoid.openvkmatcha.data.model.InstanceLinks
import com.deliriousvoid.openvkmatcha.data.model.InstanceLink
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class AboutInstanceViewModel : ViewModel() {
    private val api = OpenVKMatchaApp.instance.api

    private val _instanceInfo = MutableStateFlow<InstanceInfo?>(null)
    val instanceInfo = _instanceInfo.asStateFlow()

    private val _developers = MutableStateFlow<List<UserProfile>>(emptyList())
    val developers = _developers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                coroutineScope {
                    val instanceDeferred = async {
                        api.callMethod(
                            "ovk.aboutInstance",
                            mapOf(
                                "fields" to "statistics,links,administrators",
                                "admin_fields" to "photo_100,photo_200,online,verified,screen_name"
                            )
                        )
                    }

                    val devUserDeferred = async {
                        api.callMethod(
                            "users.get",
                            mapOf("user_ids" to "20350,13990", "fields" to "photo_200,online,verified,screen_name")
                        )
                    }

                    val devGroupDeferred = async {
                        api.callMethod(
                            "groups.getById",
                            mapOf("group_ids" to "12083", "fields" to "photo_200,verified,screen_name")
                        )
                    }

                    val instanceResult = instanceDeferred.await()
                    val devUserResult = devUserDeferred.await()
                    val devGroupResult = devGroupDeferred.await()

                    // Process instance info
                    instanceResult.onSuccess { json ->
                        val response = json.optJSONObject("response") ?: json
                        val info = InstanceInfo(
                            name = response.optString("name", "OpenVK"),
                            description = response.optString("description", ""),
                            openvkVersion = response.optString("openvk_version", ""),
                            statistics = response.optJSONObject("statistics")?.let { stats ->
                                InstanceStatistics(
                                    usersCount = stats.optInt("users_count"),
                                    onlineUsersCount = stats.optInt("online_users_count"),
                                    activeUsersCount = stats.optInt("active_users_count"),
                                    groupsCount = stats.optInt("groups_count")
                                )
                            },
                            administrators = response.optJSONObject("administrators")?.optJSONArray("items")?.let { items ->
                                val admins = mutableListOf<UserProfile>()
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    admins.add(parseUserProfile(item))
                                }
                                InstanceAdmins(admins)
                            },
                            links = response.optJSONObject("links")?.optJSONArray("items")?.let { items ->
                                val links = mutableListOf<InstanceLink>()
                                for (i in 0 until items.length()) {
                                    val item = items.getJSONObject(i)
                                    links.add(InstanceLink(item.optString("name"), item.optString("url")))
                                }
                                InstanceLinks(links)
                            }
                        )
                        _instanceInfo.value = info
                    }.onFailure {
                        _error.value = it.message ?: "Ошибка при загрузке информации"
                    }

                    // Process developers
                    val devs = mutableListOf<UserProfile>()
                    devUserResult.onSuccess { json ->
                        val response = json.optJSONArray("response")
                        if (response != null) {
                            for (i in 0 until response.length()) {
                                devs.add(parseUserProfile(response.getJSONObject(i)))
                            }
                        }
                    }
                    devGroupResult.onSuccess { json ->
                        val response = json.optJSONArray("response")
                        if (response != null) {
                            for (i in 0 until response.length()) {
                                devs.add(parseUserProfile(response.getJSONObject(i)))
                            }
                        }
                    }
                    _developers.value = devs
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Произошла непредвиденная ошибка"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseUserProfile(json: JSONObject): UserProfile {
        val rawId = json.optInt("id", 0)
        val groupId = json.optInt("group_id", 0)
        val isGroup = json.has("name") || groupId != 0 || rawId < 0
        
        val finalId = when {
            groupId != 0 -> -groupId
            isGroup && rawId > 0 -> -rawId
            else -> rawId
        }
        
        return UserProfile(
            id = finalId,
            firstName = json.optString("first_name", json.optString("name", "DELETED")),
            lastName = json.optString("last_name", ""),
            screenName = json.optString("screen_name", ""),
            photo50 = json.optString("photo_50", json.optString("photo_100")),
            photo200 = json.optString("photo_200", json.optString("photo_100")),
            status = json.optString("status", ""),
            online = json.optInt("online") == 1,
            verified = json.optInt("verified") == 1,
            isGroup = isGroup
        )
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AboutInstanceViewModel() as T
            }
        }
    }
}
