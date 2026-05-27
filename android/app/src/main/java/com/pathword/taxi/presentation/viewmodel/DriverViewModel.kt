package com.pathword.taxi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val activeRide: Bid? = null
)

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val repository: ITaxiRepository
) : ViewModel() {

    private val driverId = 2L // Mock driver ID

    private val _state = MutableStateFlow(DriverState())
    val state: StateFlow<DriverState> = _state.asStateFlow()

    private var gpsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.incomingOrders.collect { order ->
                _state.value = _state.value.copy(incomingOrders = _state.value.incomingOrders + order)
            }
        }
        viewModelScope.launch {
            repository.rideStarted.collect { bid ->
                if (bid.driverId == driverId) {
                    _state.value = _state.value.copy(activeRide = bid, incomingOrders = emptyList())
                }
            }
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

    fun sendBid(orderId: String, price: String) {
        repository.sendBid(Bid(orderId, driverId, price))
    }

    override fun onCleared() {
        super.onCleared()
        stopGpsUpdates()
        repository.disconnect()
    }
}
