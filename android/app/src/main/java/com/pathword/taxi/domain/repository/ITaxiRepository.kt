package com.pathword.taxi.domain.repository

import com.pathword.taxi.domain.model.Bid
import com.pathword.taxi.domain.model.GpsUpdate
import com.pathword.taxi.domain.model.Order
import com.pathword.taxi.domain.model.OrderRequest
import kotlinx.coroutines.flow.Flow

interface ITaxiRepository {
    fun connect(userId: Long)
    fun disconnect()

    fun sendGpsUpdate(update: GpsUpdate)
    fun createOrder(request: OrderRequest)
    fun sendBid(bid: Bid)
    fun acceptBid(bid: Bid)

    val incomingOrders: Flow<Order>
    val incomingBids: Flow<Bid>
    val rideStarted: Flow<Bid>
}
