package com.pathword.taxi.presentation.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.pathword.taxi.domain.model.Bid
import com.pathword.taxi.domain.model.GpsUpdate
import com.pathword.taxi.domain.model.Location
import com.pathword.taxi.domain.model.Order
import com.pathword.taxi.domain.repository.ITaxiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverState(
    val isOnline: Boolean = false,
    val currentLocation: Location = Location(43.238949, 76.889709), // Almaty center approx
    val incomingOrders: List<Order> = emptyList(),
    val activeRide: Bid? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val repository: ITaxiRepository,
    private val sharedPreferences: SharedPreferences,
    private val gson: Gson
) : ViewModel() {

    private val driverId = 2L // Mock driver ID

    private val _state = MutableStateFlow(DriverState())
    val state: StateFlow<DriverState> = _state.asStateFlow()

    private var gpsJob: Job? = null

    init {
        restoreState()

        viewModelScope.launch {
            repository.incomingOrders.collect { order ->
                _state.value = _state.value.copy(incomingOrders = _state.value.incomingOrders + order)
            }
        }
        viewModelScope.launch {
            repository.rideStarted.collect { bid ->
                if (bid.driverId == driverId) {
                    saveActiveRide(bid)
                    _state.value = _state.value.copy(activeRide = bid, incomingOrders = emptyList())
                }
            }
        }
        viewModelScope.launch {
            repository.errorMessages.collect { msg ->
                _state.value = _state.value.copy(errorMessage = msg)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun restoreState() {
        val savedBidJson = sharedPreferences.getString("active_ride_bid", null)
        if (savedBidJson != null) {
            try {
                val bid = gson.fromJson(savedBidJson, Bid::class.java)
                _state.value = _state.value.copy(activeRide = bid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveActiveRide(bid: Bid?) {
        if (bid == null) {
            sharedPreferences.edit().remove("active_ride_bid").apply()
        } else {
            val json = gson.toJson(bid)
            sharedPreferences.edit().putString("active_ride_bid", json).apply()
        }
    }

    fun toggleOnline() {
        val newState = !_state.value.isOnline
        _state.value = _state.value.copy(isOnline = newState)
        if (newState) {
            repository.connect(driverId)
            startGpsUpdates()
        } else {
            stopGpsUpdates()
            repository.disconnect()
        }
    }

    private fun startGpsUpdates() {
        gpsJob?.cancel()
        gpsJob = viewModelScope.launch {
            while (true) {
                val curLoc = _state.value.currentLocation
                val newLoc = Location(curLoc.lat + 0.001, curLoc.lng + 0.001)
                _state.value = _state.value.copy(currentLocation = newLoc)

                repository.sendGpsUpdate(GpsUpdate(driverId, newLoc))
                delay(3000)
            }
        }
    }

    private fun stopGpsUpdates() {
        gpsJob?.cancel()
        gpsJob = null
    }

    private var lastBidTime: Long = 0

    fun sendBid(orderId: String, price: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBidTime < 500) {
            return // Throttle clicks
        }
        lastBidTime = currentTime
        repository.sendBid(Bid(orderId, driverId, price))
    }

    override fun onCleared() {
        super.onCleared()
        stopGpsUpdates()
        repository.disconnect()
    }
}
