package dev.wolly.dsbmaterial.ui

import android.app.Application
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.wolly.dsbmaterial.AutoFetchWorker
import dev.wolly.dsbmaterial.BuildConfig
import dev.wolly.dsbmaterial.DSBWidget
import dev.wolly.dsbmaterial.LocalWebServer
import dev.wolly.dsbmaterial.R
import dev.wolly.dsbmaterial.api.AppUpdate
import dev.wolly.dsbmaterial.api.DevBuildInstaller
import dev.wolly.dsbmaterial.api.GitCommit
import dev.wolly.dsbmaterial.api.DSBAuthException
import dev.wolly.dsbmaterial.api.DSBMobileAPI
import dev.wolly.dsbmaterial.api.DSBNetwork
import dev.wolly.dsbmaterial.api.UpdateChannel
import dev.wolly.dsbmaterial.api.UpdateChecker
import dev.wolly.dsbmaterial.data.DataStoreManager
import dev.wolly.dsbmaterial.data.SubstitutionEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Stable
sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val entries: List<SubstitutionEntry>) : UiState()
    data class Error(val message: String) : UiState()
    object NeedsLogin : UiState()
    object NeedsSetup : UiState()
    data class SelectingClass(val classes: List<String>, val u: String, val p: String) : UiState()
    data class SetupPreview(val entries: List<SubstitutionEntry>) : UiState()
}

enum class UpdateCheckStatus { Idle, Checking, UpToDate, Available, Error }

enum class CommitStatus { Idle, Loading, Loaded, Error }

