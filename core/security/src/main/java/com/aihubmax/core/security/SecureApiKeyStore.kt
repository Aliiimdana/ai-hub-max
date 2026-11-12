package com.aihubmax.core.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secureApiDataStore by preferencesDataStore(name = "secure_api_keys")

@Singleton
class SecureApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cryptoManager: KeystoreCryptoManager
) {

    private fun keyForProvider(providerId: String) = stringPreferencesKey("enc_key_$providerId")

    val connectedProviderIds: Flow<Set<String>> = context.secureApiDataStore.data.map { prefs ->
        prefs.asMap().keys
            .mapNotNull { prefKey ->
                val name = prefKey.name
                if (name.startsWith("enc_key_")) name.removePrefix("enc_key_") else null
            }
            .toSet()
    }

    suspend fun saveApiKey(providerId: String, rawApiKey: String) {
        val encrypted = cryptoManager.encrypt(rawApiKey.trim())
        context.secureApiDataStore.edit { prefs ->
            prefs[keyForProvider(providerId)] = encrypted
        }
    }

    suspend fun getApiKey(providerId: String): String? {
        val prefs = context.secureApiDataStore.data.first()
        val encrypted = prefs[keyForProvider(providerId)] ?: return null
        return runCatching { cryptoManager.decrypt(encrypted) }.getOrNull()
    }

    suspend fun removeApiKey(providerId: String) {
        context.secureApiDataStore.edit { prefs ->
            prefs.remove(keyForProvider(providerId))
        }
    }

    suspend fun clearAllKeys() {
        context.secureApiDataStore.edit { it.clear() }
    }
}
