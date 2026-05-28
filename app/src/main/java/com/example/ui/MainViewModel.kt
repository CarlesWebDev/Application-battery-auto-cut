package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChargingSession
import com.example.data.EcoChargeRepository
import com.example.util.WebhookSender
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BatteryState(
    val level: Int = 50,
    val maxLevel: Int = 100,
    val isCharging: Boolean = false,
    val voltage: Int = 3980, // mV
    val temperature: Double = 28.5, // °C
    val pluggedType: String = "Baterai",
    val health: String = "Baik",
    val estimatedWattage: Double = 0.0
)

sealed interface UIEvent {
    data class TriggerAlarm(val level: Int, val isTtsEnabled: Boolean) : UIEvent
    data class ShowToast(val message: String) : UIEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val db = AppDatabase.getDatabase(application)
    private val repository = EcoChargeRepository(db)

    // Current battery status
    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    // Config states
    private val _autocutLimit = MutableStateFlow(80)
    val autocutLimit: StateFlow<Int> = _autocutLimit.asStateFlow()

    private val _isAlarmEnabled = MutableStateFlow(true)
    val isAlarmEnabled: StateFlow<Boolean> = _isAlarmEnabled.asStateFlow()

    private val _isTtsEnabled = MutableStateFlow(true)
    val isTtsEnabled: StateFlow<Boolean> = _isTtsEnabled.asStateFlow()

    private val _isWebhookEnabled = MutableStateFlow(false)
    val isWebhookEnabled: StateFlow<Boolean> = _isWebhookEnabled.asStateFlow()

    private val _webhookUrl = MutableStateFlow("")
    val webhookUrl: StateFlow<String> = _webhookUrl.asStateFlow()

    private val _webhookMethod = MutableStateFlow("GET")
    val webhookMethod: StateFlow<String> = _webhookMethod.asStateFlow()

    private val _webhookBody = MutableStateFlow("{}")
    val webhookBody: StateFlow<String> = _webhookBody.asStateFlow()

    // Interactive Autocut Simulation State
    private val _isSimulationActive = MutableStateFlow(false)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    // Triggers and event states
    private val _autocutTriggered = MutableStateFlow(false)
    val autocutTriggered: StateFlow<Boolean> = _autocutTriggered.asStateFlow()

    private val _webhookLogs = MutableStateFlow<String>("")
    val webhookLogs: StateFlow<String> = _webhookLogs.asStateFlow()

    // Event Flow for Activity actions (sound execution, toast etc)
    private val _uiEvents = MutableSharedFlow<UIEvent>()
    val uiEvents: SharedFlow<UIEvent> = _uiEvents.asSharedFlow()

    // Charging session details Tracker
    private var currentSessionId: Long? = null
    private var maxTempRecordedInSession: Double = 0.0
    private var chargeStartLevel: Int = 0