data class UpdateState(
    val status: UpdateCheckStatus = UpdateCheckStatus.Idle,
    val update: AppUpdate? = null,
    val channel: UpdateChannel = UpdateChannel.STABLE,
    val installing: Boolean = false,
    val commitStatus: CommitStatus = CommitStatus.Idle,
    val commits: List<GitCommit> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStoreManager = DataStoreManager(application)
    private val gson = Gson()

    companion object {
        private val DAY_SORT_REGEX = Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
        private val PERIOD_DIGITS_REGEX = Regex("\\d+")
        private val DAY_NAMES = listOf(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "montag", "dienstag", "mittwoch", "donnerstag", "freitag", "samstag", "sonntag"
        )
    }
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _lastUpdated = MutableStateFlow<Long?>(null)
    val lastUpdated: StateFlow<Long?> = _lastUpdated

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    val isRoomFirst: StateFlow<Boolean> = dataStoreManager.swapDataFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val dynamicColor: StateFlow<Boolean> = dataStoreManager.dynamicColorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val sortByPeriod: StateFlow<Boolean> = dataStoreManager.sortPeriodFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val themeIndex: StateFlow<Int> = dataStoreManager.themeIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val navHidden: StateFlow<Boolean> = dataStoreManager.navHiddenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val useCustomFont: StateFlow<Boolean> = dataStoreManager.useCustomFontFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val fontWeight: StateFlow<Float> = dataStoreManager.fontWeightFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 400f)

    val fontWidth: StateFlow<Float> = dataStoreManager.fontWidthFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100f)

    val fontOpsz: StateFlow<Float> = dataStoreManager.fontOpszFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14f)

    val fontSlnt: StateFlow<Float> = dataStoreManager.fontSlntFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val fontGrad: StateFlow<Float> = dataStoreManager.fontGradFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val fontRond: StateFlow<Float> = dataStoreManager.fontRondFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100f)

    val username: StateFlow<String?> = dataStoreManager.usernameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val password: StateFlow<String?> = dataStoreManager.passwordFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val autoUpdateCheck: StateFlow<Boolean> = dataStoreManager.autoUpdateCheckFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val updateChannel: StateFlow<UpdateChannel> = dataStoreManager.updateChannelFlow
        .map { UpdateChannel.fromKey(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UpdateChannel.STABLE)

    private val _archive = MutableStateFlow<List<SubstitutionEntry>>(emptyList())
    val archive: StateFlow<List<SubstitutionEntry>> = _archive

    private val _selectedClasses = MutableStateFlow<List<String>>(emptyList())
    val selectedClasses: StateFlow<List<String>> = _selectedClasses

    val autoFetchEnabled: StateFlow<Boolean> = dataStoreManager.autoFetchEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val autoFetchInterval: StateFlow<Int> = dataStoreManager.autoFetchIntervalFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 30)

    val notificationsEnabled: StateFlow<Boolean> = dataStoreManager.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _customServerUrl = MutableStateFlow<String?>(null)
    val customServerUrl: StateFlow<String?> = _customServerUrl

    private val _webServerEnabled = MutableStateFlow(false)
    val webServerEnabled: StateFlow<Boolean> = _webServerEnabled

    private val _webServerUrls = MutableStateFlow<List<String>>(emptyList())
    val webServerUrls: StateFlow<List<String>> = _webServerUrls

    private val _selectedCalendarDay = MutableStateFlow<String?>(null)
    val selectedCalendarDay: StateFlow<String?> = _selectedCalendarDay

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState

    private var lastSuccessEntries: List<SubstitutionEntry> = emptyList()
    private var lastRawEntries: List<SubstitutionEntry> = emptyList()
    private var isDemoMode = false
    private var setupInProgress = false
    private var appOpenTime = 0L
    private val minLoadingDurationMs = 1800L
    private val minUpdateLoadDurationMs = 3000L

    private suspend fun ensureLoadingFeel() {
        val elapsed = SystemClock.elapsedRealtime() - appOpenTime
        val remaining = minLoadingDurationMs - elapsed
        if (remaining > 0) delay(remaining)
    }

    private suspend fun ensureUpdateLoadFeel(start: Long) {
        val elapsed = SystemClock.elapsedRealtime() - start
        val remaining = minUpdateLoadDurationMs - elapsed
        if (remaining > 0) delay(remaining)
    }

    private fun showLoginError(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
        _uiState.value = UiState.NeedsLogin
    }

    init {
        appOpenTime = SystemClock.elapsedRealtime()
        viewModelScope.launch {
            _customServerUrl.value = dataStoreManager.customServerUrlFlow.first()
        }
        viewModelScope.launch {
            val enabled = dataStoreManager.webServerEnabledFlow.first()
            _webServerEnabled.value = enabled
            if (enabled && LocalWebServer.start(getApplication())) {
                _webServerUrls.value = LocalWebServer.urls.value
            }
        }
        viewModelScope.launch {
            LocalWebServer.urls.collect { _webServerUrls.value = it }
        }
        checkCredentialsAndFetch()
        loadArchive()
        loadSelectedClasses()
        loadCachedSnapshot()
        scheduleAutoFetchOnStartup()
        viewModelScope.launch {
            if (dataStoreManager.autoUpdateCheckFlow.first()) {
                checkForUpdates()
            }
        }
    }

    fun setUpdateChannel(channel: UpdateChannel) {
        if (channel == updateChannel.value) return
        viewModelScope.launch { dataStoreManager.saveUpdateChannel(channel.key) }
        checkForUpdates(channel)
    }

    fun checkForUpdates(channel: UpdateChannel = updateChannel.value) {
        if (_updateState.value.status == UpdateCheckStatus.Checking) return
        viewModelScope.launch {
            val start = SystemClock.elapsedRealtime()
            _updateState.value = _updateState.value.copy(status = UpdateCheckStatus.Checking, channel = channel)
            val update = when (channel) {
                UpdateChannel.DEV -> UpdateChecker.fetchDevBuild()?.let {
                    AppUpdate(
                        version = "dev",
                        name = it.name,
                        publishedAt = it.createdAt,
                        downloadUrl = it.archiveUrl
                    )
                }
                else -> UpdateChecker.checkLatest(channel)
            }
            ensureUpdateLoadFeel(start)
            _updateState.value = _updateState.value.copy(
                status = when {
                    update == null -> UpdateCheckStatus.Error
                    channel == UpdateChannel.DEV -> UpdateCheckStatus.Available
                    UpdateChecker.isUpdateAvailable(update.version) -> UpdateCheckStatus.Available
                    else -> UpdateCheckStatus.UpToDate
                },
                update = update
            )
        }
    }

    fun installDevBuild() {
        val url = _updateState.value.update?.downloadUrl ?: return
        if (_updateState.value.installing) return
        viewModelScope.launch {
            _updateState.value = _updateState.value.copy(installing = true)
            val apk = DevBuildInstaller.downloadApk(getApplication(), url)
            _updateState.value = _updateState.value.copy(installing = false)
            if (apk == null) {
                Toast.makeText(getApplication(), R.string.msg_dev_install_error, Toast.LENGTH_LONG).show()
            } else {
                DevBuildInstaller.install(getApplication(), apk)
            }
        }
    }

    fun refreshCommits() {
        if (_updateState.value.commitStatus == CommitStatus.Loading) return
        _updateState.value = _updateState.value.copy(commitStatus = CommitStatus.Loading)
        viewModelScope.launch {
            val start = SystemClock.elapsedRealtime()
            val commits = UpdateChecker.fetchCommits()
            ensureUpdateLoadFeel(start)
            _updateState.value = _updateState.value.copy(
                commitStatus = if (commits == null) CommitStatus.Error else CommitStatus.Loaded,
                commits = commits.orEmpty()
            )
        }
    }

    fun loadUpdatePage() {
        if (_updateState.value.status == UpdateCheckStatus.Idle) checkForUpdates()
        if (_updateState.value.commitStatus != CommitStatus.Loaded) refreshCommits()
    }

    private fun loadCachedSnapshot() {
        viewModelScope.launch {
            val timestamp = dataStoreManager.lastUpdatedFlow.first()
            if (timestamp > 0L) {
                _lastUpdated.value = timestamp
            }
            val cached = withContext(Dispatchers.IO) { loadCachedEntries() }
            if (cached != null && (_uiState.value == UiState.Idle || _uiState.value is UiState.Loading)) {
                ensureLoadingFeel()
                if (_uiState.value is UiState.Loading) {
                    lastSuccessEntries = cached
                    _uiState.value = UiState.Success(sortEntries(cached))
                }
            }
        }
    }

    private suspend fun loadCachedEntries(): List<SubstitutionEntry>? {
        val json = dataStoreManager.cachedEntriesFlow.first() ?: return null
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<SubstitutionEntry>>() {}.type
        val entries: List<SubstitutionEntry> = gson.fromJson(json, type)
        return entries.takeIf { it.isNotEmpty() }
    }

    private suspend fun saveCache(entries: List<SubstitutionEntry>) {
        if (entries.isEmpty()) return
        val now = System.currentTimeMillis()
        withContext(Dispatchers.IO) {
            dataStoreManager.saveCachedEntries(gson.toJson(entries))
            dataStoreManager.saveLastUpdated(now)
        }
        _lastUpdated.value = now
        _isOffline.value = false
    }

    private fun loadArchive() {
        viewModelScope.launch {
            dataStoreManager.archiveFlow.distinctUntilChanged().collect { json ->
                if (!json.isNullOrEmpty()) {
                    val entries = withContext(Dispatchers.IO) {
                        val type = object : TypeToken<List<SubstitutionEntry>>() {}.type
                        val parsed: List<SubstitutionEntry> = gson.fromJson(json, type)
                        sortArchive(parsed)
                    }
                    _archive.value = entries
                    LocalWebServer.setEntries(entries, _lastUpdated.value ?: 0L)
                }
            }
        }
    }

    private fun loadSelectedClasses() {
        viewModelScope.launch {
            dataStoreManager.selectedClassesFlow.distinctUntilChanged().collect { json ->
                if (!json.isNullOrEmpty()) {
                    _selectedClasses.value = json.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }
        }
    }

    private fun scheduleAutoFetchOnStartup() {
        viewModelScope.launch {
            val enabled = dataStoreManager.autoFetchEnabledFlow.first()
            val interval = dataStoreManager.autoFetchIntervalFlow.first()
            scheduleAutoFetch(enabled, interval)
        }
    }

    private fun parseDaySortKey(day: String): Long {
        val match = DAY_SORT_REGEX.find(day)
        if (match != null) {
            val (d, m, y) = match.destructured
            return y.toLong() * 10000 + m.toLong() * 100 + d.toLong()
        }
        val index = DAY_NAMES.indexOfFirst { day.lowercase().startsWith(it) }
        if (index >= 0) return (index % 7).toLong() + 1
        return Long.MAX_VALUE
    }

    private fun sortArchive(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        return entries.sortedWith(
            compareBy<SubstitutionEntry> { parseDaySortKey(it.day) }
                .thenBy { it.lesson.filter { c -> c.isDigit() }.toIntOrNull() ?: 999 }
        )
    }

    fun archiveSubstitutions(entries: List<SubstitutionEntry>? = null) {
        val toArchive = entries ?: lastSuccessEntries
        if (toArchive.isNotEmpty()) {
            viewModelScope.launch {
                val sortedArchive = withContext(Dispatchers.IO) {
                    val newArchive = (toArchive + _archive.value).distinctBy {
                        it.day + it.lesson + it.subject + it.room + it.art + it.text
                    }
                    val sorted = sortArchive(newArchive)
                    dataStoreManager.saveArchive(gson.toJson(sorted))
                    sorted
                }
                _archive.value = sortedArchive
                updateWidget()
            }
        }
    }

    fun removeFromArchive(entry: SubstitutionEntry) {
        viewModelScope.launch {
            val newArchive = withContext(Dispatchers.IO) {
                val filtered = _archive.value.filter { it != entry }
                dataStoreManager.saveArchive(gson.toJson(filtered))
                filtered
            }
            _archive.value = newArchive
            updateWidget()
        }
    }

    fun removeFromArchive(entries: List<SubstitutionEntry>) {
        viewModelScope.launch {
            val newArchive = withContext(Dispatchers.IO) {
                val filtered = _archive.value.filter { it !in entries }
                dataStoreManager.saveArchive(gson.toJson(filtered))
                filtered
            }
            _archive.value = newArchive
            updateWidget()
        }
    }

    fun clearArchive() {
        viewModelScope.launch {
            _archive.value = emptyList()
            withContext(Dispatchers.IO) { dataStoreManager.saveArchive("") }
            updateWidget()
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    private fun updateWidget() {
        viewModelScope.launch {
            try {
                val manager = androidx.glance.appwidget.GlanceAppWidgetManager(getApplication())
                val glanceIds = manager.getGlanceIds(DSBWidget::class.java)
                glanceIds.forEach { glanceId ->
                    DSBWidget().update(getApplication(), glanceId)
                }
            } catch (_: Exception) {}
        }
    }

    fun setThemeIndex(index: Int) {
        viewModelScope.launch {
            dataStoreManager.saveThemeIndex(index)
            LocalWebServer.setSettings(isRoomFirst.value, sortByPeriod.value, index, dynamicColor.value)
            updateWidget()
        }
    }

    fun toggleColumnOrder() {
        viewModelScope.launch {
            dataStoreManager.saveSwapPreference(!isRoomFirst.value)
            LocalWebServer.setSettings(!isRoomFirst.value, sortByPeriod.value, themeIndex.value, dynamicColor.value)
            updateWidget()
        }
    }

    fun toggleDynamicColor() {
        viewModelScope.launch {
            dataStoreManager.saveDynamicColorPreference(!dynamicColor.value)
            LocalWebServer.setSettings(isRoomFirst.value, sortByPeriod.value, themeIndex.value, !dynamicColor.value)
            updateWidget()
        }
    }

    fun toggleNavHidden() {
        viewModelScope.launch {
            dataStoreManager.saveNavHiddenPreference(!navHidden.value)
        }
    }

    fun toggleCustomFont() {
        viewModelScope.launch {
            dataStoreManager.saveCustomFont(!useCustomFont.value)
        }
    }

    fun setFontWeight(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontWeight(value) }
    }

    fun setFontWidth(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontWidth(value) }
    }

    fun setFontOpsz(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontOpsz(value) }
    }

    fun setFontSlnt(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontSlnt(value) }
    }

    fun setFontGrad(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontGrad(value) }
    }

    fun setFontRond(value: Float) {
        viewModelScope.launch { dataStoreManager.saveFontRond(value) }
    }

    fun toggleAutoFetch() {
        viewModelScope.launch {
            val newValue = !autoFetchEnabled.value
            dataStoreManager.saveAutoFetchEnabled(newValue)
            scheduleAutoFetch(newValue, autoFetchInterval.value)
        }
    }

    fun setAutoFetchInterval(minutes: Int) {
        viewModelScope.launch {
            dataStoreManager.saveAutoFetchInterval(minutes)
            if (autoFetchEnabled.value) {
                scheduleAutoFetch(true, minutes)
            }
        }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            dataStoreManager.saveNotificationsEnabled(!notificationsEnabled.value)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveNotificationsEnabled(enabled)
        }
    }

    private fun scheduleAutoFetch(enabled: Boolean, intervalMinutes: Int) {
        val workManager = WorkManager.getInstance(getApplication())
        workManager.cancelUniqueWork(AutoFetchWorker.WORK_NAME)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<AutoFetchWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniquePeriodicWork(
                AutoFetchWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    fun selectCalendarDay(day: String?) {
        _selectedCalendarDay.value = day
    }

    fun getArchiveDays(): List<String> {
        return _archive.value.groupBy { it.day }.keys.sortedBy { parseDaySortKey(it) }
    }

    fun getArchiveEntriesForDay(day: String): List<SubstitutionEntry> {
        return _archive.value.filter { it.day == day }
    }

    fun getArchiveDates(): List<Pair<String, Int>> {
        return _archive.value
            .groupBy { it.day }
            .map { (day, entries) -> day to entries.size }
            .sortedBy { parseDaySortKey(it.first) }
    }

    fun toggleSortByPeriod() {
        viewModelScope.launch {
            dataStoreManager.saveSortPreference(!sortByPeriod.value)
            LocalWebServer.setSettings(isRoomFirst.value, !sortByPeriod.value, themeIndex.value, dynamicColor.value)
            if (_uiState.value is UiState.Success) {
                _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
            }
        }
    }

    fun toggleWebServer() {
        viewModelScope.launch {
            val newValue = !_webServerEnabled.value
            val started = if (newValue) LocalWebServer.start(getApplication()) else {
                LocalWebServer.stop()
                true
            }
            _webServerEnabled.value = newValue && started
            _webServerUrls.value = LocalWebServer.urls.value
            dataStoreManager.saveWebServerEnabled(_webServerEnabled.value)
        }
    }

    fun addSelectedClass(className: String) {
        if (className.isBlank()) return
        val trimmed = className.trim()
        if (_selectedClasses.value.contains(trimmed)) return
        viewModelScope.launch {
            val updated = _selectedClasses.value + trimmed
            _selectedClasses.value = updated
            dataStoreManager.saveSelectedClasses(updated)
            fetchData()
        }
    }

    fun removeSelectedClass(className: String) {
        viewModelScope.launch {
            val updated = _selectedClasses.value.filter { it != className }
            _selectedClasses.value = updated
            dataStoreManager.saveSelectedClasses(updated)
            fetchData()
        }
    }

    fun openSettings() {
        _selectedTab.value = 3
    }

    fun closeSettings() {
        _selectedTab.value = 0
        if (lastSuccessEntries.isNotEmpty()) {
            _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
        } else {
            checkCredentialsAndFetch()
        }
    }

    fun changeClass() {
        viewModelScope.launch {
            val u = dataStoreManager.usernameFlow.first() ?: ""
            val p = dataStoreManager.passwordFlow.first() ?: ""
            if (u.isNotEmpty() && p.isNotEmpty()) {
                fetchClasses(u, p)
            } else {
                _uiState.value = UiState.NeedsLogin
            }
        }
    }

    fun cancelClassSelection() {
        viewModelScope.launch {
            val className = dataStoreManager.classNameFlow.first() ?: ""
            if (className.isEmpty()) {
                if (setupInProgress) {
                    _uiState.value = UiState.NeedsSetup
                } else {
                    _uiState.value = UiState.NeedsLogin
                }
            } else {
                openSettings()
            }
        }
    }

    fun checkCredentialsAndFetch() {
        if (isDemoMode) {
            loginDemo()
            return
        }
        viewModelScope.launch {
            val username = dataStoreManager.usernameFlow.first()
            val password = dataStoreManager.passwordFlow.first()
            val className = dataStoreManager.classNameFlow.first() ?: ""

            if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
                val completed = dataStoreManager.setupCompletedFlow.first()
                _uiState.value = if (completed) UiState.NeedsLogin else UiState.NeedsSetup
            } else if (className.isEmpty()) {
                fetchClasses(username, password)
            } else {
                fetchData(username, password, className)
            }
        }
    }

    fun login(username: String, password: String) {
        isDemoMode = false
        setupInProgress = false
        lastRawEntries = emptyList()
        DSBNetwork.resetSession()
        viewModelScope.launch {
            dataStoreManager.saveSetupCompleted(true)
            fetchClasses(username, password)
        }
    }

    fun loginFromSetup(username: String, password: String) {
        isDemoMode = false
        setupInProgress = true
        lastRawEntries = emptyList()
        DSBNetwork.resetSession()
        viewModelScope.launch {
            fetchClasses(username, password)
        }
    }

    fun skipSetup() {
        setupInProgress = false
        viewModelScope.launch {
            dataStoreManager.saveSetupCompleted(true)
            _uiState.value = UiState.NeedsLogin
        }
    }

    fun finishSetup() {
        setupInProgress = false
        viewModelScope.launch {
            dataStoreManager.saveSetupCompleted(true)
            _uiState.value = UiState.Success(sortEntries(lastSuccessEntries))
        }
    }

    fun setAutoUpdateCheck(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAutoUpdateCheck(enabled)
            if (enabled) checkForUpdates()
        }
    }

    fun loginDemo() {
        isDemoMode = true
        setupInProgress = false
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            dataStoreManager.saveSetupCompleted(true)
            delay(1000)
            val demoEntries = listOf(
                SubstitutionEntry("Montag", "Vertretung", "10a", "1 - 2", "Mathematik", "R101", "", "", "Lehrer krank", ""),
                SubstitutionEntry("Montag", "Entfall", "10a", "3", "Physik", "R102", "", "", "", ""),
                SubstitutionEntry("Dienstag", "Raumänderung", "10a", "5", "Englisch", "Turnhalle", "", "", "Wasserschaden in R105", ""),
                SubstitutionEntry("Mittwoch", "Vertretung", "10a", "4 - 5", "Geschichte", "R203", "", "", "", "")
            )
            lastSuccessEntries = demoEntries
            _uiState.value = UiState.Success(sortEntries(demoEntries))
            archiveSubstitutions(demoEntries)
        }
    }

    private suspend fun fetchClasses(u: String, p: String) {
        _uiState.value = UiState.Loading
        try {
            val api = DSBMobileAPI(u, p, resolveBaseUrl())
            val raw = api.getSubstitutions("")
            lastRawEntries = raw
            ensureLoadingFeel()
            val classes = raw.map { it.className }
                .flatMap { it.split(",").map { s -> s.trim() } }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            if (classes.isEmpty()) {
                showLoginError(getApplication<Application>().getString(R.string.msg_no_classes))
            } else {
                _uiState.value = UiState.SelectingClass(classes, u, p)
            }
        } catch (e: DSBAuthException) {
            ensureLoadingFeel()
            showLoginError(getApplication<Application>().getString(R.string.msg_login_failed))
        } catch (e: Exception) {
            ensureLoadingFeel()
            showLoginError(e.message ?: getApplication<Application>().getString(R.string.msg_unknown_error))
        }
    }

    fun selectClass(username: String, password: String, className: String) {
        viewModelScope.launch {
            dataStoreManager.saveCredentials(username, password, className)
            fetchData(username, password, className, lastRawEntries.takeIf { it.isNotEmpty() })
        }
    }

    fun selectAllClasses(username: String, password: String) {
        viewModelScope.launch {
            dataStoreManager.saveCredentials(username, password, "")
            fetchData(username, password, "", lastRawEntries.takeIf { it.isNotEmpty() })
        }
    }
    
    fun setCustomServerUrl(url: String) {
        _customServerUrl.value = url.ifBlank { null }
        viewModelScope.launch {
            dataStoreManager.saveCustomServerUrl(url)
        }
    }

    private suspend fun resolveBaseUrl(): String {
        var url = _customServerUrl.value
        if (url == null) {
            url = dataStoreManager.customServerUrlFlow.first()
            _customServerUrl.value = url
        }
        return if (url.isNullOrBlank()) "" else url.trimEnd('/')
    }

    fun logout() {
        viewModelScope.launch {
            DSBNetwork.resetSession()
            dataStoreManager.clearCredentials()
            dataStoreManager.saveCachedEntries("")
            dataStoreManager.saveLastUpdated(0L)
            _lastUpdated.value = null
            _isOffline.value = false
            lastSuccessEntries = emptyList()
            lastRawEntries = emptyList()
            _uiState.value = UiState.NeedsLogin
            _selectedTab.value = 0
            _selectedClasses.value = emptyList()
        }
    }

    fun fetchData() {
        viewModelScope.launch {
            val username = dataStoreManager.usernameFlow.first() ?: return@launch
            val password = dataStoreManager.passwordFlow.first() ?: return@launch
            val className = dataStoreManager.classNameFlow.first() ?: ""
            if (username.isEmpty() || password.isEmpty()) return@launch

            _isRefreshing.value = true
            try {
                val api = DSBMobileAPI(username, password, resolveBaseUrl())
                val allRaw = api.getSubstitutions("")

                val filtered = if (className.isEmpty() && _selectedClasses.value.isEmpty()) {
                    allRaw
                } else {
                    val allClassNames = mutableSetOf<String>()
                    if (className.isNotEmpty()) allClassNames.add(className)
                    allClassNames.addAll(_selectedClasses.value)
                    allRaw.filter { entry ->
                        allClassNames.any { cls -> entry.className.equals(cls, ignoreCase = true) }
                    }
                }

                val deduped = filtered.distinctBy { it.day + it.lesson + it.subject + it.room + it.art + it.text }
                lastSuccessEntries = deduped
                _uiState.value = UiState.Success(sortEntries(deduped))
                saveCache(deduped)
                archiveSubstitutions(deduped)
            } catch (e: DSBAuthException) {
                showLoginError(getApplication<Application>().getString(R.string.msg_login_failed))
            } catch (e: Exception) {
                fallBackToCache(e.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchData(u: String, p: String, c: String, raw: List<SubstitutionEntry>? = null) {
        if (_uiState.value !is UiState.Success) _uiState.value = UiState.Loading
        _isRefreshing.value = true
        try {
            val allRaw = raw ?: run {
                val api = DSBMobileAPI(u, p, resolveBaseUrl())
                api.getSubstitutions("")
            }

            val filtered = if (c.isEmpty() && _selectedClasses.value.isEmpty()) {
                allRaw
            } else {
                val allClassNames = mutableSetOf<String>()
                if (c.isNotEmpty()) allClassNames.add(c)
                allClassNames.addAll(_selectedClasses.value)
                allRaw.filter { entry ->
                    allClassNames.any { cls -> entry.className.equals(cls, ignoreCase = true) }
                }
            }

            val deduped = filtered.distinctBy { it.day + it.lesson + it.subject + it.room + it.art + it.text }
            saveCache(deduped)
            archiveSubstitutions(deduped)
            ensureLoadingFeel()
            lastSuccessEntries = deduped
            _uiState.value = if (setupInProgress) {
                UiState.SetupPreview(sortEntries(deduped))
            } else {
                UiState.Success(sortEntries(deduped))
            }
        } catch (e: DSBAuthException) {
            ensureLoadingFeel()
            showLoginError(getApplication<Application>().getString(R.string.msg_login_failed))
        } catch (e: Exception) {
            ensureLoadingFeel()
            fallBackToCache(e.message ?: "Unknown error")
        } finally {
            _isRefreshing.value = false
        }
    }

    private suspend fun fallBackToCache(message: String) {
        val cached = withContext(Dispatchers.IO) { loadCachedEntries() }
        if (cached != null) {
            _isOffline.value = true
            lastSuccessEntries = cached
            _uiState.value = UiState.Success(sortEntries(cached))
        } else {
            _isOffline.value = false
            _uiState.value = UiState.Error(message)
        }
    }

    private fun sortEntries(entries: List<SubstitutionEntry>): List<SubstitutionEntry> {
        val byDay = compareBy<SubstitutionEntry> { parseDaySortKey(it.day) }
        if (!sortByPeriod.value) return entries.sortedWith(byDay)
        return entries.sortedWith(
            byDay.thenBy { it.lesson.filter { c -> c.isDigit() }.toIntOrNull() ?: 999 }
        )
    }

    override fun onCleared() {
        LocalWebServer.stop()
        super.onCleared()
    }
}
