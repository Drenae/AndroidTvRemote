package com.telecommande.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.telecommande.data.model.PairedTvInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class AppSettings(private val context: Context) {

    companion object {
        private val PAIRED_TV_NAME_KEY = stringPreferencesKey("paired_tv_name")
        private val LAST_USED_TV_IP_KEY = stringPreferencesKey("last_used_tv_ip")
        private val LAST_USED_TV_MAC_KEY = stringPreferencesKey("last_used_tv_mac")

        private val ALL_PAIRED_TVS_LIST_KEY = stringSetPreferencesKey("all_paired_tvs_list")
        private const val TAG = "AppSettings"
    }

    private fun Flow<Preferences>.catchPreferencesIOException(tag: String, message: String): Flow<Preferences> {
        return this.catch { exception ->
            if (exception is IOException) {
                Log.e(tag, "$message from preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
    }

    val activeTvNameFlow: Flow<String?> = context.dataStore.data
        .catchPreferencesIOException(TAG, "Error reading active TV name")
        .map { preferences -> preferences[PAIRED_TV_NAME_KEY] }

    val activeTvIpFlow: Flow<String?> = context.dataStore.data
        .catchPreferencesIOException(TAG, "Error reading active TV IP")
        .map { preferences -> preferences[LAST_USED_TV_IP_KEY] }

    val activeTvMacFlow: Flow<String?> = context.dataStore.data
        .catchPreferencesIOException(TAG, "Error reading active TV MAC")
        .map { preferences -> preferences[LAST_USED_TV_MAC_KEY] }

    suspend fun saveActiveTvInfo(tvInfo: PairedTvInfo?) {
        context.dataStore.edit { settings ->
            if (tvInfo != null) {
                settings[LAST_USED_TV_IP_KEY] = tvInfo.ipAddress
                tvInfo.name?.let { settings[PAIRED_TV_NAME_KEY] = it } ?: settings.remove(PAIRED_TV_NAME_KEY)
                tvInfo.macAddress?.let { settings[LAST_USED_TV_MAC_KEY] = it } ?: settings.remove(LAST_USED_TV_MAC_KEY)
                Log.d(TAG, "Active TV info saved: ${tvInfo.name} (${tvInfo.ipAddress})")
            } else {
                settings.remove(PAIRED_TV_NAME_KEY)
                settings.remove(LAST_USED_TV_IP_KEY)
                settings.remove(LAST_USED_TV_MAC_KEY)
                Log.d(TAG, "Active TV info cleared.")
            }
        }
    }

    suspend fun getActiveTvInfo(): PairedTvInfo? {
        val ip = activeTvIpFlow.firstOrNull() ?: return null
        val name = activeTvNameFlow.firstOrNull()
        val mac = activeTvMacFlow.firstOrNull()
        return PairedTvInfo(ip, name, mac)
    }

    suspend fun getActiveTvNameOrDefault(defaultName: String = "TV Android"): String {
        return activeTvNameFlow.firstOrNull() ?: defaultName
    }

    val allPairedTvsFlow: Flow<List<PairedTvInfo>> = context.dataStore.data
        .catchPreferencesIOException(TAG, "Error reading all paired TVs list")
        .map { preferences ->
            val jsonStringSet = preferences[ALL_PAIRED_TVS_LIST_KEY] ?: emptySet()
            jsonStringSet.mapNotNull { jsonString ->
                try {
                    Json.decodeFromString<PairedTvInfo>(jsonString)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding PairedTvInfo from JSON: $jsonString", e)
                    null
                }
            }
        }

    suspend fun getAllPairedTvs(): List<PairedTvInfo> {
        return allPairedTvsFlow.first()
    }

    suspend fun addOrUpdatePairedTv(tvInfo: PairedTvInfo) {
        context.dataStore.edit { settings ->
            val currentList = settings[ALL_PAIRED_TVS_LIST_KEY]?.mapNotNull {
                try { Json.decodeFromString<PairedTvInfo>(it) } catch (e: Exception) { null }
            } ?: emptyList()

            val updatedList = currentList.filterNot { it.ipAddress == tvInfo.ipAddress }.toMutableList()
            updatedList.add(0, tvInfo)

            settings[ALL_PAIRED_TVS_LIST_KEY] = updatedList.map { Json.encodeToString(it) }.toSet()
            Log.d(TAG, "Paired TV added/updated: ${tvInfo.name} (${tvInfo.ipAddress}). Total: ${updatedList.size}")
        }
    }

    suspend fun renamePairedTv(ipAddress: String, newName: String) {
        context.dataStore.edit { settings ->
            val currentJsonSet = settings[ALL_PAIRED_TVS_LIST_KEY] ?: return@edit
            val updatedJsonSet = mutableSetOf<String>()
            var foundAndUpdated = false

            currentJsonSet.forEach { jsonString ->
                try {
                    var tv = Json.decodeFromString<PairedTvInfo>(jsonString)
                    if (tv.ipAddress == ipAddress) {
                        tv = tv.copy(name = newName)
                        foundAndUpdated = true
                    }
                    updatedJsonSet.add(Json.encodeToString(tv))
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing TV for rename: $jsonString", e)
                    updatedJsonSet.add(jsonString)
                }
            }

            if (foundAndUpdated) {
                settings[ALL_PAIRED_TVS_LIST_KEY] = updatedJsonSet
                Log.d(TAG, "TV renamed: IP $ipAddress to $newName")

                if (activeTvIpFlow.firstOrNull() == ipAddress) {
                    settings[PAIRED_TV_NAME_KEY] = newName
                }
            }
        }
    }

    suspend fun removePairedTv(ipAddress: String) {
        context.dataStore.edit { settings ->
            val currentJsonSet = settings[ALL_PAIRED_TVS_LIST_KEY] ?: return@edit
            val updatedJsonSet = currentJsonSet.filterNot { jsonString ->
                try {
                    Json.decodeFromString<PairedTvInfo>(jsonString).ipAddress == ipAddress
                } catch (e: Exception) {
                    false
                }
            }.toSet()

            if (updatedJsonSet.size < currentJsonSet.size) {
                settings[ALL_PAIRED_TVS_LIST_KEY] = updatedJsonSet
                Log.d(TAG, "Paired TV removed: IP $ipAddress. Remaining: ${updatedJsonSet.size}")

                if (settings[LAST_USED_TV_IP_KEY] == ipAddress) {
                    settings.remove(PAIRED_TV_NAME_KEY)
                    settings.remove(LAST_USED_TV_IP_KEY)
                    settings.remove(LAST_USED_TV_MAC_KEY)
                    Log.d(TAG, "Active TV info cleared because it was removed from the list.")
                }
            }
        }
    }

    @Deprecated("Utilisez saveActiveTvInfo pour gérer la TV active.", ReplaceWith("saveActiveTvInfo(PairedTvInfo(ipAddress, name, macAddress))"))
    suspend fun savePairedTvName(name: String?) {
        Log.w(TAG, "savePairedTvName est dépréciée. Utilisez saveActiveTvInfo.")
    }

    @Deprecated("Utilisez getActiveTvNameOrDefault.", ReplaceWith("getActiveTvNameOrDefault(defaultName)"))
    suspend fun getStoredTvNameOrDefault(defaultName: String = "TV Android"): String {
        Log.w(TAG, "getStoredTvNameOrDefault est dépréciée. Utilisez getActiveTvNameOrDefault.")
        return getActiveTvNameOrDefault(defaultName)
    }

    @Deprecated("Utilisez saveActiveTvInfo.", ReplaceWith("saveActiveTvInfo(PairedTvInfo(ipAddress, currentName, currentMac))"))
    suspend fun saveLastUsedTvIp(ipAddress: String?) {
        Log.w(TAG, "saveLastUsedTvIp est dépréciée. Utilisez saveActiveTvInfo.")
    }

    @Deprecated("Utilisez getActiveTvInfo()?.ipAddress.", ReplaceWith("getActiveTvInfo()?.ipAddress"))
    suspend fun getLastUsedTvIp(): String? {
        Log.w(TAG, "getLastUsedTvIp est dépréciée. Utilisez getActiveTvInfo()?.ipAddress.")
        return getActiveTvInfo()?.ipAddress
    }

    @Deprecated("Utilisez saveActiveTvInfo.", ReplaceWith("saveActiveTvInfo(PairedTvInfo(currentIp, currentName, macAddress))"))
    suspend fun saveLastUsedTvMac(macAddress: String?) {
        Log.w(TAG, "saveLastUsedTvMac est dépréciée. Utilisez saveActiveTvInfo.")
    }

    @Deprecated("Utilisez getActiveTvInfo()?.macAddress.", ReplaceWith("getActiveTvInfo()?.macAddress"))
    suspend fun getLastUsedTvMac(): String? {
        Log.w(TAG, "getLastUsedTvMac est dépréciée. Utilisez getActiveTvInfo()?.macAddress.")
        return getActiveTvInfo()?.macAddress
    }

    suspend fun clearAllStoredData() {
        context.dataStore.edit { settings ->
            settings.remove(PAIRED_TV_NAME_KEY)
            settings.remove(LAST_USED_TV_IP_KEY)
            settings.remove(LAST_USED_TV_MAC_KEY)
            settings.remove(ALL_PAIRED_TVS_LIST_KEY)
            Log.d(TAG, "All stored TV settings (active TV and paired list) cleared.")
        }
    }
}