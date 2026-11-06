package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.DriverProfile
import com.example.data.models.FuelStation
import com.example.data.models.FuelStockStatus
import com.example.data.models.FuelType
import com.example.data.models.QueueTicket
import com.example.data.models.StationQueueState
import com.example.data.models.TicketStatus
import com.example.data.models.VehicleCategory
import com.example.data.repository.FoliishoRepository
import com.example.localization.AmharicStrings
import com.example.localization.AppLanguage
import com.example.localization.EnglishStrings
import com.example.localization.StringsDefinition
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoliishoAlert(
    val title: String,
    val message: String,
    val isUrgent: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class FoliishoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FoliishoRepository(application)
    private val auth = Firebase.auth

    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser = _currentUser.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.AMHARIC)
    val language = _language.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFuelFilter = MutableStateFlow<FuelType?>(null)
    val selectedFuelFilter = _selectedFuelFilter.asStateFlow()

    private val _activeAlert = MutableStateFlow<FoliishoAlert?>(null)
    val activeAlert = _activeAlert.asStateFlow()

    private val _clockOffsetMinutes = MutableStateFlow(0)
    val clockOffsetMinutes = _clockOffsetMinutes.asStateFlow()

    // Hawassa City Center coordinates: 7.0504° N, 38.4716° E
    private val hawassaCenterLat = 7.0504
    private val hawassaCenterLng = 38.4716

    private val _stations = MutableStateFlow<List<FuelStation>>(FoliishoRepository.defaultHawassaStations)
    val stations: StateFlow<List<FuelStation>> = _stations.asStateFlow()

    private val _queueStates = MutableStateFlow<Map<String, StationQueueState>>(emptyMap())
    val queueStates: StateFlow<Map<String, StationQueueState>> = _queueStates.asStateFlow()

    private val _driverProfile = MutableStateFlow<DriverProfile?>(null)
    val driverProfile = _driverProfile.asStateFlow()

    private val _activeTicket = MutableStateFlow<QueueTicket?>(null)
    val activeTicket = _activeTicket.asStateFlow()

    private val _ticketHistory = MutableStateFlow<List<QueueTicket>>(emptyList())
    val ticketHistory = _ticketHistory.asStateFlow()

    private var previousTicketStatus: String? = null

    private var stationJob: Job? = null
    private var queueJob: Job? = null
    private var profileJob: Job? = null
    private var activeTicketJob: Job? = null
    private var historyJob: Job? = null

    init {
        // Immediately start public stations and queue observers on startup
        startObservingStationsAndQueues()

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            _currentUser.value = user
            if (user != null) {
                bindUserData(user.uid)
            } else {
                profileJob?.cancel()
                activeTicketJob?.cancel()
                historyJob?.cancel()
                _driverProfile.value = null
                _activeTicket.value = null
                _ticketHistory.value = emptyList()
            }
        }
    }

    private fun startObservingStationsAndQueues() {
        viewModelScope.launch {
            try {
                repository.seedHawassaStationsIfEmpty()
            } catch (_: Throwable) {}
        }

        if (stationJob == null || stationJob?.isActive == false) {
            stationJob = viewModelScope.launch {
                try {
                    repository.observeStations()
                        .catch { emit(FoliishoRepository.defaultHawassaStations) }
                        .collect { list ->
                            _stations.value = if (list.isNotEmpty()) list else FoliishoRepository.defaultHawassaStations
                        }
                } catch (_: Throwable) {
                    _stations.value = FoliishoRepository.defaultHawassaStations
                }
            }
        }

        if (queueJob == null || queueJob?.isActive == false) {
            queueJob = viewModelScope.launch {
                try {
                    repository.observeQueueStates()
                        .catch { emit(emptyMap()) }
                        .collect { map ->
                            _queueStates.value = map
                        }
                } catch (_: Throwable) {}
            }
        }
    }

    fun onAuthChanged() {
        val user = auth.currentUser
        _currentUser.value = user
        startObservingStationsAndQueues()
        if (user != null) {
            bindUserData(user.uid)
        }
    }

    private fun bindUserData(uid: String) {
        startObservingStationsAndQueues()

        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            try {
                repository.observeDriverProfile(uid)
                    .catch { emit(null) }
                    .collect { profile ->
                        _driverProfile.value = profile
                        if (profile != null) {
                            _language.value = if (profile.language == "en") AppLanguage.ENGLISH else AppLanguage.AMHARIC
                        }
                    }
            } catch (_: Throwable) {}
        }

        activeTicketJob?.cancel()
        activeTicketJob = viewModelScope.launch {
            try {
                repository.observeActiveTicket(uid)
                    .catch { emit(null) }
                    .collect { ticket ->
                        _activeTicket.value = ticket
                        try {
                            handleTicketStatusTransition(ticket)
                        } catch (_: Throwable) {}
                    }
            } catch (_: Throwable) {}
        }

        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            try {
                repository.observeTicketHistory(uid)
                    .catch { emit(emptyList()) }
                    .collect { history ->
                        _ticketHistory.value = history
                    }
            } catch (_: Throwable) {}
        }
    }

    private fun handleTicketStatusTransition(ticket: QueueTicket?) {
        val currentStatus = ticket?.status
        val strings = if (_language.value == AppLanguage.AMHARIC) AmharicStrings else EnglishStrings

        if (currentStatus != null && currentStatus != previousTicketStatus) {
            when (currentStatus) {
                TicketStatus.WAITING.name -> {
                    // Check if wait time is ~10 mins
                    val adjustedWait = maxOf(0L, ticket.estimatedWaitMinutes - _clockOffsetMinutes.value.toLong())
                    if (adjustedWait <= 10L && adjustedWait > 0L) {
                        triggerVibration(isUrgent = false)
                        _activeAlert.value = FoliishoAlert(
                            title = strings.notificationTitleStage2,
                            message = strings.notificationBodyStage2,
                            isUrgent = false
                        )
                    }
                }
                TicketStatus.APPROACHING.name -> {
                    triggerVibration(isUrgent = false)
                    _activeAlert.value = FoliishoAlert(
                        title = strings.notificationTitleStage2,
                        message = strings.notificationBodyStage2,
                        isUrgent = false
                    )
                }
                TicketStatus.CALLED.name -> {
                    triggerVibration(isUrgent = true)
                    _activeAlert.value = FoliishoAlert(
                        title = strings.notificationTitleStage3,
                        message = strings.notificationBodyStage3,
                        isUrgent = true
                    )
                }
                TicketStatus.SERVING.name -> {
                    triggerVibration(isUrgent = false)
                    _activeAlert.value = FoliishoAlert(
                        title = if (_language.value == AppLanguage.AMHARIC) "አገልግሎት ጀምሯል" else "Service Started",
                        message = if (_language.value == AppLanguage.AMHARIC) "በፓምፑ እየተስተናገዱ ነው።" else "Your vehicle is currently being served at the pump.",
                        isUrgent = false
                    )
                }
            }
        }
        previousTicketStatus = currentStatus
    }

    private fun triggerVibration(isUrgent: Boolean) {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = if (isUrgent) longArrayOf(0, 300, 150, 300, 150, 500) else longArrayOf(0, 200, 100, 200)
                    val amplitudes = if (isUrgent) intArrayOf(0, 255, 0, 255, 0, 255) else intArrayOf(0, 180, 0, 180)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(if (isUrgent) 600L else 300L)
                }
            }
        } catch (_: Throwable) {}
    }

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        val profile = _driverProfile.value
        if (profile != null) {
            viewModelScope.launch {
                repository.saveDriverProfile(profile.copy(language = lang.code))
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFuelFilter(filter: FuelType?) {
        _selectedFuelFilter.value = filter
    }

    fun dismissAlert() {
        _activeAlert.value = null
    }

    fun fastForwardClock(additionalMinutes: Int) {
        _clockOffsetMinutes.value += additionalMinutes
        val currentTicket = _activeTicket.value
        if (currentTicket != null) {
            val strings = if (_language.value == AppLanguage.AMHARIC) AmharicStrings else EnglishStrings
            val adjusted = maxOf(0L, currentTicket.estimatedWaitMinutes - _clockOffsetMinutes.value.toLong())
            if (adjusted <= 10L) {
                triggerVibration(isUrgent = false)
                _activeAlert.value = FoliishoAlert(
                    title = strings.notificationTitleStage2,
                    message = strings.notificationBodyStage2,
                    isUrgent = false
                )
            }
        }
    }

    fun triggerTestAlert() {
        val strings = if (_language.value == AppLanguage.AMHARIC) AmharicStrings else EnglishStrings
        triggerVibration(isUrgent = true)
        _activeAlert.value = FoliishoAlert(
            title = strings.notificationTitleStage2,
            message = strings.notificationBodyStage2,
            isUrgent = false
        )
    }

    fun bookQueueTicket(
        stationId: String,
        fuelType: FuelType,
        onSuccess: (QueueTicket) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val profile = _driverProfile.value ?: DriverProfile(
                    fullName = "Hawassa Driver",
                    phoneNumber = "0911223344",
                    vehiclePlate = "3-A12345 SD",
                    vehicleCategory = VehicleCategory.BAJAJ.name
                )
                val ticket = repository.bookQueueTicket(
                    stationId = stationId,
                    driverName = profile.fullName.ifBlank { "Hawassa Driver" },
                    driverPhone = profile.phoneNumber.ifBlank { "0911223344" },
                    vehiclePlate = profile.vehiclePlate.ifBlank { "3-A12345 SD" },
                    vehicleCategory = profile.vehicleCategory,
                    fuelType = fuelType
                )
                val strings = if (_language.value == AppLanguage.AMHARIC) AmharicStrings else EnglishStrings
                _activeAlert.value = FoliishoAlert(
                    title = strings.notificationTitleStage1,
                    message = "${strings.notificationBodyStage1} (${ticket.ticketId})",
                    isUrgent = false
                )
                onSuccess(ticket)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Booking failed")
            }
        }
    }

    fun cancelTicket(ticket: QueueTicket, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.cancelTicket(ticket)
                onComplete()
            } catch (_: Exception) {}
        }
    }

    fun saveProfile(
        name: String,
        phone: String,
        plate: String,
        category: String,
        fuel: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val uid = repository.currentUserId() ?: return@launch
            val updated = DriverProfile(
                userId = uid,
                fullName = name,
                phoneNumber = phone,
                vehiclePlate = plate,
                vehicleCategory = category,
                fuelPreference = fuel,
                language = _language.value.code
            )
            repository.saveDriverProfile(updated)
            onSuccess()
        }
    }

    // Calculate approximate distance from Hawassa reference point
    fun calculateDistanceKm(stationLat: Double, stationLng: Double): Double {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(hawassaCenterLat, hawassaCenterLng, stationLat, stationLng, results)
            (results[0] / 1000.0 * 10).toInt() / 10.0
        } catch (_: Exception) {
            1.5
        }
    }

    // Check if vehicle is within geofence perimeter (e.g. within 350 meters of station)
    fun isWithinPerimeter(stationLat: Double, stationLng: Double): Boolean {
        return try {
            val results = FloatArray(1)
            Location.distanceBetween(hawassaCenterLat, hawassaCenterLng, stationLat, stationLng, results)
            results[0] <= 450.0
        } catch (_: Exception) {
            true
        }
    }

    // Simulation tool for user testing: Call Next Ticket from Manager perspective
    fun simCallNextTicket(stationId: String) {
        viewModelScope.launch {
            repository.managerCallNextTicket(stationId)
        }
    }

    // Simulation tool for user testing: Complete Service from Manager perspective
    fun simCompleteService(stationId: String, ticketId: String) {
        viewModelScope.launch {
            repository.managerCompleteService(stationId, ticketId)
        }
    }

    // Simulation tool: Toggle fuel availability
    fun simToggleStock(stationId: String, benzene: FuelStockStatus, diesel: FuelStockStatus) {
        viewModelScope.launch {
            repository.updateStationStock(stationId, benzene, diesel)
        }
    }
}
