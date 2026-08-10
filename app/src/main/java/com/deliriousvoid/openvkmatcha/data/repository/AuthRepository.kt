package com.deliriousvoid.openvkmatcha.data.repository

import android.util.Log
import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import com.deliriousvoid.openvkmatcha.services.LongPollService
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp

class AuthRepository(private val api: OpenVKApi) {

    fun hasToken(): Boolean = api.hasToken()

    fun getSavedInstance(): String = api.baseUrl

    suspend fun setOnline() = api.callMethod("account.setOnline")

    suspend fun login(username: String, password: String, code: String? = null) =
        api.login(username, password, code).onSuccess { token ->
            val app = OpenVKMatchaApp.instance
            LongPollService.start(app)
            app.onlineManager.start()
            
            // Save to account manager
            saveToAccountManager(token, api.baseUrl)
        }

    suspend fun loginWithToken(token: String) = api.loginWithToken(token).onSuccess {
        val app = OpenVKMatchaApp.instance
        app.longPollManager.start()
        app.onlineManager.start()

        // Save to account manager
        saveToAccountManager(token, api.baseUrl)
    }

    private suspend fun saveToAccountManager(token: String, instance: String) {
        val app = OpenVKMatchaApp.instance
        api.callMethod("users.get", mapOf("fields" to "photo_200,screen_name,verified")).onSuccess { json ->
            try {
                val response = json.getJSONArray("response").getJSONObject(0)
                val profile = JsonParsers.parseUser(response)
                app.accountManager.addAccount(token, instance, profile)
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to parse profile", e)
            }
        }
    }

    suspend fun validateSession() = api.validateToken()

    fun saveInstance(url: String) = api.saveInstance(url)

    fun switchAccount(accountId: String): Boolean {
        val app = OpenVKMatchaApp.instance
        val accounts = app.accountManager.getAccounts()
        val account = accounts.find { it.id == accountId } ?: return false
        
        app.accountManager.setCurrentAccountId(accountId)
        app.tokenManager.updateAccount(account.token, account.instance)
        
        LongPollService.stop(app)
        app.onlineManager.stop()
        LongPollService.start(app)
        app.onlineManager.start()
        
        return true
    }

    fun getAccounts() = OpenVKMatchaApp.instance.accountManager.getAccounts()
    
    fun removeAccount(id: String) = OpenVKMatchaApp.instance.accountManager.removeAccount(id)

    fun logout() {
        api.logout()
        LongPollService.stop(OpenVKMatchaApp.instance)
        OpenVKMatchaApp.instance.onlineManager.stop()
    }
}
