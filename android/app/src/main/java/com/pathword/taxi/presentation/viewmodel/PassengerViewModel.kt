package com.pathword.taxi.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pathword.taxi.domain.model.Bid
import com.pathword.taxi.domain.model.Location
import com.pathword.taxi.domain.model.OrderRequest
import com.pathword.taxi.domain.repository.ITaxiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PassengerState(
    val isConnected: Boolean = false,
    val destination: Location? = null,
    val suggestedPrice: String = "",
    val activeOrderId: String? = null,
    val bids: List<Bid> = emptyList(),
    val rideStarted: Boolean = false,
    val acceptedBid: Bid? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PassengerViewModel @Inject constructor(
    private val repository: ITaxiRepository
) : ViewModel() {

    private val passengerId = 1L // Mock ID

    private val _state = MutableStateFlow(PassengerState())
    val state: StateFlow<PassengerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.incomingBids.collect { bid ->
                _state.value = _state.value.copy(bids = _state.value.bids + bid)
            }
        }
        viewModelScope.launch {
            repository.rideStarted.collect { bid ->
                _state.value = _state.value.copy(rideStarted = true, acceptedBid = bid)
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

    fun connect() {
        repository.connect(passengerId)
        _state.value = _state.value.copy(isConnected = true)
    }

    fun disconnect() {
        repository.disconnect()
        _state.value = _state.value.copy(isConnected = false)
    }

    fun setDestination(location: Location) {
        _state.value = _state.value.copy(destination = location)
    }

    fun setPrice(price: String) {
        _state.value = _state.value.copy(suggestedPrice = price)
    }

    fun createOrder() {
        val dest = _state.value.destination ?: return
        val price = _state.value.suggestedPrice
        repository.createOrder(OrderRequest(passengerId, dest, price))
        _state.value = _state.value.copy(bids = emptyList(), rideStarted = false)
    }

    fun acceptBid(bid: Bid) {
        repository.acceptBid(bid)
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}
