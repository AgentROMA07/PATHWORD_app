package com.pathword.taxi.domain.model

data class Location(val lat: Double, val lng: Double)

data class OrderRequest(
    val passengerId: Long,
    val destination: Location,
    val price: String
)

data class Order(
    val id: String,
    val passengerId: Long,
    val destination: Location,
    val price: String,
    val status: String
)

data class Bid(
    val orderId: String,
    val driverId: Long,
    val price: String
)

data class GpsUpdate(
    val driverId: Long,
    val location: Location
)
