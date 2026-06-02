package com.pathword.taxi.domain.repository

import com.pathword.taxi.domain.model.Location

interface IRoutingRepository {
    suspend fun getRoute(origin: Location, destination: Location): String?
    suspend fun getMapMatchedRoute(coordinates: List<Location>): String?
}
