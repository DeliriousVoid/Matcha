package com.deliriousvoid.openvkmatcha.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.deliriousvoid.openvkmatcha.Constants

class TokenManager(context: Context) {
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

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun saveInstance(url: String) {
        val cleanUrl = url.trimEnd('/')
        prefs.edit().putString(KEY_INSTANCE, cleanUrl).apply()
    }

    fun getInstance(): String = prefs.getString(KEY_INSTANCE, Constants.DEFAULT_INSTANCE)!!

    fun updateAccount(token: String, instance: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_INSTANCE, instance.trimEnd('/'))
            .apply()
    }

    fun getClientName(): String = prefs.getString(KEY_CLIENT_NAME, Constants.CLIENT_NAME) ?: Constants.CLIENT_NAME

    fun saveClientName(name: String) {
        prefs.edit().putString(KEY_CLIENT_NAME, name).apply()
    }

    fun hasToken(): Boolean = !getToken().isNullOrBlank()

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    companion object {
        private const val PREF_NAME = "matcha_prefs"
        private const val KEY_TOKEN = "access_token"
        private const val KEY_INSTANCE = "instance_url"
        private const val KEY_CLIENT_NAME = "client_name"
    }
}
