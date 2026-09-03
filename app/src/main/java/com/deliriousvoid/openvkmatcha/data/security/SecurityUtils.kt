package com.deliriousvoid.openvkmatcha.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

object SecurityUtils {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val DEFAULT_MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    fun getEncryptedSharedPreferences(
        context: Context,
        fileName: String,
        masterKey: MasterKey
    ): SharedPreferences {
        return try {
            createEncryptedPrefs(context, fileName, masterKey)
        } catch (e: Exception) {
            // Handle Keystore crashes (e.g. android.security.KeyStoreException: -62)
            // by clearing the corrupted key and settings
            try {
                clearCorruptedKeyAndPrefs(context, fileName, DEFAULT_MASTER_KEY_ALIAS)
                createEncryptedPrefs(context, fileName, masterKey)
            } catch (e2: Exception) {
                // Final fallback to regular SharedPreferences if encryption is completely broken
                context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
            }
        }
    }

    private fun createEncryptedPrefs(
        context: Context,
        fileName: String,
        masterKey: MasterKey
    ): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            fileName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun clearCorruptedKeyAndPrefs(context: Context, fileName: String, keyAlias: String) {
        // Delete SharedPreferences file
        context.deleteSharedPreferences(fileName)

        // Delete key from Keystore
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }
}
