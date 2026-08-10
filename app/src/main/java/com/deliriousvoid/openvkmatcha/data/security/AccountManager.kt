package com.deliriousvoid.openvkmatcha.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.deliriousvoid.openvkmatcha.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class AccountManager(context: Context) {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    data class Account(
        val id: String,
        val token: String,
        val instance: String,
        val userId: String,
        val fullName: String,
        val screenName: String,
        val photoUrl: String,
        val isVerified: Boolean
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("id", id)
            put("token", token)
            put("instance", instance)
            put("userId", userId)
            put("fullName", fullName)
            put("screenName", screenName)
            put("photoUrl", photoUrl)
            put("isVerified", isVerified)
        }

        companion object {
            fun fromJson(json: JSONObject): Account = Account(
                id = json.getString("id"),
                token = json.getString("token"),
                instance = json.getString("instance"),
                userId = json.getString("userId"),
                fullName = json.optString("fullName", ""),
                screenName = json.optString("screenName", ""),
                photoUrl = json.optString("photoUrl", ""),
                isVerified = json.optBoolean("isVerified", false)
            )
        }
    }

    fun addAccount(token: String, instance: String, profile: UserProfile) {
        val accounts = getAccounts().toMutableList()
        val userId = profile.id.toString()
        
        // Remove existing if same user on same instance
        accounts.removeAll { it.userId == userId && it.instance == instance }
        
        val account = Account(
            id = UUID.randomUUID().toString(),
            token = token,
            instance = instance,
            userId = userId,
            fullName = profile.fullName,
            screenName = profile.screenName,
            photoUrl = profile.photo200,
            isVerified = profile.verified
        )
        
        accounts.add(account)
        saveAccounts(accounts)
        setCurrentAccountId(account.id)
    }

    fun getAccounts(): List<Account> {
        val json = prefs.getString(KEY_ACCOUNTS, "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map {
            Account.fromJson(array.getJSONObject(it))
        }
    }

    private fun saveAccounts(accounts: List<Account>) {
        val array = JSONArray()
        accounts.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
    }

    fun getCurrentAccountId(): String? = prefs.getString(KEY_CURRENT_ACCOUNT_ID, null)

    fun setCurrentAccountId(id: String?) {
        prefs.edit().putString(KEY_CURRENT_ACCOUNT_ID, id).apply()
    }

    fun getCurrentAccount(): Account? {
        val currentId = getCurrentAccountId() ?: return null
        return getAccounts().find { it.id == currentId }
    }

    fun removeAccount(id: String) {
        val accounts = getAccounts().toMutableList()
        val toRemove = accounts.find { it.id == id }
        if (toRemove != null) {
            accounts.remove(toRemove)
            saveAccounts(accounts)
            if (getCurrentAccountId() == id) {
                setCurrentAccountId(accounts.firstOrNull()?.id)
            }
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "matcha_accounts"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_CURRENT_ACCOUNT_ID = "current_account_id"
    }
}