    // Database session histories
    val chargingHistory: StateFlow<List<ChargingSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var simulationJob: Job? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _autocutLimit.value = repository.getIntSetting("KEY_AUTOCUT_LIMIT", 80)
            _isAlarmEnabled.value = repository.getBooleanSetting("KEY_ALARM_ENABLED", true)
            _isTtsEnabled.value = repository.getBooleanSetting("KEY_TTS_ENABLED", true)
            _isWebhookEnabled.value = repository.getBooleanSetting("KEY_WEBHOOK_ENABLED", false)
            _webhookUrl.value = repository.getStringSetting("KEY_WEBHOOK_URL", "")
            _webhookMethod.value = repository.getStringSetting("KEY_WEBHOOK_METHOD", "GET")
            _webhookBody.value = repository.getStringSetting("KEY_WEBHOOK_BODY", "{}")
        }
    }

    // Handles real physical battery broadcast events
    fun updateBatteryStatus(
        level: Int,
        voltage: Int,
        temperature: Double,
        isCharging: Boolean,
        pluggedType: String,
        health: String
    ) {
        // If simulation mode is active, do not let real hardware changes override
        if (_isSimulationActive.value) return

        val state = _batteryState.value
        val calcWattage = if (isCharging) {
            val estimatedAmp = when (pluggedType) {
                "AC" -> 2.5 // Quick charge speed (e.g. 10W - 18W)
                "USB" -> 0.5 // Slow laptop speed (e.g. 2.5W)
                "Wireless" -> 1.0 // Mid wireless charge
                else -> 0.0
            }
            (voltage / 1000.0) * estimatedAmp
        } else {
            0.0
        }

        _batteryState.value = BatteryState(
            level = level,
            maxLevel = 100,
            isCharging = isCharging,
            voltage = voltage,
            temperature = temperature,
            pluggedType = pluggedType,
            health = health,
            estimatedWattage = calcWattage
        )

        handleChargingLifecycle(isCharging, level, temperature)
    }

    private fun handleChargingLifecycle(isCharging: Boolean, currentLevel: Int, currentTemp: Double) {
        viewModelScope.launch {
            if (isCharging) {
                // Keep track of maximum temp in this session
                if (currentTemp > maxTempRecordedInSession) {
                    maxTempRecordedInSession = currentTemp
                }

                if (currentSessionId == null) {
                    // Charging started! Save initial session
                    chargeStartLevel = currentLevel
                    maxTempRecordedInSession = currentTemp
                    val newSession = ChargingSession(
                        startBatteryLevel = currentLevel,
                        endBatteryLevel = currentLevel,
                        peakTemperature = currentTemp,
                        isAutocutTriggered = false
                    )
                    currentSessionId = repository.insertSession(newSession)
                } else {
                    // Update latest session with final levels
                    val latest = repository.getLatestSession()
                    if (latest != null && latest.id.toLong() == currentSessionId) {
                        val updated = latest.copy(
                            endBatteryLevel = currentLevel,
                            peakTemperature = maxTempRecordedInSession
                        )
                        repository.insertSession(updated)
                    }
                }

                // Check Autocut Trigger
                val targetLimit = _autocutLimit.value
                if (currentLevel >= targetLimit && !_autocutTriggered.value) {
                    executeAutocutTrigger(targetLimit)
                }
            } else {
                // Charging stopped / Disconnected.
                if (currentSessionId != null) {
                    val latest = repository.getLatestSession()
                    if (latest != null && latest.id.toLong() == currentSessionId) {
                        // Calculate simulated energy saved
                        // Saving energy of trickle overcharging preventions
                        val wasAutocut = _autocutTriggered.value
                        val calcPowerSaved = if (wasAutocut) {
                            // ~0.08Wh per minute saved by cutting power immediately instead of hours of 100% hover
                            ((currentLevel - chargeStartLevel) * 0.15) + 0.4
                        } else {
                            0.0
                        }

                        val finalSession = latest.copy(
                            endTime = System.currentTimeMillis(),
                            endBatteryLevel = currentLevel,
                            isAutocutTriggered = wasAutocut,
                            peakTemperature = maxTempRecordedInSession,
                            energySavedWh = calcPowerSaved
                        )
                        repository.insertSession(finalSession)
                    }
                    currentSessionId = null
                }
                // Reset triggered status for the next plug-in
                _autocutTriggered.value = false
            }
        }
    }

    private suspend fun executeAutocutTrigger(limit: Int) {
        _autocutTriggered.value = true
        
        // Push UI events for alarm / TTS sound
        if (_isAlarmEnabled.value) {
            _uiEvents.emit(UIEvent.TriggerAlarm(limit, _isTtsEnabled.value))
        }

        // Webhook trigger if enabled
        if (_isWebhookEnabled.value && _webhookUrl.value.isNotBlank()) {
            _webhookLogs.value = "Menghubungi plug pintar: Mengirim sinyal cut-off..."
            viewModelScope.launch {
                val res = WebhookSender.sendWebhook(
                    url = _webhookUrl.value,
                    method = _webhookMethod.value,
                    body = _webhookBody.value
                )
                res.onSuccess {
                    _webhookLogs.value = "Webhook Terkirim! Sinyal pemutus menyala fisik (Smart Plug Terputus)."
                    _uiEvents.emit(UIEvent.ShowToast("Smart Plug: Autocut Sukses!"))
                }.onFailure {
                    _webhookLogs.value = "Webhook Gagal: ${it.localizedMessage}"
                    _uiEvents.emit(UIEvent.ShowToast("Smart Plug: Koneksi gagal!"))
                }
            }
        }
    }

    // Toggle Simulation Mode
    fun setSimulationActive(active: Boolean) {
        _isSimulationActive.value = active
        if (active) {
            // Start charging simulation from current battery level or 75%
            val startLevel = if (_batteryState.value.level >= 100) 74 else _batteryState.value.level
            _batteryState.value = BatteryState(
                level = startLevel,
                isCharging = true,
                voltage = 3980,
                temperature = 29.8,
                pluggedType = "Simulasi (AC)",
                health = "Baik (Sehat)",
                estimatedWattage = 18.5
            )
            _autocutTriggered.value = false
            handleChargingLifecycle(isCharging = true, currentLevel = startLevel, currentTemp = 29.8)

            simulationJob?.cancel()
            simulationJob = viewModelScope.launch {
                while (_isSimulationActive.value) {
                    delay(3000) // Increase 1% every 3 seconds for fast-paced interactive review
                    val current = _batteryState.value
                    if (current.level < 100) {
                        val nextLevel = current.level + 1
                        val nextTemp = current.temperature + 0.1
                        val nextVoltage = current.voltage + 10
                        _batteryState.value = current.copy(
                            level = nextLevel,
                            temperature = nextTemp,
                            voltage = nextVoltage
                        )
                        handleChargingLifecycle(isCharging = true, currentLevel = nextLevel, currentTemp = nextTemp)
                    } else {
                        // At 100%, disconnect simulation
                        delay(2000)
                        setSimulationActive(false)
                        break
                    }
                }
            }
        } else {
            simulationJob?.cancel()
            val state = _batteryState.value
            _batteryState.value = state.copy(isCharging = false, pluggedType = "Baterai")
            handleChargingLifecycle(isCharging = false, currentLevel = state.level, currentTemp = state.temperature)
        }
    }

    fun triggerTestWebhook() {
        if (_webhookUrl.value.isBlank()) {
            _webhookLogs.value = "Gagal: URL Kosong! Silakan isi URL target terlebih dahulu."
            return
        }
        _webhookLogs.value = "Mengirim tes webhook ke ${_webhookUrl.value}..."
        viewModelScope.launch {
            val res = WebhookSender.sendWebhook(
                url = _webhookUrl.value,
                method = _webhookMethod.value,
                body = _webhookBody.value
            )
            res.onSuccess {
                _webhookLogs.value = "Tes Sukses: Sinyal Webhook terkirim sempurna."
            }.onFailure {
                _webhookLogs.value = "Tes Gagal: ${it.localizedMessage}"
            }
        }
    }

    // User inputs settings update
    fun setAutocutLimit(limit: Int) {
        _autocutLimit.value = limit
        viewModelScope.launch { repository.setIntSetting("KEY_AUTOCUT_LIMIT", limit) }
    }

    fun toggleAlarmSetting(enabled: Boolean) {
        _isAlarmEnabled.value = enabled
        viewModelScope.launch { repository.setBooleanSetting("KEY_ALARM_ENABLED", enabled) }
    }

    fun toggleTtsSetting(enabled: Boolean) {
        _isTtsEnabled.value = enabled
        viewModelScope.launch { repository.setBooleanSetting("KEY_TTS_ENABLED", enabled) }
    }

    fun toggleWebhookSetting(enabled: Boolean) {
        _isWebhookEnabled.value = enabled
        viewModelScope.launch { repository.setBooleanSetting("KEY_WEBHOOK_ENABLED", enabled) }
    }

    fun updateWebhookUrl(url: String) {
        _webhookUrl.value = url
        viewModelScope.launch { repository.setStringSetting("KEY_WEBHOOK_URL", url) }
    }

    fun updateWebhookMethod(method: String) {
        _webhookMethod.value = method
        viewModelScope.launch { repository.setStringSetting("KEY_WEBHOOK_METHOD", method) }
    }

    fun updateWebhookBody(body: String) {
        _webhookBody.value = body
        viewModelScope.launch { repository.setStringSetting("KEY_WEBHOOK_BODY", body) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearSessions()
            _uiEvents.emit(UIEvent.ShowToast("Riwayat dibersihkan!"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
