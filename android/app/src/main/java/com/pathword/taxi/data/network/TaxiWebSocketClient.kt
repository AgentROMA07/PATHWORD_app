package com.pathword.taxi.data.network

import com.google.gson.Gson
import com.pathword.taxi.domain.model.*
import com.pathword.taxi.domain.repository.ITaxiRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaxiWebSocketClient @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson
) : ITaxiRepository {

    private var webSocket: WebSocket? = null

    private val _incomingOrders = MutableSharedFlow<Order>(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingOrders: SharedFlow<Order> = _incomingOrders

    private val _incomingBids = MutableSharedFlow<Bid>(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val incomingBids: SharedFlow<Bid> = _incomingBids

    private val _rideStarted = MutableSharedFlow<Bid>(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val rideStarted: SharedFlow<Bid> = _rideStarted

    private val _errorMessages = MutableSharedFlow<String>(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val errorMessages: SharedFlow<String> = _errorMessages

    private var currentUserId: Long? = null
    private var reconnectJob: Job? = null
    private var isIntentionallyDisconnected = false
    private var currentReconnectDelay = 1000L

    override fun connect(userId: Long) {
        currentUserId = userId
        isIntentionallyDisconnected = false
        connectWithRetry(userId, 1000L)
    }

    private fun connectWithRetry(userId: Long, delayMs: Long) {
        currentReconnectDelay = delayMs
        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/ws?user_id=$userId")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                reconnectJob?.cancel()
                currentReconnectDelay = 1000L // Reset delay on successful connection
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val message = gson.fromJson(text, WsMessage::class.java)
                    handleIncomingMessage(message)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                if (!isIntentionallyDisconnected) {
                    scheduleReconnect(currentReconnectDelay)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                if (!isIntentionallyDisconnected) {
                    scheduleReconnect(currentReconnectDelay)
                }
            }
        })
    }

    private fun scheduleReconnect(delayMs: Long) {
        reconnectJob?.cancel()
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            delay(delayMs)
            currentUserId?.let {
                val nextDelay = if (delayMs < 32000L) delayMs * 2 else 32000L
                connectWithRetry(it, nextDelay)
            }
        }
    }

    private fun handleIncomingMessage(message: WsMessage) {
        when (message.type) {
            "new_order" -> {
                val order = gson.fromJson(gson.toJson(message.payload), Order::class.java)
                _incomingOrders.tryEmit(order)
            }
            "bid" -> {
                val bid = gson.fromJson(gson.toJson(message.payload), Bid::class.java)
                _incomingBids.tryEmit(bid)
            }
            "ride_started" -> {
                val bid = gson.fromJson(gson.toJson(message.payload), Bid::class.java)
                _rideStarted.tryEmit(bid)
            }
            "error" -> {
                val errorMsg = message.payload as? String ?: "Unknown error"
                _errorMessages.tryEmit(errorMsg)
            }
        }
    }

    override fun disconnect() {
        isIntentionallyDisconnected = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    override fun sendGpsUpdate(update: GpsUpdate) {
        val msg = WsMessage("gps_update", update)
        webSocket?.send(gson.toJson(msg))
    }

    override fun createOrder(request: OrderRequest) {
        val msg = WsMessage("NEW_ORDER", request)
        webSocket?.send(gson.toJson(msg))
    }

    override fun sendBid(bid: Bid) {
        val msg = WsMessage("bid", bid)
        webSocket?.send(gson.toJson(msg))
    }

    override fun acceptBid(bid: Bid) {
        val msg = WsMessage("accept_bid", bid)
        webSocket?.send(gson.toJson(msg))
    }
}

data class WsMessage(
    val type: String,
    val payload: Any
)
