package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.GameRomEntity
import com.example.data.db.StandaloneProfileEntity
import com.example.data.db.SystemEntity
import com.example.data.launcher.IntentLauncher
import com.example.data.launcher.LaunchResult
import com.example.data.model.DisplaySettings
import com.example.data.repository.LauncherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    val repository = LauncherRepository(application)
    private val intentLauncher = IntentLauncher(application)

    val systems: StateFlow<List<SystemEntity>> = repository.allSystems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val standaloneProfiles: StateFlow<List<StandaloneProfileEntity>> = repository.allStandaloneProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteRoms: StateFlow<List<GameRomEntity>> = repository.favoriteRoms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedRoms: StateFlow<List<GameRomEntity>> = repository.recentlyPlayedRoms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoms: StateFlow<List<GameRomEntity>> = repository.allRoms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSystemId = MutableStateFlow<String?>("android_apps")
    val selectedSystemId: StateFlow<String?> = _selectedSystemId.asStateFlow()

    private val _romListSettings = MutableStateFlow(com.example.data.model.RomListSettings())
    val romListSettings: StateFlow<com.example.data.model.RomListSettings> = _romListSettings.asStateFlow()

    private val _gamepadSettings = MutableStateFlow(com.example.data.model.GamepadSettings())
    val gamepadSettings: StateFlow<com.example.data.model.GamepadSettings> = _gamepadSettings.asStateFlow()

    private val _bottomBarSettings = MutableStateFlow(com.example.data.model.BottomBarSettings())
    val bottomBarSettings: StateFlow<com.example.data.model.BottomBarSettings> = _bottomBarSettings.asStateFlow()

    private val _hiddenAndroidApps = MutableStateFlow<Set<String>>(emptySet())
    val hiddenAndroidApps: StateFlow<Set<String>> = _hiddenAndroidApps.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _displaySettings = MutableStateFlow(DisplaySettings())
    val displaySettings: StateFlow<DisplaySettings> = _displaySettings.asStateFlow()

    private val _customIcons = MutableStateFlow<Map<String, String>>(emptyMap())
    val customIcons: StateFlow<Map<String, String>> = _customIcons.asStateFlow()

    private val _launchEvent = MutableSharedFlow<LaunchResult>()
    val launchEvent: SharedFlow<LaunchResult> = _launchEvent.asSharedFlow()

    val currentSystemRoms: StateFlow<List<GameRomEntity>> = combine(
        repository.allRoms,
        _selectedSystemId,
        _searchQuery,
        _displaySettings
    ) { allRoms, systemId, query, displaySettings ->
        var filtered = when (systemId) {
            "favorites" -> allRoms.filter { it.isFavorite }
            "recently_played" -> allRoms
                .filter { it.lastPlayedTimestamp > 0 }
                .sortedByDescending { it.lastPlayedTimestamp }
                .take(displaySettings.maxRecentCount)
            null -> allRoms
            else -> allRoms.filter { it.systemId == systemId }
        }
        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.fileName.contains(query, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
            val savedSystemId = repository.configStorageManager.loadActiveSystemId()
            if (savedSystemId != null) {
                _selectedSystemId.value = savedSystemId
            }
            _displaySettings.value = repository.loadDisplaySettings()
            _romListSettings.value = repository.loadRomListSettings()
            _gamepadSettings.value = repository.loadGamepadSettings()
            _bottomBarSettings.value = repository.loadBottomBarSettings()
            _hiddenAndroidApps.value = repository.getHiddenAndroidApps()
            _customIcons.value = repository.loadCustomIcons()
        }
    }

    fun saveCustomIcon(game: GameRomEntity, iconPath: String) {
        viewModelScope.launch {
            repository.saveCustomIcon(game.filePath, iconPath)
            _customIcons.value = repository.loadCustomIcons()
        }
    }

    fun updateRomListSettings(settings: com.example.data.model.RomListSettings) {
        _romListSettings.value = settings
        viewModelScope.launch {
            repository.saveRomListSettings(settings)
        }
    }

    fun updateGamepadSettings(settings: com.example.data.model.GamepadSettings) {
        _gamepadSettings.value = settings
        viewModelScope.launch {
            repository.saveGamepadSettings(settings)
        }
    }

    fun updateBottomBarSettings(settings: com.example.data.model.BottomBarSettings) {
        _bottomBarSettings.value = settings
        viewModelScope.launch {
            repository.saveBottomBarSettings(settings)
        }
    }

    fun saveHiddenAndroidApps(hiddenSet: Set<String>) {
        viewModelScope.launch {
            repository.saveHiddenAndroidApps(hiddenSet)
            _hiddenAndroidApps.value = hiddenSet
        }
    }

    fun getVisibleAndroidAppsXml(systemId: String): Set<String>? {
        return repository.getVisibleAndroidAppsXml(systemId)
    }

    fun saveVisibleAndroidApps(systemId: String, visibleSet: Set<String>) {
        viewModelScope.launch {
            repository.saveVisibleAndroidApps(systemId, visibleSet)
        }
    }

    fun removeFromRecent(game: GameRomEntity) {
        viewModelScope.launch {
            repository.removeRomFromRecent(game)
        }
    }

    fun selectSystem(systemId: String?) {
        _selectedSystemId.value = systemId
        if (systemId != null) {
            saveActiveSystemId(systemId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun launchGame(system: SystemEntity, game: GameRomEntity) {
        viewModelScope.launch {
            repository.updateRomPlayed(game)
            val targetSystem = if (system.id == "favorites" || system.id == "recently_played") {
                systems.value.firstOrNull { it.id == game.systemId } ?: system
            } else {
                system
            }
            val profile = standaloneProfiles.value.firstOrNull { it.id == targetSystem.customXmlProfileId }
            val result = intentLauncher.launchGame(targetSystem, game, profile)
            _launchEvent.emit(result)
        }
    }

    fun saveSystem(system: SystemEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateSystem(system)
        }
    }

    fun reorderSystems(updatedSystems: List<SystemEntity>) {
        viewModelScope.launch {
            repository.updateSystemsList(updatedSystems)
        }
    }

    fun deleteSystem(system: SystemEntity) {
        viewModelScope.launch {
            repository.deleteSystem(system)
            if (_selectedSystemId.value == system.id) {
                _selectedSystemId.value = systems.value.firstOrNull { it.id != system.id }?.id
            }
        }
    }

    fun saveStandaloneProfile(profile: StandaloneProfileEntity) {
        viewModelScope.launch {
            repository.saveStandaloneProfile(profile)
        }
    }

    fun deleteStandaloneProfile(profile: StandaloneProfileEntity) {
        viewModelScope.launch {
            repository.deleteStandaloneProfile(profile)
        }
    }

    fun toggleFavorite(game: GameRomEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(game)
        }
    }

    fun renameGame(game: GameRomEntity, newName: String) {
        viewModelScope.launch {
            repository.renameGame(game, newName)
        }
    }

    fun saveActiveSystemId(systemId: String) {
        repository.configStorageManager.saveActiveSystemId(systemId)
    }

    fun loadActiveSystemId(): String? {
        return repository.configStorageManager.loadActiveSystemId()
    }

    fun rescanCurrentSystemRoms(system: SystemEntity) {
        viewModelScope.launch {
            repository.scanFolderForRoms(system)
        }
    }

    fun updateDisplaySettings(newSettings: DisplaySettings) {
        _displaySettings.value = newSettings
        viewModelScope.launch {
            repository.saveDisplaySettings(newSettings)
        }
    }

    fun rescanRoms() {
        viewModelScope.launch {
            repository.scanAllSystemFolders()
        }
    }
}
