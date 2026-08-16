package dev.wolly.dsbmaterial.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val CLASS_NAME = stringPreferencesKey("class_name")
        val SWAP_DATA = booleanPreferencesKey("swap_data")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val SORT_PERIOD = booleanPreferencesKey("sort_period")
        val ARCHIVE = stringPreferencesKey("archive")
        val THEME_INDEX = intPreferencesKey("theme_index")
        val NAV_HIDDEN = booleanPreferencesKey("nav_hidden")
        val SELECTED_CLASSES = stringPreferencesKey("selected_classes")
        val USE_CUSTOM_FONT = booleanPreferencesKey("use_custom_font")
        val FONT_WEIGHT = floatPreferencesKey("font_weight")
        val FONT_WIDTH = floatPreferencesKey("font_width")
        val FONT_OPSZ = floatPreferencesKey("font_opsz")
        val FONT_SLNT = floatPreferencesKey("font_slnt")
        val FONT_GRAD = floatPreferencesKey("font_grad")
        val FONT_ROND = floatPreferencesKey("font_rond")
        val AUTO_FETCH_ENABLED = booleanPreferencesKey("auto_fetch_enabled")
        val AUTO_FETCH_INTERVAL = intPreferencesKey("auto_fetch_interval")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CUSTOM_SERVER_URL = stringPreferencesKey("custom_server_url")
        val CACHED_ENTRIES = stringPreferencesKey("cached_entries")
        val LAST_UPDATED = longPreferencesKey("last_updated")
        val WEB_SERVER_ENABLED = booleanPreferencesKey("web_server_enabled")
        val AUTO_UPDATE_CHECK = booleanPreferencesKey("auto_update_check")
        val UPDATE_CHANNEL = stringPreferencesKey("update_channel")
        val HAPTICS = booleanPreferencesKey("haptics")
        val SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
    }

    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[USERNAME] }
    val passwordFlow: Flow<String?> = context.dataStore.data.map { it[PASSWORD] }
    val classNameFlow: Flow<String?> = context.dataStore.data.map { it[CLASS_NAME] }
    val swapDataFlow: Flow<Boolean> = context.dataStore.data.map { it[SWAP_DATA] ?: true }
    val dynamicColorFlow: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }
    val amoledModeFlow: Flow<Boolean> = context.dataStore.data.map { it[AMOLED_MODE] ?: false }
    val sortPeriodFlow: Flow<Boolean> = context.dataStore.data.map { it[SORT_PERIOD] ?: true }
    val archiveFlow: Flow<String?> = context.dataStore.data.map { it[ARCHIVE] }
    val themeIndexFlow: Flow<Int> = context.dataStore.data.map { it[THEME_INDEX] ?: 0 }
    val navHiddenFlow: Flow<Boolean> = context.dataStore.data.map { it[NAV_HIDDEN] ?: true }
    val selectedClassesFlow: Flow<String?> = context.dataStore.data.map { it[SELECTED_CLASSES] }
    val useCustomFontFlow: Flow<Boolean> = context.dataStore.data.map { it[USE_CUSTOM_FONT] ?: true }
    val fontWeightFlow: Flow<Float> = context.dataStore.data.map { it[FONT_WEIGHT] ?: 400f }
    val fontWidthFlow: Flow<Float> = context.dataStore.data.map { it[FONT_WIDTH] ?: 100f }
    val fontOpszFlow: Flow<Float> = context.dataStore.data.map { it[FONT_OPSZ] ?: 14f }
    val fontSlntFlow: Flow<Float> = context.dataStore.data.map { it[FONT_SLNT] ?: 0f }
    val fontGradFlow: Flow<Float> = context.dataStore.data.map { it[FONT_GRAD] ?: 0f }
    val fontRondFlow: Flow<Float> = context.dataStore.data.map { it[FONT_ROND] ?: 100f }
    val autoFetchEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_FETCH_ENABLED] ?: false }
    val autoFetchIntervalFlow: Flow<Int> = context.dataStore.data.map { it[AUTO_FETCH_INTERVAL] ?: 30 }
    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: false }
    val customServerUrlFlow: Flow<String?> = context.dataStore.data.map { it[CUSTOM_SERVER_URL] }
    val cachedEntriesFlow: Flow<String?> = context.dataStore.data.map { it[CACHED_ENTRIES] }
    val lastUpdatedFlow: Flow<Long> = context.dataStore.data.map { it[LAST_UPDATED] ?: 0L }
    val webServerEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[WEB_SERVER_ENABLED] ?: false }
    val autoUpdateCheckFlow: Flow<Boolean> = context.dataStore.data.map { it[AUTO_UPDATE_CHECK] ?: true }
    val updateChannelFlow: Flow<String> = context.dataStore.data.map { it[UPDATE_CHANNEL] ?: "stable" }
    val hapticsFlow: Flow<Boolean> = context.dataStore.data.map { it[HAPTICS] ?: true }
    val setupCompletedFlow: Flow<Boolean> = context.dataStore.data.map { it[SETUP_COMPLETED] ?: false }

    suspend fun saveCredentials(username: String, password: String, className: String) {
        context.dataStore.edit { settings ->
            settings[USERNAME] = username
            settings[PASSWORD] = password
            settings[CLASS_NAME] = className
        }
    }

    suspend fun saveSelectedClasses(classes: List<String>) {
        context.dataStore.edit { it[SELECTED_CLASSES] = classes.joinToString(",") }
    }

    suspend fun saveSwapPreference(isRoomFirst: Boolean) {
        context.dataStore.edit { it[SWAP_DATA] = isRoomFirst }
    }

    suspend fun saveDynamicColorPreference(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun saveAmoledMode(enabled: Boolean) {
        context.dataStore.edit { it[AMOLED_MODE] = enabled }
    }

    suspend fun saveThemeIndex(index: Int) {
        context.dataStore.edit { it[THEME_INDEX] = index }
    }

    suspend fun saveSortPreference(enabled: Boolean) {
        context.dataStore.edit { it[SORT_PERIOD] = enabled }
    }

    suspend fun saveArchive(json: String) {
        context.dataStore.edit { it[ARCHIVE] = json }
    }
    
    suspend fun saveNavHiddenPreference(hidden: Boolean) {
        context.dataStore.edit { it[NAV_HIDDEN] = hidden }
    }

    suspend fun saveCustomFont(enabled: Boolean) {
        context.dataStore.edit { it[USE_CUSTOM_FONT] = enabled }
    }

    suspend fun saveFontWeight(value: Float) {
        context.dataStore.edit { it[FONT_WEIGHT] = value }
    }

    suspend fun saveFontWidth(value: Float) {
        context.dataStore.edit { it[FONT_WIDTH] = value }
    }

    suspend fun saveFontOpsz(value: Float) {
        context.dataStore.edit { it[FONT_OPSZ] = value }
    }

    suspend fun saveFontSlnt(value: Float) {
        context.dataStore.edit { it[FONT_SLNT] = value }
    }

    suspend fun saveFontGrad(value: Float) {
        context.dataStore.edit { it[FONT_GRAD] = value }
    }

    suspend fun saveFontRond(value: Float) {
        context.dataStore.edit { it[FONT_ROND] = value }
    }

    suspend fun saveAutoFetchEnabled(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_FETCH_ENABLED] = enabled }
    }

    suspend fun saveAutoFetchInterval(minutes: Int) {
        context.dataStore.edit { it[AUTO_FETCH_INTERVAL] = minutes }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun saveCustomServerUrl(url: String) {
        context.dataStore.edit { settings ->
            if (url.isBlank()) settings.remove(CUSTOM_SERVER_URL)
            else settings[CUSTOM_SERVER_URL] = url
        }
    }

    suspend fun saveCachedEntries(json: String) {
        context.dataStore.edit { it[CACHED_ENTRIES] = json }
    }

    suspend fun saveLastUpdated(timestamp: Long) {
        context.dataStore.edit { it[LAST_UPDATED] = timestamp }
    }

    suspend fun saveWebServerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[WEB_SERVER_ENABLED] = enabled }
    }

    suspend fun saveAutoUpdateCheck(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_UPDATE_CHECK] = enabled }
    }

    suspend fun saveUpdateChannel(channel: String) {
        context.dataStore.edit { it[UPDATE_CHANNEL] = channel }
    }

    suspend fun saveHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HAPTICS] = enabled }
    }

    suspend fun saveSetupCompleted(completed: Boolean) {
        context.dataStore.edit { it[SETUP_COMPLETED] = completed }
    }

    suspend fun clearCredentials() {
        context.dataStore.edit { it.clear() }
    }
}
