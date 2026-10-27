package com.deliriousvoid.openvkmatcha.data.repository

import android.content.Context
import com.deliriousvoid.openvkmatcha.data.api.OpenVKApi
import com.deliriousvoid.openvkmatcha.data.parser.JsonParsers
import com.deliriousvoid.openvkmatcha.services.LongPollService
import com.deliriousvoid.openvkmatcha.OpenVKMatchaApp
import com.deliriousvoid.openvkmatcha.data.security.AccountManager
import com.deliriousvoid.openvkmatcha.util.MLog

class AuthRepository(private val api: OpenVKApi) {

    fun hasToken(): Boolean = api.hasToken()

    fun getSavedInstance(): String = api.baseUrl

    suspend fun setOnline() = api.callMethod("account.setOnline")

    suspend fun login(username: String, password: String, code: String? = null) =
        api.login(username, password, code).onSuccess { token ->
            val app = OpenVKMatchaApp.instance
            val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("background_notifications", false)) {
                LongPollService.start(app)
            }
            app.onlineManager.start()
            
            // Save to account manager
            saveToAccountManager(token, api.baseUrl)
        }

    suspend fun loginWithToken(token: String) = api.loginWithToken(token).onSuccess {
        val app = OpenVKMatchaApp.instance
        val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("background_notifications", false)) {
            LongPollService.start(app)
        }
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
                MLog.e("AuthRepository", "Failed to parse profile", e)
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
        app.longPollManager.stop()
        
        val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("background_notifications", false)) {
            LongPollService.start(app)
        }
        app.onlineManager.start()
        app.longPollManager.start()
        
        return true
    }

    fun getAccounts() = OpenVKMatchaApp.instance.accountManager.getAccounts()
    
    fun reorderAccounts(accounts: List<AccountManager.Account>) = 
        OpenVKMatchaApp.instance.accountManager.reorderAccounts(accounts)
    
    fun removeAccount(id: String) {
        val app = OpenVKMatchaApp.instance
        val isCurrent = app.accountManager.getCurrentAccountId() == id
        app.accountManager.removeAccount(id)
        if (isCurrent) {
            val nextAccount = app.accountManager.getCurrentAccount()
            if (nextAccount != null) {
                switchAccount(nextAccount.id)
            } else {
                logout()
            }
        }
    }

    fun logout() {
        api.logout()
        val app = OpenVKMatchaApp.instance
        LongPollService.stop(app)
        app.onlineManager.stop()
        app.longPollManager.stop()
    }
}
